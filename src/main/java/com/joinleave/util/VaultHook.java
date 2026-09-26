package com.joinleave.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional Vault chat-prefix lookup. Vault is a soft dependency, so it is reached
 * entirely through reflection and the plugin works normally when it is absent.
 */
public final class VaultHook {

    private static final String CHAT_SERVICES = "net.milkbowl.vault.chat.ChatServices";

    private Object provider;
    private Method getPrefix;
    private boolean attempted;
    private final Logger logger;

    public VaultHook(Logger logger) {
        this.logger = logger;
    }

    /** Resolves Vault once. Safe to call repeatedly. */
    public synchronized void setup() {
        if (attempted) {
            return;
        }
        attempted = true;
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            Class<?> services = Class.forName(CHAT_SERVICES);
            provider = services.getMethod("getProvider").invoke(null);
            if (provider != null) {
                getPrefix = provider.getClass().getMethod("getPrefix", Player.class);
            }
        } catch (Throwable t) {
            // A Vault fork with a different API is not a reason to break anything.
            logger.log(Level.FINE, "Vault found but its chat API did not match; "
                    + "%prefix% will be empty.", t);
            provider = null;
            getPrefix = null;
        }
    }

    public boolean isAvailable() {
        setup();
        return provider != null && getPrefix != null;
    }

    /** @return the player's Vault prefix, or an empty string when unavailable. */
    public String prefix(Player player) {
        if (player == null || !isAvailable()) {
            return "";
        }
        try {
            Object result = getPrefix.invoke(provider, player);
            return result == null ? "" : String.valueOf(result);
        } catch (Throwable t) {
            return "";
        }
    }
}
