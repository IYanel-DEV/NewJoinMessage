package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional MVdWPlaceholderAPI integration. MVdWPlaceholderAPI is a soft dependency.
 * Another popular placeholder plugin for broader compatibility.
 */
public final class MVdWPlaceholderAPIHook {

    private static final String PLUGIN_NAME = "MVdWPlaceholderAPI";
    private static final String CLASS_NAME = "be.maximvdw.placeholderapi.MVdWPlaceholderAPI";

    private static Method replacePlaceholdersMethod;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(MVdWPlaceholderAPIHook.class.getName());

    private MVdWPlaceholderAPIHook() {
    }

    /** Resolves MVdWPlaceholderAPI once. Safe to call repeatedly. */
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
            replacePlaceholdersMethod = apiClass.getMethod("replacePlaceholders", Player.class, String.class);
        } catch (Throwable t) {
            logger.log(Level.FINE, "MVdWPlaceholderAPI found but API did not match; placeholders will be empty.", t);
            replacePlaceholdersMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return replacePlaceholdersMethod != null;
    }

    /**
     * Parses MVdWPlaceholderAPI placeholders in the given message for the player.
     */
    public static String parsePlaceholders(Player player, String message) {
        if (player == null || message == null || !isAvailable()) {
            return message;
        }
        try {
            Object result = replacePlaceholdersMethod.invoke(null, player, message);
            return result == null ? message : String.valueOf(result);
        } catch (Throwable ignored) {
            return message;
        }
    }

    /**
     * Parses MVdWPlaceholderAPI placeholders without a player.
     */
    public static String parsePlaceholders(String message) {
        if (message == null || !isAvailable()) {
            return message;
        }
        try {
            Class<?> apiClass = Class.forName(CLASS_NAME);
            Method staticMethod = apiClass.getMethod("replacePlaceholders", String.class);
            Object result = staticMethod.invoke(null, message);
            return result == null ? message : String.valueOf(result);
        } catch (Throwable ignored) {
            return message;
        }
    }
}