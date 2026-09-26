package com.joinleave;

import com.joinleave.Handler.*;
import com.joinleave.util.Perms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class JoinleaveCommand implements CommandExecutor, TabCompleter {

    private final JoinleaveMessage plugin;
    private final MessageHandler messageHandler;
    private final SetPlayerHandler setPlayerHandler;
    private final SetHandler setHandler;
    private final GuiHandler guiHandler;
    private final ClearHandler clearHandler;
    private final ReloadHandler reloadHandler;
    private final LanguageHandler languageHandler; // Use LanguageHandler
    private final Language language; // Add Language handler

    // Constructor
    public JoinleaveCommand(JoinleaveMessage plugin) {
        this.plugin = plugin;
        this.messageHandler = new MessageHandler(plugin);
        this.setPlayerHandler = new SetPlayerHandler(plugin, new LanguageHandler(plugin));
        this.setHandler = new SetHandler(plugin);
        this.guiHandler = new GuiHandler(plugin);
        this.clearHandler = new ClearHandler(plugin, new LanguageHandler(plugin)); // Initialize with LanguageHandler
        this.reloadHandler = new ReloadHandler(plugin);
        this.languageHandler = new LanguageHandler(plugin); // Initialize LanguageHandler
        this.language = new Language(plugin); // Initialize Language handler
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            return handleCommand(sender, args);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Command /" + label + " failed safely.", t);
            sender.sendMessage(ChatColor.RED + "NewJoinMessage could not complete that command. Check the console.");
            return true;
        }
    }

    private boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // OPs / permitted players get the GUI (admin panel if admin); others get help
            if (sender instanceof Player && Perms.canOpenGui((Player) sender)) {
                return guiHandler.handleGuiCommand(sender);
            }
            displayHelpMenu(sender);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            displayHelpMenu(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("preview")) {
            if (!(sender instanceof Player) || !Perms.has(sender, "joinleave.preview")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to preview messages.");
                return true;
            }
            Player player = (Player) sender;
            player.sendMessage(ChatColor.GRAY + "Join preview: " + plugin.renderMessage(player, "join"));
            player.sendMessage(ChatColor.GRAY + "Leave preview: " + plugin.renderMessage(player, "leave"));
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (!(sender instanceof Player) || !Perms.has(sender, "joinleave.toggle")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to toggle messages.");
                return true;
            }
            boolean enabled = plugin.toggleBroadcast((Player) sender);
            sender.sendMessage((enabled ? ChatColor.GREEN : ChatColor.RED)
                    + "Your join/leave broadcasts are now " + (enabled ? "enabled." : "disabled."));
            return true;
        }

        if (args[0].equalsIgnoreCase("icon")) {
            if (!(sender instanceof Player) || !Perms.has(sender, "joinleave.icon")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to choose an icon.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /njm icon <icon|off>");
                return true;
            }
            String icon = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            if (icon.length() > 16) {
                sender.sendMessage(ChatColor.RED + "Icons can be at most 16 characters.");
                return true;
            }
            plugin.setIcon((Player) sender, icon);
            sender.sendMessage(ChatColor.GREEN + "Your icon is now " + ("off".equalsIgnoreCase(icon) ? "disabled." : icon));
            return true;
        }

        if (args[0].equalsIgnoreCase("sound")) {
            if (!(sender instanceof Player) || !Perms.has(sender, "joinleave.sound")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to choose sounds.");
                return true;
            }
            if (args.length != 3 || !(args[1].equalsIgnoreCase("join") || args[1].equalsIgnoreCase("leave"))) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /njm sound <join|leave> <sound|off>");
                return true;
            }
            if (!plugin.setSound((Player) sender, args[1].toLowerCase(), args[2])) {
                sender.sendMessage(ChatColor.RED + "That sound does not exist on this server version.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Your " + args[1].toLowerCase() + " sound was updated.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("info")) {
            return messageHandler.handleInfoCommand(sender, args);
        }

        if (args.length >= 4 && args[0].equalsIgnoreCase("setplayer")) {
            return setPlayerHandler.handleSetPlayerCommand(sender, args);
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("set")) {
            return setHandler.handleSetCommand(sender, args);
        }
        if (args[0].equalsIgnoreCase("language") && sender instanceof Player) {
            Player player = (Player) sender;
            language.openLanguageGUI(player);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            return guiHandler.handleGuiCommand(sender);
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("clear")) {
            return clearHandler.handleClearCommand(sender, args);
        }

        if (args[0].equalsIgnoreCase("reload")) {
            return reloadHandler.handleReloadCommand(sender);
        }

        // Display help menu
        displayHelpMenu(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("setplayer");
            subCommands.add("set");
            subCommands.add("gui");
            subCommands.add("language"); // Add 'language' to subCommands
            subCommands.add("clear");
            subCommands.add("reload");
            subCommands.add("info");
            subCommands.add("preview");
            subCommands.add("toggle");
            subCommands.add("icon");
            subCommands.add("sound");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setplayer")) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setplayer")) {
            List<String> messageTypes = new ArrayList<>();
            messageTypes.add("join");
            messageTypes.add("leave");
            StringUtil.copyPartialMatches(args[2], messageTypes, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            List<String> messageTypes = new ArrayList<>();
            messageTypes.add("join");
            messageTypes.add("leave");
            completions = messageTypes;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            List<String> messageTypes = new ArrayList<>();
            messageTypes.add("all");
            messageTypes.add("join");
            messageTypes.add("leave");
            StringUtil.copyPartialMatches(args[1], messageTypes, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sound")) {
            StringUtil.copyPartialMatches(args[1], Arrays.asList("join", "leave"), completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("icon")) {
            StringUtil.copyPartialMatches(args[1], Collections.singletonList("off"), completions);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("sound")) {
            StringUtil.copyPartialMatches(args[2], Collections.singletonList("off"), completions);
        }

        Collections.sort(completions);
        return completions;
    }

    private void displayHelpMenu(CommandSender sender) {
        Player player = (sender instanceof Player) ? (Player) sender : null;

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.DARK_PURPLE + "                NewJoinMessage Help");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "      NJM version: " + ChatColor.GREEN + plugin.getDescription().getVersion() + ChatColor.GREEN + " ✔");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "      Made With " + ChatColor.RED + "❤" + ChatColor.GRAY + " by Yanel");

        boolean hasSetJoinPermission = Perms.has(sender, "joinleave.set.join");
        boolean hasSetLeavePermission = Perms.has(sender, "joinleave.set.leave");
        boolean hasSetPlayerPermission = Perms.has(sender, "joinleave.setplayer");
        boolean hasClearPlayerPermission = Perms.has(sender, "joinleave.clearplayer");
        boolean hasReloadPermission = Perms.has(sender, "joinleave.reload");
        boolean hasGuiPermission = Perms.has(sender, "joinleave.gui");
        boolean hasInfoPermission = Perms.has(sender, "joinleave.info");
        boolean hasAnyPermission = Perms.isAdmin(sender) || hasSetJoinPermission || hasSetLeavePermission || hasSetPlayerPermission || hasClearPlayerPermission || hasReloadPermission || hasGuiPermission || hasInfoPermission;

        if (!hasAnyPermission) {
            sender.sendMessage(" ");
            sender.sendMessage(ChatColor.RED + languageHandler.getMessage(player, "help.noPermissions"));

            sender.sendMessage(" ");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + languageHandler.getMessage(player, "help.setLanguage"));

            sender.sendMessage(" ");
        } else {
            if (hasSetJoinPermission) {
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + languageHandler.getMessage(player, "help.setJoinMessage"));
            }
            if (hasSetLeavePermission) {
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + languageHandler.getMessage(player, "help.setLeaveMessage"));
            }
            if (hasSetPlayerPermission) {
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + languageHandler.getMessage(player, "help.setPlayerMessage"));
            }
            if (hasClearPlayerPermission) {
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + languageHandler.getMessage(player, "help.clearMessages"));
            }
            if (hasGuiPermission) {
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + languageHandler.getMessage(player, "help.openGui"));
            }
            if (hasInfoPermission) {
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + languageHandler.getMessage(player, "help.viewInfo"));
            }
            if (hasReloadPermission) {
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + languageHandler.getMessage(player, "help.reloadPlugin"));
            }
            sender.sendMessage(" ");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/njm preview" + ChatColor.DARK_PURPLE + " - Preview your messages");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/njm toggle" + ChatColor.DARK_PURPLE + " - Toggle your broadcasts");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/njm icon <icon|off>" + ChatColor.DARK_PURPLE + " - Choose an icon");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/njm sound <join|leave> <sound|off>" + ChatColor.DARK_PURPLE + " - Choose sounds");
        }

        if (player != null) {
            String joinMessage = plugin.getMessage(player, "join", "default-join-message");
            String leaveMessage = plugin.getMessage(player, "leave", "default-leave-message");
            sender.sendMessage(" ");
            sender.sendMessage(ChatColor.GREEN + languageHandler.getMessage(player, "help.currentJoinMessage") + ChatColor.RESET + (joinMessage.equals(plugin.getConfig().getString("default-join-message")) ? ChatColor.GRAY + languageHandler.getMessage(player, "help.usingDefaultMessage") : joinMessage));
            sender.sendMessage(ChatColor.GREEN + languageHandler.getMessage(player, "help.currentLeaveMessage") + ChatColor.RESET + (leaveMessage.equals(plugin.getConfig().getString("default-leave-message")) ? ChatColor.GRAY + languageHandler.getMessage(player, "help.usingDefaultMessage") : leaveMessage));
        }

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
