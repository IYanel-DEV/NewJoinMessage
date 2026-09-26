package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional Multiverse-Core integration. Multiverse-Core is a soft dependency.
 * Provides multi-world support for join/leave messages.
 */
public final class MultiverseHook {

    private static final String PLUGIN_NAME = "Multiverse-Core";
    private static Method getMVWorldManagerMethod;
    private static Method getMVWorldMethod;
    private static Method getColoredWorldNameMethod;
    private static Method getAliasMethod;
    private static Method isLoadedMethod;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(MultiverseHook.class.getName());

    private MultiverseHook() {
    }

    /** Resolves Multiverse-Core API once. Safe to call repeatedly. */
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
            Class<?> multiverseCoreClass = Class.forName("com.onarandombox.MultiverseCore.MultiverseCore");
            getMVWorldManagerMethod = multiverseCoreClass.getMethod("getMVWorldManager");
            Object worldManager = getMVWorldManagerMethod.invoke(plugin);

            Class<?> worldManagerClass = Class.forName("com.onarandombox.MultiverseCore.api.MVWorldManager");
            getMVWorldMethod = worldManagerClass.getMethod("getMVWorld", String.class);

            Class<?> mvWorldClass = Class.forName("com.onarandombox.MultiverseCore.api.MultiverseWorld");
            getColoredWorldNameMethod = mvWorldClass.getMethod("getColoredName");
            getAliasMethod = mvWorldClass.getMethod("getAlias");
            isLoadedMethod = mvWorldClass.getMethod("isLoaded");
        } catch (Throwable t) {
            logger.log(Level.FINE, "Multiverse-Core found but API did not match; Multiverse placeholders will be empty.", t);
            getMVWorldManagerMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return getMVWorldManagerMethod != null;
    }

    public static String getColoredWorldName(World world) {
        if (world == null || !isAvailable()) {
            return world != null ? world.getName() : "";
        }
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            Object worldManager = getMVWorldManagerMethod.invoke(plugin);
            Object mvWorld = getMVWorldMethod.invoke(worldManager, world.getName());
            if (mvWorld == null) {
                return world.getName();
            }
            Object coloredName = getColoredWorldNameMethod.invoke(mvWorld);
            return coloredName == null ? world.getName() : String.valueOf(coloredName);
        } catch (Throwable ignored) {
            return world.getName();
        }
    }

    public static String getWorldAlias(World world) {
        if (world == null || !isAvailable()) {
            return world != null ? world.getName() : "";
        }
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            Object worldManager = getMVWorldManagerMethod.invoke(plugin);
            Object mvWorld = getMVWorldMethod.invoke(worldManager, world.getName());
            if (mvWorld == null) {
                return world.getName();
            }
            Object alias = getAliasMethod.invoke(mvWorld);
            return alias == null ? world.getName() : String.valueOf(alias);
        } catch (Throwable ignored) {
            return world.getName();
        }
    }

    public static boolean isWorldLoaded(World world) {
        if (world == null || !isAvailable()) {
            return world != null;
        }
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            Object worldManager = getMVWorldManagerMethod.invoke(plugin);
            Object mvWorld = getMVWorldMethod.invoke(worldManager, world.getName());
            if (mvWorld == null) {
                return true;
            }
            Object loaded = isLoadedMethod.invoke(mvWorld);
            return loaded != null && (Boolean) loaded;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static String getWorldDisplayName(Player player) {
        if (player == null) {
            return "";
        }
        World world = player.getWorld();
        String colored = getColoredWorldName(world);
        return colored.isEmpty() ? world.getName() : colored;
    }
}