package com.joinleave.Handler;

import com.joinleave.JoinleaveMessage;
import com.joinleave.LanguageHandler;
import com.joinleave.util.Perms;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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
        if (!messageType.equals("all") && !messageType.equals("join") && !messageType.equals("leave")) {
            sender.sendMessage(languageHandler.getMessage(null, "clear_invalid_type"));
            return true;
        }

        UUID targetId;
        Player onlineTarget = null;

        if (args.length >= 3) {
            if (!Perms.has(sender, "joinleave.clearplayer")) {
                sender.sendMessage(languageHandler.getMessage(null, "clear_no_permission"));
                return true;
            }
            targetId = plugin.resolvePlayerUuid(args[2]);
            if (targetId == null) {
                sender.sendMessage(languageHandler.getMessage(null, "clear_player_not_found"));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(languageHandler.getMessage(null, "clear_console_error"));
                return true;
            }
            onlineTarget = (Player) sender;
            boolean allowed = "all".equals(messageType)
                    ? plugin.canCustomize(onlineTarget, "join") && plugin.canCustomize(onlineTarget, "leave")
                    : plugin.canCustomize(onlineTarget, messageType);
            if (!allowed) {
                sender.sendMessage(languageHandler.getMessage(onlineTarget, "clear_no_permission"));
                return true;
            }
            targetId = onlineTarget.getUniqueId();
        }

        plugin.clearMessage(targetId, messageType);
        sender.sendMessage(languageHandler.getMessage(onlineTarget, "clear_success")
                .replace("%type%", messageType)
                .replace("%player%", plugin.displayNameFor(targetId, args.length >= 3 ? args[2] : "player")));
        return true;
    }
}
