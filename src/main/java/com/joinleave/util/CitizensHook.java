package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional Citizens integration. Citizens is a soft dependency.
 * Provides NPC detection and NPC name/trait access for join/leave messages.
 */
public final class CitizensHook {

    private static final String PLUGIN_NAME = "Citizens";
    private static Method getNPCSelectorMethod;
    private static Method getNPCMethod;
    private static Method getNameMethod;
    private static Method getFullNameMethod;
    private static Method getTraitsMethod;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(CitizensHook.class.getName());

    private CitizensHook() {
    }

    /** Resolves Citizens API once. Safe to call repeatedly. */
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
            Class<?> citizensAPIClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
            getNPCSelectorMethod = citizensAPIClass.getMethod("getNPCSelector");
            Object selector = getNPCSelectorMethod.invoke(null);

            Class<?> selectorClass = Class.forName("net.citizensnpcs.api.npc.NPCSelector");
            getNPCMethod = selectorClass.getMethod("getNPC", Entity.class);

            Class<?> npcClass = Class.forName("net.citizensnpcs.api.npc.NPC");
            getNameMethod = npcClass.getMethod("getName");
            getFullNameMethod = npcClass.getMethod("getFullName");
            getTraitsMethod = npcClass.getMethod("getTraits");
        } catch (Throwable t) {
            logger.log(Level.FINE, "Citizens found but API did not match; Citizens placeholders will be empty.", t);
            getNPCSelectorMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return getNPCSelectorMethod != null;
    }

    public static boolean isNPC(Entity entity) {
        if (entity == null || !isAvailable()) {
            return false;
        }
        try {
            Object selector = getNPCSelectorMethod.invoke(null);
            Object npc = getNPCMethod.invoke(selector, entity);
            return npc != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String getNPCName(Entity entity) {
        if (entity == null || !isAvailable()) {
            return "";
        }
        try {
            Object selector = getNPCSelectorMethod.invoke(null);
            Object npc = getNPCMethod.invoke(selector, entity);
            if (npc == null) {
                return "";
            }
            Object name = getFullNameMethod.invoke(npc);
            return name == null ? "" : String.valueOf(name);
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static boolean hasTrait(Entity entity, String traitName) {
        if (entity == null || !isAvailable() || traitName == null) {
            return false;
        }
        try {
            Object selector = getNPCSelectorMethod.invoke(null);
            Object npc = getNPCMethod.invoke(selector, entity);
            if (npc == null) {
                return false;
            }
            Object traits = getTraitsMethod.invoke(npc);
            if (traits instanceof Iterable) {
                for (Object trait : (Iterable<?>) traits) {
                    if (trait.getClass().getSimpleName().equalsIgnoreCase(traitName)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static Object getNPC(Entity entity) {
        if (entity == null || !isAvailable()) {
            return null;
        }
        try {
            Object selector = getNPCSelectorMethod.invoke(null);
            Object npc = getNPCMethod.invoke(selector, entity);
            return npc;
        } catch (Throwable ignored) {
            return null;
        }
    }
}