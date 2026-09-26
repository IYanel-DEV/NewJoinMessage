package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional ProtocolLib integration. ProtocolLib is a soft dependency.
 * Allows advanced packet manipulation for join/leave messages.
 */
public final class ProtocolLibHook {

    private static final String PLUGIN_NAME = "ProtocolLib";
    private static Method getProtocolManagerMethod;
    private static Method createPacketMethod;
    private static Method sendServerPacketMethod;
    private static Method addPacketListenerMethod;
    private static Method removePacketListenerMethod;
    private static Object protocolManager;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(ProtocolLibHook.class.getName());

    private ProtocolLibHook() {
    }

    /** Resolves ProtocolLib API once. Safe to call repeatedly. */
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
            Class<?> protocolLibraryClass = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            getProtocolManagerMethod = protocolLibraryClass.getMethod("getProtocolManager");
            protocolManager = getProtocolManagerMethod.invoke(null);

            Class<?> protocolManagerClass = Class.forName("com.comphenix.protocol.ProtocolManager");
            createPacketMethod = protocolManagerClass.getMethod("createPacket", Class.forName("com.comphenix.protocol.PacketType"));
            sendServerPacketMethod = protocolManagerClass.getMethod("sendServerPacket", Player.class, Class.forName("com.comphenix.protocol.events.PacketContainer"));
            addPacketListenerMethod = protocolManagerClass.getMethod("addPacketListener", Class.forName("com.comphenix.protocol.events.PacketAdapter"));
            removePacketListenerMethod = protocolManagerClass.getMethod("removePacketListener", Class.forName("com.comphenix.protocol.events.PacketAdapter"));
        } catch (Throwable t) {
            logger.log(Level.FINE, "ProtocolLib found but API did not match; packet features disabled.", t);
            getProtocolManagerMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return protocolManager != null;
    }

    public static Object getProtocolManager() {
        setup();
        return protocolManager;
    }

    public static void sendPacket(Player player, Object packet) {
        if (!isAvailable() || player == null || packet == null) {
            return;
        }
        try {
            sendServerPacketMethod.invoke(protocolManager, player, packet);
        } catch (Throwable ignored) {
        }
    }

    public static void broadcastPacket(Object packet) {
        if (!isAvailable() || packet == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPacket(player, packet);
        }
    }

    public static Object createChatPacket(String message, Player sender) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Class<?> packetTypeClass = Class.forName("com.comphenix.protocol.PacketType");
            Object packetType = packetTypeClass.getField("Play").get(null);
            Class<?> serverPacketClass = Class.forName("com.comphenix.protocol.PacketType$Play$Server");
            Object chatPacketType = serverPacketClass.getField("CHAT").get(null);

            Object packet = createPacketMethod.invoke(protocolManager, chatPacketType);

            Class<?> packetContainerClass = Class.forName("com.comphenix.protocol.events.PacketContainer");
            Class<?> wrappedChatComponentClass = Class.forName("com.comphenix.protocol.wrappers.WrappedChatComponent");
            Method fromTextMethod = wrappedChatComponentClass.getMethod("fromText", String.class);
            Object chatComponent = fromTextMethod.invoke(null, message);

            Method getChatComponentsMethod = packetContainerClass.getMethod("getChatComponents");
            Object chatComponents = getChatComponentsMethod.invoke(packet);
            Class<?> listClass = Class.forName("com.comphenix.protocol.utility.MinecraftReflection");
            Method writeMethod = chatComponents.getClass().getMethod("write", int.class, Object.class);
            writeMethod.invoke(chatComponents, 0, chatComponent);

            if (sender != null) {
                Method getStringsMethod = packetContainerClass.getMethod("getStrings");
                Object strings = getStringsMethod.invoke(packet);
                writeMethod.invoke(strings, 0, sender.getName());

                Method getIntegersMethod = packetContainerClass.getMethod("getIntegers");
                Object integers = getIntegersMethod.invoke(packet);
                Method writeIntMethod = integers.getClass().getMethod("write", int.class, int.class);
                writeIntMethod.invoke(integers, 0, 1);

                Method getUUIDsMethod = packetContainerClass.getMethod("getUUIDs");
                Object uuids = getUUIDsMethod.invoke(packet);
                writeMethod.invoke(uuids, 0, sender.getUniqueId());
            }
            return packet;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void addPacketListener(JavaPlugin plugin, Object listener) {
        if (!isAvailable() || listener == null) {
            return;
        }
        try {
            addPacketListenerMethod.invoke(protocolManager, listener);
        } catch (Throwable ignored) {
        }
    }

    public static void removePacketListener(JavaPlugin plugin, Object listener) {
        if (!isAvailable() || listener == null) {
            return;
        }
        try {
            removePacketListenerMethod.invoke(protocolManager, listener);
        } catch (Throwable ignored) {
        }
    }
}