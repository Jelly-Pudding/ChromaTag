package com.jellypudding.chromaTag.commands;

import com.jellypudding.chromaTag.ChromaTag;
import com.jellypudding.chromaTag.data.ColorConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ChromaTagCommand implements CommandExecutor {

    private final ChromaTag plugin;

    public ChromaTagCommand(ChromaTag plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        Player player = sender instanceof Player ? (Player) sender : null;

        if (args.length < 1 || args.length > 2) {
            if (player != null) {
                player.sendMessage(Component.text("Usage: /chromatag <color|reset|getcolor> [player]").color(NamedTextColor.RED));
            } else {
                sender.sendMessage(Component.text("Usage: chromatag <color|reset|getcolor> <player>"));
            }
            return true;
        }

        String action = args[0];

        if (action.equalsIgnoreCase("getcolor")) {
            return handleGetColor(sender, player, args);
        }

        Player target = resolveOnlineTarget(sender, player, args);
        if (target == null) return true;

        if (action.equalsIgnoreCase("reset")) {
            return handleReset(sender, player, target);
        }

        return handleSetColor(sender, player, target, action);
    }

    private boolean handleGetColor(@NotNull CommandSender sender, Player player, String[] args) {
        if (player != null && !player.hasPermission("chromatag.getcolor")) {
            player.sendMessage(Component.text("You don't have permission to get player colors.").color(NamedTextColor.RED));
            return true;
        }

        UUID targetUUID;
        if (args.length == 2) {
            Player online = Bukkit.getPlayerExact(args[1]);
            if (online != null) {
                targetUUID = online.getUniqueId();
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                if (!offline.hasPlayedBefore()) {
                    send(sender, player,
                            Component.text("Player not found: " + args[1]).color(NamedTextColor.RED),
                            Component.text("Player not found: " + args[1]));
                    return true;
                }
                targetUUID = offline.getUniqueId();
            }
        } else {
            if (player == null) {
                sender.sendMessage(Component.text("You must specify a player when running from console."));
                return true;
            }
            targetUUID = player.getUniqueId();
        }

        TextColor color = plugin.getPlayerColor(targetUUID);
        String hex = (color != null) ? String.format("#%06X", color.value()) : "#FFFFFF";
        if (player != null) {
            player.sendMessage(Component.text(hex).color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text(hex));
        }
        return true;
    }

    private boolean handleReset(@NotNull CommandSender sender, Player player, @NotNull Player target) {
        boolean hasPermission = player == null
                || (target.equals(player) && player.hasPermission("chromatag.reset.self"))
                || (!target.equals(player) && player.hasPermission("chromatag.reset.other"));

        if (!hasPermission) {
            player.sendMessage(Component.text("You don't have permission to reset colors.").color(NamedTextColor.RED));
            return true;
        }

        boolean hadColor = plugin.clearColor(target);
        Component msg = hadColor
                ? Component.text(target.getName() + "'s name color has been reset to default.").color(NamedTextColor.GREEN)
                : Component.text("Could not reset " + target.getName() + "'s color (they might not have one set).").color(NamedTextColor.YELLOW);
        send(sender, player, msg, msg);
        return true;
    }

    private boolean handleSetColor(@NotNull CommandSender sender, Player player,
                                   @NotNull Player target, String colorStr) {
        boolean hasPermission = player == null
                || (target.equals(player) && player.hasPermission("chromatag.set.self"))
                || (!target.equals(player) && player.hasPermission("chromatag.set.other"));

        if (!hasPermission) {
            player.sendMessage(Component.text("You don't have permission to set colors.").color(NamedTextColor.RED));
            return true;
        }

        TextColor color = parseColor(colorStr);
        if (color == null) {
            Component msg = Component.text("Invalid color '" + colorStr + "'. Use hex (#RRGGBB) or a name (e.g. red, dark_blue).").color(NamedTextColor.RED);
            send(sender, player, msg, msg);
            return true;
        }

        plugin.applyColor(target, color);

        Component msg = Component.text(target.getName() + "'s name color has been updated.").color(color);
        send(sender, player, msg, msg);
        if (player == null || !target.equals(player)) {
            target.sendMessage(Component.text("Your name color has been set.").color(color));
        }
        return true;
    }

    private Player resolveOnlineTarget(@NotNull CommandSender sender, Player player, String[] args) {
        if (args.length == 2) {
            if (player != null && !player.hasPermission("chromatag.set.other")) {
                player.sendMessage(Component.text("You don't have permission to set other players' colors.").color(NamedTextColor.RED));
                return null;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                send(sender, player,
                        Component.text("Player not found: " + args[1]).color(NamedTextColor.RED),
                        Component.text("Player not found: " + args[1]));
                return null;
            }
            return target;
        }
        if (player == null) {
            sender.sendMessage(Component.text("You must specify a player when running from console."));
            return null;
        }
        return player;
    }

    private TextColor parseColor(String input) {
        if (input == null) return null;
        String resolved = ColorConstants.NAMED_COLORS.get(input.toLowerCase());
        if (resolved == null) {
            resolved = input.startsWith("#") ? input : "#" + input;
        }
        return TextColor.fromHexString(resolved);
    }

    private void send(@NotNull CommandSender sender, Player player,
                      Component playerMsg, Component consoleMsg) {
        if (player != null) {
            player.sendMessage(playerMsg);
        } else {
            sender.sendMessage(consoleMsg);
        }
    }
}
