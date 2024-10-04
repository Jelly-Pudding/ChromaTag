package com.jellypudding.chromaTag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChromaTag extends JavaPlugin implements Listener {

    private Map<UUID, TextColor> playerColors;
    private Scoreboard scoreboard;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        playerColors = new HashMap<>();
        loadPlayerColors();
        setupScoreboard();
        getServer().getPluginManager().registerEvents(this, this);
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

    private void loadPlayerColors() {
        FileConfiguration config = this.getConfig();
        if (config.contains("player-colors")) {
            for (String uuidString : config.getConfigurationSection("player-colors").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                String colorString = config.getString("player-colors." + uuidString);
                TextColor color = TextColor.fromHexString(colorString);
                if (color != null) {
                    playerColors.put(uuid, color);
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

            if (colorOrAction.equalsIgnoreCase("reload")) {
                if (!player.hasPermission("chromatag.reload")) {
                    player.sendMessage(Component.text("You don't have permission to reload the plugin.").color(NamedTextColor.RED));
                    return true;
                }
                reloadConfig();
                loadPlayerColors();
                player.sendMessage(Component.text("ChromaTag configuration reloaded.").color(NamedTextColor.GREEN));
                return true;
            }

            if (!player.hasPermission("chromatag.set")) {
                player.sendMessage(Component.text("You don't have permission to set colors.").color(NamedTextColor.RED));
                return true;
            }

            TextColor color = TextColor.fromHexString(colorOrAction);
            if (color == null) {
                player.sendMessage(Component.text("Invalid color. Please use a hex color code (e.g., #FF0000 for red).").color(NamedTextColor.RED));
                return true;
            }

            playerColors.put(targetPlayer.getUniqueId(), color);
            updatePlayerName(targetPlayer);
            player.sendMessage(Component.text(targetPlayer.getName() + "'s name color has been updated!").color(color));
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerName(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayerFromTeam(event.getPlayer());
    }

    private void updatePlayerName(Player player) {
        TextColor color = playerColors.getOrDefault(player.getUniqueId(), NamedTextColor.WHITE);
        Component displayName = Component.text(player.getName()).color(color);

        player.displayName(displayName);
        player.playerListName(displayName);

        // Update scoreboard team
        String teamName = "ChromaTag_" + player.getUniqueId().toString().substring(0, 8);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // Find the closest NamedTextColor
        NamedTextColor closestNamedColor = findClosestNamedColor(color);
        team.color(closestNamedColor);

        // Use the exact color in the prefix
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
        updatePlayerName(player);
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
}