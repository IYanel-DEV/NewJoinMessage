package com.joinleave.Handler;

import com.joinleave.JoinleaveMessage;
import com.joinleave.JoinLeaveGUI;
import com.joinleave.util.Perms;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuiHandler {

    private final JoinleaveMessage plugin;

    public GuiHandler(JoinleaveMessage plugin) {
        this.plugin = plugin;
    }

    public boolean handleGuiCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }
        Player player = (Player) sender;
        if (!Perms.canOpenGui(player)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to open the GUI. (joinleave.gui)");
            return true;
        }
        plugin.getGui().openGUI(player);
        return true;
    }
}
