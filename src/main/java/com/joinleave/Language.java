package com.joinleave;

import com.joinleave.util.GuiMaterials;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Level;

public class Language implements Listener {

    private static final String TITLE = "Language Selection";

    private final Plugin plugin;
    private final LanguageManager languageManager;

    public Language(Plugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Open the GUI
    public void openLanguageGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 18, ChatColor.AQUA + TITLE);

        gui.setItem(0, createBaseheadItem("english",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNhYzk3NzRkYTEyMTcyNDg1MzJjZTE0N2Y3ODMxZjY3YTEyZmRjY2ExY2YwY2I0YjM4NDhkZTZiYzk0YjQifX19"));
        gui.setItem(1, createBaseheadItem("germany",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ODk5YjQ4MDY4NTg2OTdlMjgzZjA4NGQ5MTczZmU0ODc4ODY0NTM3NzQ2MjZiMjRiZDhjZmVjYzc3YjNmIn19fQ=="));
        gui.setItem(2, createBaseheadItem("french",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTEyNjlhMDY3ZWUzN2U2MzYzNWNhMWU3MjNiNjc2ZjEzOWRjMmRiZGRmZjk2YmJmZWY5OWQ4YjM1Yzk5NmJjIn19fQ=="));
        gui.setItem(3, createBaseheadItem("spanish",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGQzOTIzYjJkMDUwY2Q0MmNkOWZiYjg5ZWU2ODNhMmE5ODk5MzQ1ZTM1MThiZDZjN2YzY2JiNTNmZDE1MWQ3MiJ9fX0="));
        gui.setItem(4, createBaseheadItem("italian",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODVjZTg5MjIzZmE0MmZlMDZhZDY1ZDhkNDRjYTQxMmFlODk5YzgzMTMwOWQ2ODkyNGRmZTBkMTQyZmRiZWVhNCJ9fX0="));
        gui.setItem(5, createBaseheadItem("chinese",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2Y5YmMwMzVjZGM4MGYxYWI1ZTExOThmMjlmM2FkM2ZkZDJiNDJkOWE2OWFlYjY0ZGU5OTA2ODE4MDBiOThkYyJ9fX0="));
        gui.setItem(6, createBaseheadItem("japanese",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMDZjMmNhNzIzODY2NmFlMWI5ZGQ5ZGFhM2Q0ZmM4MjlkYjIyNjA5ZmI1NjkzMTJkZWMxZmIwYzhkNmRkNmMxZCJ9fX0="));
        gui.setItem(7, createBaseheadItem("korean",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2ExMjkxM2Q3ZGY2NDBkMThiY2M3YTQ1YTgxNzJjNjhmZmEwNDc1NmU4NGM2ZjBhMmVkYTNkYTQ1ZTAwZGFkZCJ9fX0="));
        gui.setItem(8, createBaseheadItem("russian",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTZlYWZlZjk4MGQ2MTE3ZGFiZTg5ODJhYzRiNDUwOTg4N2UyYzQ2MjFmNmE4ZmU1YzliNzM1YTgzZDc3NWFkIn19fQ=="));

        player.openInventory(gui);
    }

    // Create a custom basehead item with a given name and texture value
    private ItemStack createBaseheadItem(String name, String textureValue) {
        ItemStack headItem = new ItemStack(GuiMaterials.playerHead(), 1);
        if ("SKULL_ITEM".equals(headItem.getType().name())) {
            headItem.setDurability((short) 3);
        }
        ItemMeta meta = headItem.getItemMeta();
        if (meta instanceof SkullMeta) {
            meta.setDisplayName(ChatColor.GOLD + name);
            setSkinViaBase64((SkullMeta) meta, textureValue);
            headItem.setItemMeta(meta);
        }
        return headItem;
    }

    private void setSkinViaBase64(SkullMeta meta, String base64) {
        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);

            GameProfile profile = new GameProfile(UUID.randomUUID(), "skull-texture");
            profile.getProperties().put("textures", new Property("textures", base64));

            profileField.set(meta, profile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            plugin.getLogger().log(Level.SEVERE, "There was a severe internal reflection error when attempting to set the skin of a player skull via base64!", e);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = event.getView().getTitle();
        title = title == null ? "" : ChatColor.stripColor(title);
        if (!title.contains(TITLE)) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }
        String displayName = clicked.getItemMeta().getDisplayName();
        if (displayName == null) {
            return;
        }
        // Slot index decides the language; the display name is only a label.
        int slot = event.getRawSlot();
        if (slot < 0 || slot > 8) {
            return;
        }
        String chosen = LANGUAGE_BY_SLOT[slot];
        if (chosen == null) {
            return;
        }
        languageManager.setPlayerLanguage(player, chosen);
        player.sendMessage(getLanguageMessage(chosen, displayName));
        player.closeInventory();
    }

    private static final String[] LANGUAGE_BY_SLOT = {
            "english", "germany", "french", "spanish", "italian",
            "chinese", "japanese", "korean", "russian"
    };

    private String getLanguageMessage(String language, String displayName) {
        String label = displayName == null ? language : displayName;
        return ChatColor.GREEN + "Language set to " + label;
    }
}
