package com.joinleave.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** 1.8-safe materials (legacy enum names vs 1.13+). */
public final class GuiMaterials {
    private GuiMaterials() {}

    public static Material of(String modern, String legacy) {
        try {
            return Material.valueOf(modern);
        } catch (IllegalArgumentException e) {
            try {
                return Material.valueOf(legacy);
            } catch (IllegalArgumentException e2) {
                return Material.STONE;
            }
        }
    }

    public static ItemStack paneGray() {
        Material m = of("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        if ("STAINED_GLASS_PANE".equals(m.name())) {
            return new ItemStack(m, 1, (short) 7);
        }
        return new ItemStack(m);
    }

    public static ItemStack panePurple() {
        Material m = of("PURPLE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        if ("STAINED_GLASS_PANE".equals(m.name())) {
            return new ItemStack(m, 1, (short) 10);
        }
        return new ItemStack(m);
    }

    public static Material greenBlock() {
        return of("LIME_WOOL", "EMERALD_BLOCK");
    }

    public static Material redBlock() {
        return of("RED_WOOL", "REDSTONE_BLOCK");
    }

    public static Material book() {
        return Material.BOOK;
    }

    public static Material barrier() {
        return of("BARRIER", "BEDROCK");
    }

    public static Material playerHead() {
        return of("PLAYER_HEAD", "SKULL_ITEM");
    }

    public static Material commandBlock() {
        return of("COMMAND_BLOCK", "COMMAND");
    }

    public static Material arrow() {
        return Material.ARROW;
    }

    public static Material tnt() {
        return Material.TNT;
    }
}
