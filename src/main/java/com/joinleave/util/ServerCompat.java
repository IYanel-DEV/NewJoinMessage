package com.joinleave.util;

public final class ServerCompat {
    private static Boolean hexOverride = null;
    private static Boolean hexCached = null;

    private ServerCompat() {}

    public static void setHexSupportedForTest(Boolean override) {
        hexOverride = override;
    }

    public static boolean supportsHex() {
        if (hexOverride != null) {
            return hexOverride.booleanValue();
        }
        if (hexCached != null) {
            return hexCached.booleanValue();
        }
        try {
            net.md_5.bungee.api.ChatColor.class.getMethod("of", String.class);
            hexCached = Boolean.TRUE;
        } catch (Throwable t) {
            hexCached = Boolean.FALSE;
        }
        return hexCached.booleanValue();
    }
}
