package com.jellypudding.chromaTag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ChromaTag extends JavaPlugin implements Listener {

    private Map<UUID, TextColor> playerColors;
    private Scoreboard scoreboard;
    private Connection connection;
    private final String dbPath = getDataFolder().getAbsolutePath() + File.separator + "chromatag.db";

    private static final Map<String, String> NAMED_COLORS = new HashMap<>();

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

    @Override
    public void onEnable() {
        playerColors = new HashMap<>();
        if (!setupDatabase()) {
            getLogger().severe("Failed to initialize the database. Disabling ChromaTag.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        loadPlayerColorsFromDB();
        setupScoreboard();
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand chromaCommand = getCommand("chromatag");
        if (chromaCommand != null) {
            chromaCommand.setTabCompleter(new ChromaTagTabCompleter());
        } else {
            getLogger().warning("Command 'chromatag' not defined in plugin.yml!");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyColorToPlayer(player);
        }

        getLogger().info("ChromaTag plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        closeDatabaseConnection();
        getLogger().info("ChromaTag plugin has been disabled.");
    }

    private boolean setupDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            if (!getDataFolder().exists()) {
                if (!getDataFolder().mkdirs()) {
                    getLogger().severe("Could not create plugin data folder!");
                    return false;
                }
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTableIfNotExists();
            return true;
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not connect to SQLite database!", e);
            return false;
        } catch (ClassNotFoundException e) {
            getLogger().log(Level.SEVERE, "SQLite JDBC driver not found!", e);
            return false;
        }
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS player_colors (" +
                     "uuid TEXT PRIMARY KEY NOT NULL," +
                     "color TEXT NOT NULL);";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not create database table!", e);
        }
    }

    private void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not close SQLite connection!", e);
        }
    }

    private void loadPlayerColorsFromDB() {
        String sql = "SELECT uuid, color FROM player_colors";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String uuidString = rs.getString("uuid");
                String colorString = rs.getString("color");
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    TextColor color = TextColor.fromHexString(colorString);
                    if (color != null) {
                        playerColors.put(uuid, color);
                    } else {
                        getLogger().warning("Failed to parse color '" + colorString + "' for player: " + uuidString);
                    }
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid UUID found in database: " + uuidString);
                }
            }
            getLogger().info("Loaded " + playerColors.size() + " player colors from database.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not load player colors from database!", e);
        }
    }

    private void savePlayerColorToDB(UUID playerUUID, TextColor color) {
        String sql = "INSERT OR REPLACE INTO player_colors (uuid, color) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, "#" + Integer.toHexString(color.value()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not save player color to database for " + playerUUID, e);
        }
    }

    private void removePlayerColorFromDB(UUID playerUUID) {
        String sql = "DELETE FROM player_colors WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not remove player color from database for " + playerUUID, e);
        }
    }

    private void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("chromatag")) {
            Player player = null;
            if (sender instanceof Player) {
                player = (Player) sender;
            }

            if (args.length < 1 || args.length > 2) {
                if (player != null) player.sendMessage(Component.text("Usage: /chromatag <color|reset> [player]").color(NamedTextColor.RED));
                else sender.sendMessage(Component.text("Usage: chromatag <color|reset> <player>"));
                return true;
            }

            Player targetPlayer;
            String colorOrAction = args[0];

            if (args.length == 2) {
                if (player != null && !player.hasPermission("chromatag.set.other")) {
                    player.sendMessage(Component.text("You don't have permission to set other players' colors.").color(NamedTextColor.RED));
                    return true;
                }
                targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    if (player != null) player.sendMessage(Component.text("Player not found: " + args[1]).color(NamedTextColor.RED));
                    else sender.sendMessage(Component.text("Player not found: " + args[1]));
                    return true;
                }
            } else {
                if (player == null) {
                    sender.sendMessage(Component.text("You must specify a player when running from console."));
                    return true;
                }
                targetPlayer = player;
            }

            if (colorOrAction.equalsIgnoreCase("reset")) {
                boolean hasPermission = (player == null)
                    || (targetPlayer.equals(player) && player.hasPermission("chromatag.reset.self"))
                    || (!targetPlayer.equals(player) && player.hasPermission("chromatag.reset.other"));

                if (!hasPermission) {
                    player.sendMessage(Component.text("You don't have permission to reset colors.").color(NamedTextColor.RED));
                    return true;
                }

                boolean success = internalResetPlayerColor(targetPlayer);
                if (success) {
                    Component message = Component.text(targetPlayer.getName() + "'s name color has been reset to default.").color(NamedTextColor.GREEN);
                    if (player != null) player.sendMessage(message);
                    else sender.sendMessage(message);
                } else {
                    Component message = Component.text("Could not reset " + targetPlayer.getName() + "'s color (they might not have one set).").color(NamedTextColor.YELLOW);
                    if (player != null) player.sendMessage(message);
                    else sender.sendMessage(message);
                }
                return true;
            }

            boolean hasPermission = (player == null)
                || (targetPlayer.equals(player) && player.hasPermission("chromatag.set.self"))
                || (!targetPlayer.equals(player) && player.hasPermission("chromatag.set.other"));

            if (!hasPermission) {
                player.sendMessage(Component.text("You don't have permission to set colors.").color(NamedTextColor.RED));
                return true;
            }

            TextColor color = getColorFromString(colorOrAction);
            if (color == null) {
                Component message = Component.text("Invalid color '" + colorOrAction + "'. Use hex (#RRGGBB) or name (e.g., red, dark_blue).").color(NamedTextColor.RED);
                if (player != null) player.sendMessage(message);
                else sender.sendMessage(message);
                return true;
            }

            boolean success = internalSetPlayerColor(targetPlayer, color);
            if (success) {
                Component message = Component.text(targetPlayer.getName() + "'s name color has been updated!").color(color);
                if (player != null) player.sendMessage(message);
                else sender.sendMessage(message);
                if (player == null || !targetPlayer.equals(player)) {
                    targetPlayer.sendMessage(Component.text("Your name color has been set!").color(color));
                }
            } else {
                Component message = Component.text("Failed to set color for " + targetPlayer.getName()).color(NamedTextColor.RED);
                if (player != null) player.sendMessage(message);
                else sender.sendMessage(message);
            }
            return true;
        }
        return false;
    }

    private TextColor getColorFromString(String colorString) {
        if (colorString == null) {
            return null;
        }
        String lowerCaseColorString = colorString.toLowerCase();
        if (NAMED_COLORS.containsKey(lowerCaseColorString)) {
            colorString = NAMED_COLORS.get(lowerCaseColorString);
        }
        if (!colorString.startsWith("#")) {
            colorString = "#" + colorString;
        }
        try {
            return TextColor.fromHexString(colorString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyColorToPlayer(player);

        TextColor color = playerColors.get(player.getUniqueId());
        if (color != null) {
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
            Component quitMessage = Component.text(player.getName()).color(color)
                    .append(Component.text(" left the game").color(NamedTextColor.YELLOW));
            event.quitMessage(quitMessage);
        }

        removePlayerFromTeam(player);
    }

    private void applyColorToPlayer(Player player) {
        TextColor color = playerColors.get(player.getUniqueId());
        updatePlayerVisuals(player, color);
    }

    private void updatePlayerVisuals(Player player, TextColor color) {
        Component displayName;
        if (color != null) {
            displayName = Component.text(player.getName()).color(color);
        } else {
            displayName = Component.text(player.getName());
        }

        player.displayName(displayName);
        player.playerListName(displayName);

        updateScoreboardTeam(player, color);
    }

    private void updateScoreboardTeam(Player player, TextColor color) {
        String teamName = getTeamName(player.getUniqueId());
        Team team = scoreboard.getTeam(teamName);

        if (color == null) {
            if (team != null) {
                team.removeEntry(player.getName());
                if (team.getEntries().isEmpty()) {
                    team.unregister();
                }
            }
            return;
        }

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        NamedTextColor closestNamedColor = findClosestNamedColor(color);
        team.color(closestNamedColor);
        team.prefix(Component.text("").color(color));
        team.suffix(Component.empty());

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    private void removePlayerFromTeam(Player player) {
        String teamName = getTeamName(player.getUniqueId());
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    private String getTeamName(UUID uuid) {
        return "CT_" + uuid.toString().substring(0, 13);
    }

    private boolean internalResetPlayerColor(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerColors.containsKey(uuid)) {
            playerColors.remove(uuid);
            removePlayerColorFromDB(uuid);
            updatePlayerVisuals(player, null);
            return true;
        }
        return false;
    }

    private boolean internalSetPlayerColor(Player player, TextColor color) {
        UUID uuid = player.getUniqueId();
        playerColors.put(uuid, color);
        savePlayerColorToDB(uuid, color);
        updatePlayerVisuals(player, color);
        return true;
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

    public TextColor getPlayerColor(UUID playerUUID) {
        return playerColors.get(playerUUID);
    }

    public boolean setPlayerColor(@NotNull UUID playerUUID, @NotNull TextColor color) {
        playerColors.put(playerUUID, color);
        savePlayerColorToDB(playerUUID, color);
        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        if (onlinePlayer != null) {
            updatePlayerVisuals(onlinePlayer, color);
        }
        return true;
    }

    public boolean resetPlayerColor(@NotNull UUID playerUUID) {
        if (playerColors.containsKey(playerUUID)) {
            playerColors.remove(playerUUID);
            removePlayerColorFromDB(playerUUID);
            Player onlinePlayer = Bukkit.getPlayer(playerUUID);
            if (onlinePlayer != null) {
                updatePlayerVisuals(onlinePlayer, null);
            }
            return true;
        }
        return false;
    }
}
