package com.joinleave;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public final class VanishAPI {
    private static final Set<Player> vanishedPlayers = new HashSet<Player>();

    private VanishAPI() {}

    public static void addVanishedPlayer(Player player) {
        vanishedPlayers.add(player);
    }

    public static void removeVanishedPlayer(Player player) {
        vanishedPlayers.remove(player);
    }

    public static boolean isVanished(Player player) {
        if (vanishedPlayers.contains(player)) {
            return true;
        }
        for (MetadataValue value : player.getMetadata("vanished")) {
            if (value.asBoolean()) {
                return true;
            }
        }
        try {
            Class<?> api = Class.forName("de.myzelyam.api.vanish.VanishAPI");
            Method invisible = api.getMethod("isInvisible", Player.class);
            return Boolean.TRUE.equals(invisible.invoke(null, player));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
