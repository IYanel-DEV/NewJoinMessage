package com.joinleave.Handler;

import com.joinleave.JoinleaveMessage;
import com.joinleave.util.Perms;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code /njm list [page]} - paginated overview of everyone with a custom
 * join or leave message.
 */
public class ListHandler {

    /** Keeps each page inside the chat window. */
    private static final int FALLBACK_PAGE_SIZE = 8;
    private static final int MAX_PAGE_SIZE = 20;
    private static final int MAX_MESSAGE_SHOW = 24;

    private final JoinleaveMessage plugin;

    public ListHandler(JoinleaveMessage plugin) {
        this.plugin = plugin;
    }

    public boolean handleListCommand(CommandSender sender, String[] args) {
        if (!Perms.has(sender, "joinleave.list")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to list players.");
            return true;
        }

        List<UUID> players = plugin.playersWithMessages();
        if (players.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No players have set a custom join or leave message yet.");
            return true;
        }

        int pageSize = pageSize();
        int totalPages = Math.max(1, (players.size() + pageSize - 1) / pageSize);

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not a page number.");
                return true;
            }
        }
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            sender.sendMessage(ChatColor.RED + "There " + (totalPages == 1 ? "is" : "are")
                    + " only " + totalPages + " page" + (totalPages == 1 ? "" : "s") + ".");
            return true;
        }

        int from = (page - 1) * pageSize;
        int to = Math.min(players.size(), from + pageSize);

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "━━━━━━ Players with custom messages ━━━━━━");
        sender.sendMessage(ChatColor.GRAY + "Showing " + (from + 1) + "-" + to
                + " of " + players.size() + "  " + ChatColor.DARK_PURPLE + "(page " + page + "/" + totalPages + ")");

        List<UUID> pageEntries = new ArrayList<UUID>(players.subList(from, to));
        for (UUID uuid : pageEntries) {
            sender.sendMessage(describe(uuid));
        }

        if (page < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "Next page: " + ChatColor.YELLOW + "/njm list " + (page + 1));
        }
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return true;
    }

    private String describe(UUID uuid) {
        String name = plugin.displayNameFor(uuid, uuid.toString().substring(0, 8));
        String join = abbreviate(plugin.getCustomMessage(uuid, "join"));
        String leave = abbreviate(plugin.getCustomMessage(uuid, "leave"));

        StringBuilder line = new StringBuilder();
        line.append(ChatColor.AQUA).append(name).append(ChatColor.GRAY).append("  ");
        line.append(ChatColor.GREEN).append("J: ").append(join);
        line.append(ChatColor.DARK_GRAY).append(" | ");
        line.append(ChatColor.RED).append("L: ").append(leave);

        StringBuilder tags = new StringBuilder();
        String icon = plugin.getIcon(uuid);
        if (icon != null && !icon.isEmpty()) {
            tags.append(ChatColor.GOLD).append("icon ");
        }
        if (!"off".equalsIgnoreCase(plugin.getSound(uuid, "join"))) {
            tags.append(ChatColor.LIGHT_PURPLE).append("sound ");
        }
        if (!plugin.isBroadcastEnabled(uuid)) {
            tags.append(ChatColor.GRAY).append("muted ");
        }
        if (tags.length() > 0) {
            line.append(ChatColor.DARK_GRAY).append(" [").append(tags).append(ChatColor.DARK_GRAY).append("]");
        }
        return line.toString();
    }

    /** Shows "(default)" for an unset message so a blank line is never confusing. */
    private String abbreviate(String message) {
        if (message == null) {
            return ChatColor.DARK_GRAY + "(default)";
        }
        String stripped = ChatColor.stripColor(message);
        if (stripped == null || stripped.trim().isEmpty()) {
            return ChatColor.DARK_GRAY + "(default)";
        }
        if (stripped.length() <= MAX_MESSAGE_SHOW) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_SHOW) + ChatColor.DARK_GRAY + "…";
    }

    private int pageSize() {
        int configured = plugin.getConfig().getInt("list-page-size", FALLBACK_PAGE_SIZE);
        if (configured <= 0) {
            return FALLBACK_PAGE_SIZE;
        }
        return Math.min(configured, MAX_PAGE_SIZE);
    }
}
