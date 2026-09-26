package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Optional PlaceholderAPI integration. PlaceholderAPI is a soft dependency.
 * When present, it allows using any PlaceholderAPI placeholder in join/leave messages.
 */
public final class PlaceholderAPIHook {

    private static final String PLUGIN_NAME = "PlaceholderAPI";
    private static final String CLASS_NAME = "me.clip.placeholderapi.PlaceholderAPI";

    private static Method setPlaceholdersMethod;
    private static boolean attempted;

    private PlaceholderAPIHook() {
    }

    /** Resolves PlaceholderAPI once. Safe to call repeatedly. */
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
            Class<?> apiClass = Class.forName(CLASS_NAME);
            setPlaceholdersMethod = apiClass.getMethod("setPlaceholders", Player.class, String.class);
        } catch (Throwable ignored) {
            setPlaceholdersMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return setPlaceholdersMethod != null;
    }

    /**
     * Parses PlaceholderAPI placeholders in the given message for the player.
     * @return the parsed message, or the original message if PlaceholderAPI is unavailable.
     */
    public static String parsePlaceholders(Player player, String message) {
        if (player == null || message == null || !isAvailable()) {
            return message;
        }
        try {
            Object result = setPlaceholdersMethod.invoke(null, player, message);
            return result == null ? message : String.valueOf(result);
        } catch (Throwable ignored) {
            return message;
        }
    }

    /**
     * Parses PlaceholderAPI placeholders without a player (for server-wide placeholders).
     */
    public static String parsePlaceholders(String message) {
        if (message == null || !isAvailable()) {
            return message;
        }
        try {
            Method staticMethod = Class.forName(CLASS_NAME).getMethod("setPlaceholders", String.class);
            Object result = staticMethod.invoke(null, message);
            return result == null ? message : String.valueOf(result);
        } catch (Throwable ignored) {
            return message;
        }
    }
}