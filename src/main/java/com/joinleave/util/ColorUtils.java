package com.joinleave.util;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    private ColorUtils() {}

    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        if (ServerCompat.supportsHex()) {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String hex = matcher.group();
                String replacement;
                try {
                    replacement = ChatColor.of(hex).toString();
                } catch (Throwable t) {
                    replacement = "";
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            message = sb.toString();
        } else {
            message = HEX_PATTERN.matcher(message).replaceAll("");
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
