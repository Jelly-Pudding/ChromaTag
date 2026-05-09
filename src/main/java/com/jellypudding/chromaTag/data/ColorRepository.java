package com.jellypudding.chromaTag.data;

import net.kyori.adventure.text.format.TextColor;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ColorRepository {

    private Connection connection;
    private final String dbPath;
    private final Logger logger;

    public ColorRepository(File dataFolder, Logger logger) {
        this.dbPath = dataFolder.getAbsolutePath() + File.separator + "chromatag.db";
        this.logger = logger;
    }

    public boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTableIfNotExists();
            return true;
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "SQLite JDBC driver not found.", e);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not connect to SQLite database.", e);
        }
        return false;
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS player_colors (" +
                     "uuid TEXT PRIMARY KEY NOT NULL," +
                     "color TEXT NOT NULL);";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not close SQLite connection.", e);
        }
    }

    public void save(UUID uuid, TextColor color) {
        String sql = "INSERT OR REPLACE INTO player_colors (uuid, color) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, String.format("#%06X", color.value()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not save player color for " + uuid, e);
        }
    }

    public boolean delete(UUID uuid) {
        String sql = "DELETE FROM player_colors WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not delete player color for " + uuid, e);
            return false;
        }
    }

    public TextColor load(UUID uuid) {
        String sql = "SELECT color FROM player_colors WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return TextColor.fromHexString(rs.getString("color"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Could not load player color for " + uuid, e);
        }
        return null;
    }
}
