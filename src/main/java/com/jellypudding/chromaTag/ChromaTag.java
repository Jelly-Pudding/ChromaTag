package com.jellypudding.chromaTag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import org.bukkit.command.PluginCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Essentials integration is currently disabled.
// import com.earth2me.essentials.Essentials;

public final class ChromaTag extends JavaPlugin implements Listener {

    private Map<UUID, TextColor> playerColors;
    private Scoreboard scoreboard;
    private static final Map<String, String> NAMED_COLORS = new HashMap<>();

    // Essentials integration disabled:
    // private Essentials essentials;
    // private boolean useEssentials = false;

    static {
        NAMED_COLORS.put("black", "#000000");
        NAMED_COLORS.put("dark_blue", "#0000AA");
        NAMED_COLORS.put("dark_green", "#00AA00");
        NAMED_COLORS.put("dark_aqua", "#00AAAA");
        NAMED_COLORS.put("dark_red", "#AA0000");
        NAMED_COLORS.put("dark_purple", "#AA00AA");
        NAMED_COLORS.put("gold", "#FFAA00");
        NAMED_COLORS.put("gray", "#AAAAAA");
        NAMED_COLORS.put("dark_gray", "#555555");
        NAMED_COLORS.put("blue", "#5555FF");
        NAMED_COLORS.put("green", "#55FF55");
        NAMED_COLORS.put("aqua", "#55FFFF");
        NAMED_COLORS.put("red", "#FF5555");
        NAMED_COLORS.put("light_purple", "#FF55FF");
        NAMED_COLORS.put("yellow", "#FFFF55");
        NAMED_COLORS.put("white", "#FFFFFF");
    }

    private String getColorCode(NamedTextColor color) {
        if (color == NamedTextColor.BLACK) return "0";
        if (color == NamedTextColor.DARK_BLUE) return "1";
        if (color == NamedTextColor.DARK_GREEN) return "2";
        if (color == NamedTextColor.DARK_AQUA) return "3";
        if (color == NamedTextColor.DARK_RED) return "4";
        if (color == NamedTextColor.DARK_PURPLE) return "5";
        if (color == NamedTextColor.GOLD) return "6";
        if (color == NamedTextColor.GRAY) return "7";
        if (color == NamedTextColor.DARK_GRAY) return "8";
        if (color == NamedTextColor.BLUE) return "9";
        if (color == NamedTextColor.GREEN) return "a";
        if (color == NamedTextColor.AQUA) return "b";
        if (color == NamedTextColor.RED) return "c";
        if (color == NamedTextColor.LIGHT_PURPLE) return "d";
        if (color == NamedTextColor.YELLOW) return "e";
        if (color == NamedTextColor.WHITE) return "f";
        return "f"; // Default to white if no match
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        playerColors = new HashMap<>();
        loadPlayerColors();
        setupScoreboard();
        // Essentials integration disabled:
        // setupEssentials();
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand chromaCommand = getCommand("chromatag");
        if (chromaCommand != null) {
            chromaCommand.setTabCompleter(new ChromaTagTabCompleter());
        } else {
            getLogger().warning("Command 'chromatag' not defined in plugin.yml!");
        }
        getLogger().info("ChromaTag plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        savePlayerColors();
        getLogger().info("ChromaTag plugin has been disabled.");
    }

    private void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    // Essentials integration disabled:
    /*
    private void setupEssentials() {
        if (getServer().getPluginManager().getPlugin("Essentials") != null) {
            essentials = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
            useEssentials = true;
            getLogger().info("ChromaTag detected Essentials. ChromaTag supports Essentials so this is fine.");
        } else {
            getLogger().info("ChromaTag did not detect Essentials. ChromaTag supports Essentials but it is not essential. This is fine.");
        }
    }
    */

    private void loadPlayerColors() {
        FileConfiguration config = this.getConfig();
        ConfigurationSection section = config.getConfigurationSection("player-colors");
        if (section != null) {
            for (String uuidString : section.getKeys(false)) {
                String colorString = config.getString("player-colors." + uuidString);
                if (colorString != null) {
                    TextColor color = TextColor.fromHexString(colorString);
                    if (color != null) {
                        playerColors.put(UUID.fromString(uuidString), color);
                    } else {
                        getLogger().warning("Failed to parse color for player: " + uuidString);
                    }
                } else {
                    getLogger().warning("No color string found for player: " + uuidString);
                }
            }
        }
    }

    private void savePlayerColors() {
        FileConfiguration config = this.getConfig();
        for (Map.Entry<UUID, TextColor> entry : playerColors.entrySet()) {
            config.set("player-colors." + entry.getKey().toString(), "#" + Integer.toHexString(entry.getValue().value()));
        }
        saveConfig();
    }

    private void savePlayerColor(UUID playerUUID, TextColor color) {
        FileConfiguration config = this.getConfig();
        if (color != null) {
            config.set("player-colors." + playerUUID.toString(), "#" + Integer.toHexString(color.value()));
        } else {
            config.set("player-colors." + playerUUID.toString(), null);
        }
        saveConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("chromatag")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
                return true;
            }

            if (args.length < 1 || args.length > 2) {
                player.sendMessage(Component.text("Usage: /chromatag <color|reset> [player]").color(NamedTextColor.RED));
                return true;
            }

            Player targetPlayer = player;
            String colorOrAction = args[0];

            if (args.length == 2) {
                targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    player.sendMessage(Component.text("Player not found: " + args[1]).color(NamedTextColor.RED));
                    return true;
                }
            }

            if (colorOrAction.equalsIgnoreCase("reset")) {
                if (!player.hasPermission("chromatag.reset")) {
                    player.sendMessage(Component.text("You don't have permission to reset colors.").color(NamedTextColor.RED));
                    return true;
                }
                resetPlayerColor(targetPlayer);
                player.sendMessage(Component.text(targetPlayer.getName() + "'s name color has been reset to default.").color(NamedTextColor.GREEN));
                return true;
            }

            if (!player.hasPermission("chromatag.set")) {
                player.sendMessage(Component.text("You don't have permission to set colors.").color(NamedTextColor.RED));
                return true;
            }

            TextColor color = getColorFromString(colorOrAction);
            if (color == null) {
                player.sendMessage(Component.text("Invalid color. Please use a hex code or color name.").color(NamedTextColor.RED));
                return true;
            }

            playerColors.put(targetPlayer.getUniqueId(), color);
            updatePlayerName(targetPlayer);
            savePlayerColor(targetPlayer.getUniqueId(), color);
            player.sendMessage(Component.text(targetPlayer.getName() + "'s name color has been updated!").color(color));
            return true;
        }
        return false;
    }

    private TextColor getColorFromString(String colorString) {
        if (colorString == null) {
            return null;
        }
        if (NAMED_COLORS.containsKey(colorString.toLowerCase())) {
            colorString = NAMED_COLORS.get(colorString.toLowerCase());
        }
        return TextColor.fromHexString(colorString);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (playerColors.containsKey(playerUUID)) {
            TextColor color = playerColors.get(playerUUID);
            updatePlayerName(player);

            // Set custom join message for players with custom color
            Component joinMessage = Component.text(player.getName()).color(color)
                    .append(Component.text(" joined the game").color(NamedTextColor.YELLOW));
            event.joinMessage(joinMessage);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TextColor color = playerColors.get(player.getUniqueId());

        if (color != null) {
            // Set custom quit message only for players with a custom color
            Component quitMessage = Component.text(player.getName()).color(color)
                    .append(Component.text(" left the game").color(NamedTextColor.YELLOW));
            event.quitMessage(quitMessage);
            removePlayerFromTeam(player);
        }
    }

    private void updatePlayerName(Player player) {
        TextColor color = playerColors.getOrDefault(player.getUniqueId(), NamedTextColor.WHITE);
        Component displayName = Component.text(player.getName()).color(color);

        player.displayName(displayName);
        player.playerListName(displayName);

        // Essentials integration disabled:
        /*
        if (useEssentials) {
            String colorCode = getEssentialsColorCode(color);
            String coloredName = colorCode + player.getName();
            essentials.getUser(player.getUniqueId()).setNickname(coloredName);
        }
        */

        // Update scoreboard team
        String teamName = "ChromaTag_" + player.getUniqueId().toString().substring(0, 8);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        NamedTextColor closestNamedColor = findClosestNamedColor(color);
        team.color(closestNamedColor);
        team.prefix(Component.text("").color(color));
        team.suffix(Component.empty());
        team.addEntry(player.getName());
    }

    private void removePlayerFromTeam(Player player) {
        String teamName = "ChromaTag_" + player.getUniqueId().toString().substring(0, 8);
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    private void resetPlayerColor(Player player) {
        playerColors.remove(player.getUniqueId());
        removePlayerFromTeam(player);

        // Remove from config completely rather than setting to null
        FileConfiguration config = this.getConfig();
        config.set("player-colors." + player.getUniqueId().toString(), null);
        saveConfig();

        // Reset their name without storing a default color
        Component displayName = Component.text(player.getName());
        player.displayName(displayName);
        player.playerListName(displayName);

        // Essentials integration disabled:
        /*
        if (useEssentials) {
            essentials.getUser(player.getUniqueId()).setNickname(null);
        }
        */
    }

    private NamedTextColor findClosestNamedColor(TextColor color) {
        NamedTextColor closestColor = NamedTextColor.WHITE;
        int minDifference = Integer.MAX_VALUE;

        for (NamedTextColor namedColor : NamedTextColor.NAMES.values()) {
            int difference = colorDifference(color, namedColor);
            if (difference < minDifference) {
                minDifference = difference;
                closestColor = namedColor;
            }
        }

        return closestColor;
    }

    private int colorDifference(TextColor color1, TextColor color2) {
        int r1 = color1.red(), g1 = color1.green(), b1 = color1.blue();
        int r2 = color2.red(), g2 = color2.green(), b2 = color2.blue();
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }

    // Essentials integration disabled:
    /*
    private String getEssentialsColorCode(TextColor color) {
        if (color instanceof NamedTextColor) {
            return "§" + getColorCode((NamedTextColor) color);
        } else {
            // For custom colors, find the closest named color
            return "§" + getColorCode(findClosestNamedColor(color));
        }
    }
    */
}
