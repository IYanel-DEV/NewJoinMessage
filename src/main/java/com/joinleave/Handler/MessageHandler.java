package com.joinleave.Handler;

import com.joinleave.JoinleaveMessage;
import com.joinleave.LangMessageChanger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class MessageHandler {

    private final JoinleaveMessage plugin;
    private final LangMessageChanger langMessageChanger;

    // Constructor
    public MessageHandler(JoinleaveMessage plugin) {
        this.plugin = plugin;
        this.langMessageChanger = new LangMessageChanger(plugin);
    }

    public boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("info")) {
            return false;
        }
        String playerName = args[1];
        // Works for offline players too, as long as they have stored data.
        UUID targetId = plugin.resolvePlayerUuid(playerName);
        if (targetId == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        String displayName = plugin.displayNameFor(targetId, playerName);
        String joinMessage = plugin.getMessage(targetId, "join", "default-join-message");
        String leaveMessage = plugin.getMessage(targetId, "leave", "default-leave-message");
        String lastJoinChange = plugin.getLastChange(targetId, "join");
        String lastLeaveChange = plugin.getLastChange(targetId, "leave");

        langMessageChanger.sendJoinLeaveInfo(sender, displayName, joinMessage, leaveMessage, lastJoinChange, lastLeaveChange);
        return true;
    }
}
