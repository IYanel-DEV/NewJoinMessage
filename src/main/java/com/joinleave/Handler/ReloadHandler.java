package com.joinleave.Handler;

import com.joinleave.JoinleaveMessage;
import com.joinleave.util.Perms;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadHandler {

    private final JoinleaveMessage plugin;

    // Constructor
    public ReloadHandler(JoinleaveMessage plugin) {
        this.plugin = plugin;
    }

    public boolean handleReloadCommand(CommandSender sender) {
        if (Perms.has(sender, "joinleave.reload")) {
            // Reload the plugin
            plugin.reloadPlugin(sender);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin.");
            return true;
        }
    }
}
