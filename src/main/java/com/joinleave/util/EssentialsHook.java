package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional EssentialsX integration. EssentialsX is a soft dependency.
 * Provides access to vanish status, nicknames, AFK status, and more.
 */
public final class EssentialsHook {

    private static final String PLUGIN_NAME = "Essentials";
    private static final String USER_CLASS = "com.earth2me.essentials.User";
    private static final String ESSENTIALS_CLASS = "com.earth2me.essentials.Essentials";
    private static final String GET_USER_METHOD = "getUser";
    private static final String GET_NICKNAME_METHOD = "getNickname";
    private static final String IS_VANISHED_METHOD = "isVanished";
    private static final String IS_AFK_METHOD = "isAfk";

    private static Method getEssentialsMethod;
    private static Method getUserMethod;
    private static Method getNicknameMethod;
    private static Method isVanishedMethod;
    private static Method isAfkMethod;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(EssentialsHook.class.getName());

    private EssentialsHook() {
    }

    /** Resolves EssentialsX API once. Safe to call repeatedly. */
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
            Class<?> essentialsClass = Class.forName(ESSENTIALS_CLASS);
            getEssentialsMethod = essentialsClass.getMethod("getPlugin");
            Object essentials = getEssentialsMethod.invoke(null);

            getUserMethod = essentialsClass.getMethod(GET_USER_METHOD, Player.class);

            Class<?> userClass = Class.forName(USER_CLASS);
            getNicknameMethod = userClass.getMethod(GET_NICKNAME_METHOD);
            isVanishedMethod = userClass.getMethod(IS_VANISHED_METHOD);
            isAfkMethod = userClass.getMethod(IS_AFK_METHOD);
        } catch (Throwable t) {
            logger.log(Level.FINE, "EssentialsX found but API did not match; Essentials placeholders will be empty.", t);
            getEssentialsMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return getEssentialsMethod != null;
    }

    public static String getNickname(Player player) {
        if (player == null || !isAvailable()) {
            return "";
        }
        try {
            Object essentials = getEssentialsMethod.invoke(null);
            Object user = getUserMethod.invoke(essentials, player);
            if (user == null) {
                return "";
            }
            Object nickname = getNicknameMethod.invoke(user);
            return nickname == null ? "" : String.valueOf(nickname);
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static boolean isVanished(Player player) {
        if (player == null || !isAvailable()) {
            return false;
        }
        try {
            Object essentials = getEssentialsMethod.invoke(null);
            Object user = getUserMethod.invoke(essentials, player);
            if (user == null) {
                return false;
            }
            Object vanished = isVanishedMethod.invoke(user);
            return vanished != null && (Boolean) vanished;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isAfk(Player player) {
        if (player == null || !isAvailable()) {
            return false;
        }
        try {
            Object essentials = getEssentialsMethod.invoke(null);
            Object user = getUserMethod.invoke(essentials, player);
            if (user == null) {
                return false;
            }
            Object afk = isAfkMethod.invoke(user);
            return afk != null && (Boolean) afk;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String getDisplayName(Player player) {
        String nickname = getNickname(player);
        return nickname.isEmpty() ? player.getName() : nickname;
    }
}