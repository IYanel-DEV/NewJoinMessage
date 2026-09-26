package com.joinleave.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** OP always counts as full access (LuckPerms / weird defaults won't hide admin UI). */
public final class Perms {
    private Perms() {}

    public static boolean isAdmin(CommandSender sender) {
        if (sender.isOp()) {
            return true;
        }
        return sender.hasPermission("joinleave.admin")
                || sender.hasPermission("joinleave.*")
                || sender.hasPermission("modernjoinmessage.admin");
    }

    public static boolean has(CommandSender sender, String permission) {
        return isAdmin(sender) || sender.hasPermission(permission);
    }

    public static boolean canOpenGui(Player player) {
        return has(player, "joinleave.gui") || has(player, "modernjoinmessage.use");
    }
}
