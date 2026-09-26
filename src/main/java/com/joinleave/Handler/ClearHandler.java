package com.joinleave.Handler;

import com.joinleave.JoinleaveMessage;
import com.joinleave.LanguageHandler;
import com.joinleave.util.Perms;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearHandler {

    private final JoinleaveMessage plugin;
    private final LanguageHandler languageHandler;

    public ClearHandler(JoinleaveMessage plugin, LanguageHandler languageHandler) {
        this.plugin = plugin;
        this.languageHandler = languageHandler;
    }

    public boolean handleClearCommand(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("clear")) {
            return false;
        }

        String messageType = args[1].toLowerCase();
        Player target;
        if (args.length >= 3) {
            if (!Perms.has(sender, "joinleave.clearplayer")) {
                sender.sendMessage(languageHandler.getMessage(null, "clear_no_permission"));
                return true;
            }
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(languageHandler.getMessage(null, "clear_player_not_found"));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(languageHandler.getMessage(null, "clear_console_error"));
                return true;
            }
            target = (Player) sender;
            boolean allowed = "all".equals(messageType)
                    ? plugin.canCustomize(target, "join") && plugin.canCustomize(target, "leave")
                    : plugin.canCustomize(target, messageType);
            if (!allowed) {
                sender.sendMessage(languageHandler.getMessage(target, "clear_no_permission"));
                return true;
            }
        }

        if (!(messageType.equals("all") || messageType.equals("join") || messageType.equals("leave"))) {
            sender.sendMessage(languageHandler.getMessage(target, "clear_invalid_type"));
            return true;
        }
        plugin.clearMessage(target, messageType);
        sender.sendMessage(languageHandler.getMessage(target, "clear_success")
                .replace("%type%", messageType)
                .replace("%player%", target.getName()));
        return true;
    }
}
