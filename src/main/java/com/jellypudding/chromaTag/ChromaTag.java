package com.jellypudding.chromaTag;

import com.jellypudding.chromaTag.commands.ChromaTagCommand;
import com.jellypudding.chromaTag.data.ColorRepository;
import com.jellypudding.chromaTag.display.NametagManager;
import com.jellypudding.chromaTag.listeners.PlayerListener;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChromaTag extends JavaPlugin {

    private Map<UUID, TextColor> colorCache;
    private ColorRepository repository;
    private NametagManager nametagManager;

    @Override
    public void onEnable() {
        colorCache = new HashMap<>();

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create plugin data folder. Disabling ChromaTag.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        repository = new ColorRepository(getDataFolder(), getLogger());
        if (!repository.connect()) {
            getLogger().severe("Failed to initialise the database. Disabling ChromaTag.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        nametagManager = new NametagManager();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        PluginCommand cmd = getCommand("chromatag");
        if (cmd != null) {
            ChromaTagCommand handler = new ChromaTagCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(new ChromaTagTabCompleter());
        } else {
            getLogger().warning("Command 'chromatag' not defined in plugin.yml.");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            loadAndApply(player);
        }

        new Metrics(this, 27553);

        getLogger().info("ChromaTag enabled.");
    }

    @Override
    public void onDisable() {
        if (repository != null) repository.close();
        getLogger().info("ChromaTag disabled.");
    }

    public void loadAndApply(Player player) {
        TextColor color = repository.load(player.getUniqueId());
        if (color != null) {
            colorCache.put(player.getUniqueId(), color);
        }
        nametagManager.applyColor(player, color);
    }

    public void applyColor(Player player, TextColor color) {
        UUID uuid = player.getUniqueId();
        if (color != null && color.value() == 0xFFFFFF) {
            colorCache.remove(uuid);
            repository.delete(uuid);
            nametagManager.applyColor(player, null);
        } else {
            colorCache.put(uuid, color);
            repository.save(uuid, color);
            nametagManager.applyColor(player, color);
        }
    }

    public boolean clearColor(Player player) {
        UUID uuid = player.getUniqueId();
        boolean inCache = colorCache.remove(uuid) != null;
        boolean inDb    = repository.delete(uuid);
        nametagManager.applyColor(player, null);
        return inCache || inDb;
    }

    public void evict(Player player) {
        colorCache.remove(player.getUniqueId());
        nametagManager.removeFromTeam(player);
    }

    // API Methods Below

    public TextColor getPlayerColor(@NotNull UUID playerUUID) {
        TextColor cached = colorCache.get(playerUUID);
        if (cached != null) return cached;
        TextColor fromDb = repository.load(playerUUID);
        if (fromDb != null) colorCache.put(playerUUID, fromDb);
        return fromDb;
    }

    public boolean setPlayerColor(@NotNull UUID playerUUID, @NotNull TextColor color) {
        if (color.value() == 0xFFFFFF) {
            colorCache.remove(playerUUID);
            repository.delete(playerUUID);
        } else {
            colorCache.put(playerUUID, color);
            repository.save(playerUUID, color);
        }
        Player online = Bukkit.getPlayer(playerUUID);
        if (online != null) {
            nametagManager.applyColor(online, color.value() == 0xFFFFFF ? null : color);
        }
        return true;
    }

    public boolean resetPlayerColor(@NotNull UUID playerUUID) {
        boolean inCache = colorCache.remove(playerUUID) != null;
        boolean inDb    = repository.delete(playerUUID);
        Player online = Bukkit.getPlayer(playerUUID);
        if (online != null) {
            nametagManager.applyColor(online, null);
        }
        return inCache || inDb;
    }
}
