package com.joinleave;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishAPI implements Listener {

    /** Tracked by UUID so a quit can always release the entry. */
    private static final Map<UUID, Boolean> vanishedPlayers = new ConcurrentHashMap<UUID, Boolean>();

    private static volatile boolean legacyApiResolved;
    private static Method legacyIsInvisible;

    private VanishAPI() {
    }

    public static void register(JoinleaveMessage plugin) {
        plugin.getServer().getPluginManager().registerEvents(new VanishQuitListener(), plugin);
    }

    public static void addVanishedPlayer(Player player) {
        if (player != null) {
            vanishedPlayers.put(player.getUniqueId(), Boolean.TRUE);
        }
    }

    public static void removeVanishedPlayer(Player player) {
        if (player != null) {
            vanishedPlayers.remove(player.getUniqueId());
        }
    }

    public static boolean isVanished(Player player) {
        if (player == null) {
            return false;
        }
        if (vanishedPlayers.containsKey(player.getUniqueId())) {
            return true;
        }
        for (MetadataValue value : player.getMetadata("vanished")) {
            if (value.asBoolean()) {
                return true;
            }
        }
        return legacyVanished(player);
    }

    /** Reflective lookup happens once, not on every join and quit. */
    private static boolean legacyVanished(Player player) {
        if (!legacyApiResolved) {
            synchronized (VanishAPI.class) {
                if (!legacyApiResolved) {
                    try {
                        Class<?> api = Class.forName("de.myzelyam.api.vanish.VanishAPI");
                        legacyIsInvisible = api.getMethod("isInvisible", Player.class);
                    } catch (Throwable ignored) {
                        legacyIsInvisible = null;
                    }
                    legacyApiResolved = true;
                }
            }
        }
        if (legacyIsInvisible == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(legacyIsInvisible.invoke(null, player));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class VanishQuitListener implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            // Without this the map would hold a UUID for every player who ever vanished.
            vanishedPlayers.remove(event.getPlayer().getUniqueId());
        }
    }
}
