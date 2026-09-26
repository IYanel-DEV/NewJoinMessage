package com.joinleave.Handler;

import com.joinleave.JoinleaveMessage;
import com.joinleave.LanguageHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHandler {

    private final JoinleaveMessage plugin;
    private final LanguageHandler languageHandler;

    // Constructor
    public SetHandler(JoinleaveMessage plugin) {
        this.plugin = plugin;
        this.languageHandler = plugin.getLanguageHandler();
    }

    public boolean handleSetCommand(CommandSender sender, String[] args) {
        if (args.length >= 3 && args[0].equalsIgnoreCase("set")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String messageType = args[1].toLowerCase();
                String message = String.join(" ", args).substring(args[0].length() + args[1].length() + 2);

                if (messageType.equals("join") || messageType.equals("leave")) {
                    if (!plugin.canCustomize(player, messageType)) {
                        sender.sendMessage(languageHandler.getMessage(player, "no_permission").replace("%type%", messageType));
                        return true;
                    }
                    if (plugin.isTooLong(message)) {
                        sender.sendMessage(languageHandler.getMessage(player, "message_too_long")
                                .replace("%max%", String.valueOf(plugin.maxMessageLength())));
                        return true;
                    }
                    if (!plugin.enforceCooldown(sender)) {
                        return true;
                    }

                    plugin.setMessage(player, messageType, message);
                    plugin.noteChange(sender);
                    sender.sendMessage(languageHandler.getMessage(player, "set_success").replace("%type%", messageType));
                } else {
                    sender.sendMessage(languageHandler.getMessage(player, "invalid_type"));
                }
                return true;
            } else {
                sender.sendMessage(languageHandler.getMessage(null, "console_error"));
                return true;
            }
        }
        return false;
    }
}
