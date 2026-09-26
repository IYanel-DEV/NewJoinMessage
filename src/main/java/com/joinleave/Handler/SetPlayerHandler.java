package com.joinleave.Handler;

import com.joinleave.JoinleaveMessage;
import com.joinleave.LanguageHandler;
import com.joinleave.util.Perms;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SetPlayerHandler {

    private final JoinleaveMessage plugin;
    private final LanguageHandler languageHandler;

    // Constructor
    public SetPlayerHandler(JoinleaveMessage plugin, LanguageHandler languageHandler) {
        this.plugin = plugin;
        this.languageHandler = languageHandler;
    }

    public boolean handleSetPlayerCommand(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[0].equalsIgnoreCase("setplayer")) {
            return false;
        }
        Player senderPlayer = sender instanceof Player ? (Player) sender : null;
        if (!Perms.has(sender, "joinleave.setplayer")) {
            sender.sendMessage(languageHandler.getMessage(senderPlayer, "setplayer_no_permission"));
            return true;
        }

        String targetName = args[1];
        // Resolves online players first, then the stored name index for offline players.
        UUID targetId = plugin.resolvePlayerUuid(targetName);
        if (targetId == null) {
            sender.sendMessage(languageHandler.getMessage(senderPlayer, "setplayer_player_not_found"));
            return true;
        }

        String messageType = args[2].toLowerCase();
        if (!messageType.equals("join") && !messageType.equals("leave")) {
            sender.sendMessage(languageHandler.getMessage(senderPlayer, "setplayer_invalid_type"));
            return true;
        }

        String message = String.join(" ", args).substring(args[0].length() + args[1].length() + args[2].length() + 3);
        if (plugin.isTooLong(message)) {
            sender.sendMessage(languageHandler.getMessage(senderPlayer, "message_too_long")
                    .replace("%max%", String.valueOf(plugin.maxMessageLength())));
            return true;
        }
        if (!plugin.enforceCooldown(sender)) {
            return true;
        }
        plugin.setMessage(targetId, messageType, message);
        plugin.noteChange(sender);
        sender.sendMessage(languageHandler.getMessage(senderPlayer, "setplayer_success")
                .replace("%type%", messageType)
                .replace("%player%", plugin.displayNameFor(targetId, targetName)));
        return true;
    }
}
