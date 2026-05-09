package com.jellypudding.chromaTag.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

public class NametagManager {

    private final Scoreboard scoreboard;

    public NametagManager() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    //Updates display name, tab-list name and nametag colour for a player.
    public void applyColor(Player player, TextColor color) {
        Component name = color != null
                ? Component.text(player.getName()).color(color)
                : Component.text(player.getName());

        player.displayName(name);
        player.playerListName(name);
        updateScoreboardTeam(player, color);
    }

    public void removeFromTeam(Player player) {
        Team team = scoreboard.getTeam(teamName(player.getUniqueId()));
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    private void updateScoreboardTeam(Player player, TextColor color) {
        String name = teamName(player.getUniqueId());
        Team team = scoreboard.getTeam(name);

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
            team = scoreboard.registerNewTeam(name);
        }

        team.color(closestNamedColor(color));
        team.prefix(Component.text("").color(color));
        team.suffix(Component.empty());

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    private String teamName(UUID uuid) {
        return "CT_" + uuid.toString().substring(0, 13);
    }

    private NamedTextColor closestNamedColor(TextColor color) {
        NamedTextColor closest = NamedTextColor.WHITE;
        int minDiff = Integer.MAX_VALUE;
        for (NamedTextColor named : NamedTextColor.NAMES.values()) {
            int diff = Math.abs(color.red()   - named.red())
                     + Math.abs(color.green() - named.green())
                     + Math.abs(color.blue()  - named.blue());
            if (diff < minDiff) {
                minDiff = diff;
                closest = named;
            }
        }
        return closest;
    }
}
