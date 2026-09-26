package com.joinleave.util;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional LuckPerms integration. LuckPerms is a soft dependency.
 * Provides access to player groups, prefixes, suffixes, and permissions.
 */
public final class LuckPermsHook {

    private static final String PLUGIN_NAME = "LuckPerms";
    private static final String API_CLASS = "net.luckperms.api.LuckPerms";
    private static final String USER_CLASS = "net.luckperms.api.model.user.User";
    private static final String NODE_CLASS = "net.luckperms.api.node.Node";
    private static final String CONTEXT_CLASS = "net.luckperms.api.context.Context";
    private static final String CONTEXT_SET_CLASS = "net.luckperms.api.context.ContextSet";
    private static final String META_DATA_CLASS = "net.luckperms.api.model.user.UserManager";
    private static final String PROVIDER_CLASS = "net.luckperms.api.LuckPermsProvider";

    private static Method getUserManagerMethod;
    private static Method getUserMethod;
    private static Method getPrimaryGroupMethod;
    private static Method getCachedDataMethod;
    private static Method getMetaDataMethod;
    private static Method getPrefixMethod;
    private static Method getSuffixMethod;
    private static Method getNodesMethod;
    private static Method getContextsMethod;
    private static Method getProviderMethod;
    private static boolean attempted;
    private static final Logger logger = Logger.getLogger(LuckPermsHook.class.getName());

    private LuckPermsHook() {
    }

    /** Resolves LuckPerms API once. Safe to call repeatedly. */
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
            Class<?> providerClass = Class.forName(PROVIDER_CLASS);
            getProviderMethod = providerClass.getMethod("get");
            Object provider = getProviderMethod.invoke(null);

            Class<?> apiClass = Class.forName(API_CLASS);
            getUserManagerMethod = apiClass.getMethod("getUserManager");
            Object userManager = getUserManagerMethod.invoke(provider);

            Class<?> userManagerClass = Class.forName(META_DATA_CLASS);
            getUserMethod = userManagerClass.getMethod("getUser", UUID.class);

            Class<?> userClass = Class.forName(USER_CLASS);
            getPrimaryGroupMethod = userClass.getMethod("getPrimaryGroup");
            getCachedDataMethod = userClass.getMethod("getCachedData");
            Class<?> metaDataClass = Class.forName("net.luckperms.api.model.user.User$MetaData");
            getPrefixMethod = metaDataClass.getMethod("getPrefix");
            getSuffixMethod = metaDataClass.getMethod("getSuffix");
            getNodesMethod = userClass.getMethod("getNodes");
            getContextsMethod = userClass.getMethod("getContexts");
        } catch (Throwable t) {
            logger.log(Level.FINE, "LuckPerms found but API did not match; LuckPerms placeholders will be empty.", t);
            getProviderMethod = null;
        }
    }

    public static boolean isAvailable() {
        setup();
        return getProviderMethod != null;
    }

    public static String getPrefix(Player player) {
        if (player == null || !isAvailable()) {
            return "";
        }
        return getMeta(player, getPrefixMethod);
    }

    public static String getSuffix(Player player) {
        if (player == null || !isAvailable()) {
            return "";
        }
        return getMeta(player, getSuffixMethod);
    }

    public static String getPrimaryGroup(Player player) {
        if (player == null || !isAvailable()) {
            return "";
        }
        try {
            Object provider = getProviderMethod.invoke(null);
            Object userManager = getUserManagerMethod.invoke(provider);
            Object user = getUserMethod.invoke(userManager, player.getUniqueId());
            if (user == null) {
                return "";
            }
            Object group = getPrimaryGroupMethod.invoke(user);
            return group == null ? "" : String.valueOf(group);
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static Set<String> getGroups(Player player) {
        if (player == null || !isAvailable()) {
            return ImmutableSet.of();
        }
        try {
            Object provider = getProviderMethod.invoke(null);
            Object userManager = getUserManagerMethod.invoke(provider);
            Object user = getUserMethod.invoke(userManager, player.getUniqueId());
            if (user == null) {
                return ImmutableSet.of();
            }
            Object nodes = getNodesMethod.invoke(user);
            if (nodes instanceof List) {
                List<?> nodeList = (List<?>) nodes;
                ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                for (Object node : nodeList) {
                    try {
                        Method getKey = node.getClass().getMethod("getKey");
                        String key = String.valueOf(getKey.invoke(node));
                        if (key.startsWith("group.")) {
                            builder.add(key.substring(6));
                        }
                    } catch (Throwable ignored) {
                    }
                }
                return builder.build();
            }
        } catch (Throwable ignored) {
        }
        return ImmutableSet.of();
    }

    public static boolean hasPermission(Player player, String permission) {
        if (player == null || !isAvailable()) {
            return false;
        }
        try {
            Object provider = getProviderMethod.invoke(null);
            Object userManager = getUserManagerMethod.invoke(provider);
            Object user = getUserMethod.invoke(userManager, player.getUniqueId());
            if (user == null) {
                return false;
            }
            Object nodes = getNodesMethod.invoke(user);
            if (nodes instanceof List) {
                List<?> nodeList = (List<?>) nodes;
                for (Object node : nodeList) {
                    try {
                        Method getKey = node.getClass().getMethod("getKey");
                        String key = String.valueOf(getKey.invoke(node));
                        if (key.equalsIgnoreCase(permission) || key.equalsIgnoreCase("*")) {
                            return true;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static String getMeta(Player player, Method metaMethod) {
        try {
            Object provider = getProviderMethod.invoke(null);
            Object userManager = getUserManagerMethod.invoke(provider);
            Object user = getUserMethod.invoke(userManager, player.getUniqueId());
            if (user == null) {
                return "";
            }
            Object cachedData = getCachedDataMethod.invoke(user);
            if (cachedData == null) {
                return "";
            }
            Object meta = metaMethod.invoke(cachedData);
            return meta == null ? "" : String.valueOf(meta);
        } catch (Throwable ignored) {
            return "";
        }
    }
}