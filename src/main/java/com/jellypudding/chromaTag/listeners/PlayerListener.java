package com.jellypudding.chromaTag.listeners;

import com.jellypudding.chromaTag.ChromaTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ChromaTag plugin;

    public PlayerListener(ChromaTag plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.loadAndApply(player);

        TextColor color = plugin.getPlayerColor(player.getUniqueId());
        if (color != null) {
            event.joinMessage(Component.text(player.getName()).color(color)
                    .append(Component.text(" joined the game").color(NamedTextColor.YELLOW)));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TextColor color = plugin.getPlayerColor(player.getUniqueId());

        if (color != null) {
            event.quitMessage(Component.text(player.getName()).color(color)
                    .append(Component.text(" left the game").color(NamedTextColor.YELLOW)));
        }

        plugin.evict(player);
    }
}
