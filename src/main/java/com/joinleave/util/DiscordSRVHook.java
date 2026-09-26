package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional DiscordSRV integration. DiscordSRV is a soft dependency.
 * Allows sending join/leave messages to Discord.
 */
public final class DiscordSRVHook {

    private static final String PLUGIN_NAME = "DiscordSRV";
    private static final String API_CLASS = "github.scarsz.discordsrv.DiscordSRV";
    private static final String DESTINATION_CLASS = "github.scarsz.discordsrv.util.Destination";

    private static Method getPluginMethod;
    private static Method getMainGuildMethod;
    private static Method getTextChannelMethod;
    private static Method sendMessageMethod;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(DiscordSRVHook.class.getName());

    private DiscordSRVHook() {
    }

    /** Resolves DiscordSRV API once. Safe to call repeatedly. */
    public static synchronized void setup() {
        if (attempted) {
            return;
        }
        attempted = true;
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            getPluginMethod = apiClass.getMethod("getPlugin");
            Object discordSRV = getPluginMethod.invoke(null);

            Class<?> discordSRVClass = discordSRV.getClass();
            getMainGuildMethod = discordSRVClass.getMethod("getMainGuild");
        } catch (Throwable t) {
            logger.log(Level.FINE, "DiscordSRV found but API did not match; Discord features disabled.", t);
            getPluginMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return getPluginMethod != null;
    }

    /**
     * Sends a message to the main Discord channel.
     */
    public static void sendMessage(String message) {
        if (!isAvailable() || message == null || message.isEmpty()) {
            return;
        }
        try {
            Object discordSRV = getPluginMethod.invoke(null);
            Object guild = getMainGuildMethod.invoke(discordSRV);
            if (guild == null) {
                return;
            }
            Class<?> guildClass = Class.forName("net.dv8tion.jda.api.entities.Guild");
            Method getDefaultChannel = guildClass.getMethod("getDefaultChannel");
            Object channel = getDefaultChannel.invoke(guild);
            if (channel == null) {
                return;
            }
            Class<?> channelClass = Class.forName("net.dv8tion.jda.api.entities.TextChannel");
            sendMessageMethod = channelClass.getMethod("sendMessage", String.class);
            sendMessageMethod.invoke(channel, message);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Sends a join message to Discord.
     */
    public static void sendJoinMessage(Player player, String message) {
        if (!isAvailable() || player == null) {
            return;
        }
        String formatted = message
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName());
        sendMessage("🟢 " + formatted);
    }

    /**
     * Sends a leave message to Discord.
     */
    public static void sendLeaveMessage(Player player, String message) {
        if (!isAvailable() || player == null) {
            return;
        }
        String formatted = message
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName());
        sendMessage("🔴 " + formatted);
    }

    /**
     * Checks if a player is verified/linked with DiscordSRV.
     */
    public static boolean isVerified(Player player) {
        if (player == null || !isAvailable()) {
            return false;
        }
        try {
            Object discordSRV = getPluginMethod.invoke(null);
            Class<?> discordSRVClass = discordSRV.getClass();
            Method getAccountLinkManager = discordSRVClass.getMethod("getAccountLinkManager");
            Object accountLinkManager = getAccountLinkManager.invoke(discordSRV);
            Class<?> managerClass = Class.forName("github.scarsz.discordsrv.dependencies.jda.api.entities.User");
            Method isLinked = accountLinkManager.getClass().getMethod("isLinked", Player.class);
            Object result = isLinked.invoke(accountLinkManager, player);
            return result != null && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }
}