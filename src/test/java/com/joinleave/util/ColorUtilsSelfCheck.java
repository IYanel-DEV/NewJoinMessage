package com.joinleave.util;

public final class ColorUtilsSelfCheck {
    public static void main(String[] args) {
        ServerCompat.setHexSupportedForTest(Boolean.FALSE);
        String noHex = ColorUtils.colorize("#FF0000Hello &aWorld");
        if (noHex.contains("#FF0000")) {
            throw new AssertionError("hex should be stripped when unsupported, got: " + noHex);
        }
        if (!noHex.contains("World")) {
            throw new AssertionError("text lost: " + noHex);
        }

        ServerCompat.setHexSupportedForTest(Boolean.TRUE);
        // Without ChatColor.of on classpath in unit run, of() may fail — colorize must not throw
        String safe = ColorUtils.colorize("#00FF00Hi &bThere");
        if (safe == null || !safe.contains("There")) {
            throw new AssertionError("colorize failed open: " + safe);
        }

        ServerCompat.setHexSupportedForTest(null);
        if (ColorUtils.colorize(null) == null || !ColorUtils.colorize(null).isEmpty()) {
            // colorize(null) must return ""
            if (!"".equals(ColorUtils.colorize(null))) {
                throw new AssertionError("null must become empty string");
            }
        }

        System.out.println("ColorUtilsSelfCheck OK");
    }
}
