package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional HolographicDisplays integration. HolographicDisplays is a soft dependency.
 * Allows creating holograms for join/leave messages.
 */
public final class HolographicDisplaysHook {

    private static final String PLUGIN_NAME = "HolographicDisplays";
    private static final String API_CLASS = "com.gmail.filoghost.holographicdisplays.api.HologramsAPI";
    private static final String HOLOGRAM_CLASS = "com.gmail.filoghost.holographicdisplays.api.Hologram";
    private static final String LINE_CLASS = "com.gmail.filoghost.holographicdisplays.api.line.TextLine";

    private static Method createHologramMethod;
    private static Method appendTextLineMethod;
    private static Method insertTextLineMethod;
    private static Method deleteHologramMethod;
    private static Method getHologramsMethod;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(HolographicDisplaysHook.class.getName());

    private HolographicDisplaysHook() {
    }

    /** Resolves HolographicDisplays API once. Safe to call repeatedly. */
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
            createHologramMethod = apiClass.getMethod("createHologram", Plugin.class, Location.class);
            getHologramsMethod = apiClass.getMethod("getHolograms", Plugin.class);

            Class<?> hologramClass = Class.forName(HOLOGRAM_CLASS);
            appendTextLineMethod = hologramClass.getMethod("appendTextLine", String.class);
            insertTextLineMethod = hologramClass.getMethod("insertTextLine", int.class, String.class);
            deleteHologramMethod = hologramClass.getMethod("delete");
        } catch (Throwable t) {
            logger.log(Level.FINE, "HolographicDisplays found but API did not match; hologram features disabled.", t);
            createHologramMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return createHologramMethod != null;
    }

    /**
     * Creates a hologram at the given location with the provided lines.
     */
    public static Object createHologram(Plugin plugin, Location location, String... lines) {
        if (!isAvailable() || plugin == null || location == null) {
            return null;
        }
        try {
            Object hologram = createHologramMethod.invoke(null, plugin, location);
            if (hologram == null || lines == null) {
                return hologram;
            }
            for (String line : lines) {
                if (line != null && !line.isEmpty()) {
                    appendTextLineMethod.invoke(hologram, line);
                }
            }
            return hologram;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Creates a temporary hologram that auto-deletes after the specified ticks.
     */
    public static void createTemporaryHologram(Plugin plugin, Location location, long ticks, String... lines) {
        Object hologram = createHologram(plugin, location, lines);
        if (hologram == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> deleteHologram(hologram), ticks);
    }

    /**
     * Deletes a hologram.
     */
    public static void deleteHologram(Object hologram) {
        if (!isAvailable() || hologram == null) {
            return;
        }
        try {
            deleteHologramMethod.invoke(hologram);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Gets all holograms owned by the plugin.
     */
    public static Iterable<?> getHolograms(Plugin plugin) {
        if (!isAvailable() || plugin == null) {
            return null;
        }
        try {
            return (Iterable<?>) getHologramsMethod.invoke(null, plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Creates a join hologram at the player's location.
     */
    public static void createJoinHologram(Plugin plugin, Player player, String message) {
        if (!isAvailable() || player == null) {
            return;
        }
        Location loc = player.getLocation().add(0, 2.5, 0);
        String formatted = message
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName());
        createTemporaryHologram(plugin, loc, 60L, "§a➤ " + formatted);
    }

    /**
     * Creates a leave hologram at the player's location.
     */
    public static void createLeaveHologram(Plugin plugin, Player player, String message) {
        if (!isAvailable() || player == null) {
            return;
        }
        Location loc = player.getLocation().add(0, 2.5, 0);
        String formatted = message
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName());
        createTemporaryHologram(plugin, loc, 60L, "§c➤ " + formatted);
    }
}