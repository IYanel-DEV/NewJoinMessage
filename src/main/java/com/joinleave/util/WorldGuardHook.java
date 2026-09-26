package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional WorldGuard integration. WorldGuard is a soft dependency.
 * Provides region-based features for join/leave messages.
 */
public final class WorldGuardHook {

    private static final String PLUGIN_NAME = "WorldGuard";
    private static Method getPlatformMethod;
    private static Method getRegionContainerMethod;
    private static Method getRegionQueryMethod;
    private static Method getApplicableRegionsMethod;
    private static Method getIdMethod;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(WorldGuardHook.class.getName());

    private WorldGuardHook() {
    }

    /** Resolves WorldGuard API once. Safe to call repeatedly. */
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
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            getPlatformMethod = worldGuardClass.getMethod("getInstance");
            Object platform = getPlatformMethod.invoke(null);

            Class<?> platformClass = Class.forName("com.sk89q.worldguard.protection.Platform");
            getRegionContainerMethod = platformClass.getMethod("getRegionContainer");

            Object container = getRegionContainerMethod.invoke(platform);
            Class<?> containerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
            getRegionQueryMethod = containerClass.getMethod("createQuery");

            Object query = getRegionQueryMethod.invoke(container);
            Class<?> queryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            getApplicableRegionsMethod = queryClass.getMethod("getApplicableRegions", Class.forName("com.sk89q.worldedit.math.BlockVector3"));

            Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
            getIdMethod = protectedRegionClass.getMethod("getId");
        } catch (Throwable t) {
            logger.log(Level.FINE, "WorldGuard found but API did not match; region features disabled.", t);
            getPlatformMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return getPlatformMethod != null;
    }

    public static Set<String> getRegionsAt(Player player) {
        Set<String> regions = new HashSet<>();
        if (player == null || !isAvailable()) {
            return regions;
        }
        try {
            Object platform = getPlatformMethod.invoke(null);
            Object container = getRegionContainerMethod.invoke(platform);
            Object query = getRegionQueryMethod.invoke(container);

            Location loc = player.getLocation();
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method asBlockVectorMethod = bukkitAdapterClass.getMethod("asBlockVector", Location.class);
            Object vector = asBlockVectorMethod.invoke(null, loc);

            Object applicableRegions = getApplicableRegionsMethod.invoke(query, vector);
            if (applicableRegions != null) {
                Class<?> applicableRegionSetClass = Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
                Method iteratorMethod = applicableRegionSetClass.getMethod("iterator");
                Object iterator = iteratorMethod.invoke(applicableRegions);
                Method hasNextMethod = iterator.getClass().getMethod("hasNext");
                Method nextMethod = iterator.getClass().getMethod("next");
                while ((Boolean) hasNextMethod.invoke(iterator)) {
                    Object region = nextMethod.invoke(iterator);
                    Object id = getIdMethod.invoke(region);
                    if (id != null) {
                        regions.add(String.valueOf(id));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return regions;
    }

    public static boolean isInRegion(Player player, String regionName) {
        if (player == null || regionName == null || !isAvailable()) {
            return false;
        }
        Set<String> regions = getRegionsAt(player);
        return regions.contains(regionName);
    }

    public static boolean isInAnyRegion(Player player, Set<String> regionNames) {
        if (player == null || regionNames == null || regionNames.isEmpty() || !isAvailable()) {
            return false;
        }
        Set<String> regions = getRegionsAt(player);
        for (String region : regionNames) {
            if (regions.contains(region)) {
                return true;
            }
        }
        return false;
    }

    public static String getFirstRegion(Player player) {
        Set<String> regions = getRegionsAt(player);
        return regions.isEmpty() ? "" : regions.iterator().next();
    }
}