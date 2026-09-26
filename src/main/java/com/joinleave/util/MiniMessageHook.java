package com.joinleave.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional MiniMessage (Adventure) integration. Adventure is a soft dependency.
 * Provides modern text formatting with MiniMessage syntax support.
 */
public final class MiniMessageHook {

    private static final String PLUGIN_NAME = "Adventure";
    private static final String MINIMESSAGE_CLASS = "net.kyori.adventure.text.minimessage.MiniMessage";
    private static final String LEGACY_SERIALIZER_CLASS = "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer";
    private static final String TAG_RESOLVER_CLASS = "net.kyori.adventure.text.minimessage.tag.resolver.TagResolver";
    private static final String COMPONENT_CLASS = "net.kyori.adventure.text.Component";

    private static Object miniMessage;
    private static Object legacySerializer;
    private static Method miniMessageMethod;
    private static Method legacySectionMethod;
    private static Method parseMethod;
    private static Method serializeMethod;
    private static Method deserializeMethod;
    private static Method resolverMethod;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(MiniMessageHook.class.getName());

    private MiniMessageHook() {
    }

    /** Resolves MiniMessage API once. Safe to call repeatedly. */
    public static synchronized void setup() {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            Class<?> miniMessageClass = Class.forName(MINIMESSAGE_CLASS);
            miniMessageMethod = miniMessageClass.getMethod("miniMessage");
            miniMessage = miniMessageMethod.invoke(null);

            Class<?> serializerClass = Class.forName(LEGACY_SERIALIZER_CLASS);
            legacySectionMethod = serializerClass.getMethod("legacySection");
            legacySerializer = legacySectionMethod.invoke(null);

            Class<?> miniMessageInstanceClass = miniMessage.getClass();
            parseMethod = miniMessageInstanceClass.getMethod("parse", String.class, Class.forName(TAG_RESOLVER_CLASS));
            serializeMethod = legacySerializer.getClass().getMethod("serialize", Class.forName(COMPONENT_CLASS));
            deserializeMethod = legacySerializer.getClass().getMethod("deserialize", String.class);

            Class<?> tagResolverClass = Class.forName(TAG_RESOLVER_CLASS);
            resolverMethod = tagResolverClass.getMethod("resolver", String.class, Object.class);
        } catch (Throwable t) {
            logger.log(Level.FINE, "MiniMessage (Adventure) found but API did not match; MiniMessage features disabled.", t);
            miniMessage = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return miniMessage != null;
    }

    public static String parseMiniMessage(String input, Player player) {
        if (!isAvailable() || input == null || input.isEmpty()) {
            return input;
        }
        try {
            Object resolver = null;
            if (player != null) {
                resolver = resolverMethod.invoke(null,
                        "player", player.getName(),
                        "displayname", player.getDisplayName(),
                        "world", player.getWorld() != null ? player.getWorld().getName() : "",
                        "online", String.valueOf(player.getServer().getOnlinePlayers().size()),
                        "max_players", String.valueOf(player.getServer().getMaxPlayers()),
                        "server_name", player.getServer().getName(),
                        "time", new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date())
                );
            }
            Object component = parseMethod.invoke(miniMessage, input, resolver);
            return (String) serializeMethod.invoke(legacySerializer, component);
        } catch (Throwable ignored) {
            return input;
        }
    }

    public static String parseMiniMessage(String input) {
        return parseMiniMessage(input, null);
    }

    public static Object deserializeLegacy(String input) {
        if (!isAvailable() || input == null) {
            return null;
        }
        try {
            return deserializeMethod.invoke(legacySerializer, input);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isMiniMessage(String input) {
        if (input == null) {
            return false;
        }
        return input.contains("<") && input.contains(">") && input.indexOf("<") < input.indexOf(">");
    }

    public static String parseAuto(String input, Player player) {
        if (input == null) {
            return "";
        }
        if (isMiniMessage(input)) {
            return parseMiniMessage(input, player);
        }
        return input;
    }
}