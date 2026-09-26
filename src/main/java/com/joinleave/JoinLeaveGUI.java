package com.joinleave;

import com.joinleave.util.GuiMaterials;
import com.joinleave.util.Perms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Menus dispatch on slot index, never on item display text. Matching a stripped
 * display name meant a player called "Closer" or "Backspace" could trigger the
 * Back or Close button, and any wording change silently broke a button.
 */
public class JoinLeaveGUI implements Listener {

    public static final String TITLE_USER = "NJM Settings";
    public static final String TITLE_ADMIN = "NJM Admin Panel";
    public static final String TITLE_PLAYERS = "NJM Player Management";

    // User menu slots
    private static final int U_JOIN = 11;
    private static final int U_LEAVE = 15;
    private static final int U_CLEAR = 22;
    private static final int U_HELP = 4;
    private static final int U_JOIN_SOUND = 10;
    private static final int U_ICON = 12;
    private static final int U_PREVIEW = 13;
    private static final int U_TOGGLE = 14;
    private static final int U_LEAVE_SOUND = 16;

    // Admin menu slots
    private static final int A_PERSONAL = 4;
    private static final int A_JOIN = 10;
    private static final int A_LEAVE = 12;
    private static final int A_MANAGEMENT = 22;
    private static final int A_SET_PLAYER_JOIN = 28;
    private static final int A_SET_PLAYER_LEAVE = 30;
    private static final int A_INFO = 32;
    private static final int A_CLEAR_PLAYER = 34;
    private static final int A_RELOAD = 40;
    private static final int A_CLOSE = 44;

    // Player management slots
    private static final int P_BACK = 45;
    private static final int P_CLOSE = 53;
    private static final int P_FIRST_SLOT = 0;
    private static final int P_LAST_SLOT = 44;

    private final JavaPlugin plugin;
    private final Map<UUID, String> pendingType = new HashMap<UUID, String>();
    private final Map<UUID, String> pendingTarget = new HashMap<UUID, String>();
    /** slot -> target player, per viewer, so no name parsing is needed. */
    private final Map<UUID, Map<Integer, UUID>> playerSlots = new HashMap<UUID, Map<Integer, UUID>>();

    public JoinLeaveGUI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private JoinleaveMessage njm() {
        return (JoinleaveMessage) plugin;
    }

    public void openGUI(Player player) {
        if (Perms.isAdmin(player) || Perms.has(player, "joinleave.setplayer")) {
            openAdminGUI(player);
        } else {
            openUserGUI(player);
        }
    }

    public void openUserGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE_USER);
        gui.setItem(U_JOIN, button(GuiMaterials.greenBlock(), ChatColor.GREEN + "" + ChatColor.BOLD + "Set Join Message",
                Arrays.asList(ChatColor.GRAY + "Click to set your join message",
                        ChatColor.YELLOW + "Current: " + current(player, "join"))));
        gui.setItem(U_LEAVE, button(GuiMaterials.redBlock(), ChatColor.RED + "" + ChatColor.BOLD + "Set Leave Message",
                Arrays.asList(ChatColor.GRAY + "Click to set your leave message",
                        ChatColor.YELLOW + "Current: " + current(player, "leave"))));
        gui.setItem(U_CLEAR, button(GuiMaterials.barrier(), ChatColor.DARK_RED + "" + ChatColor.BOLD + "Clear My Messages",
                Collections.singletonList(ChatColor.GRAY + "Clear your join and leave messages")));
        gui.setItem(U_HELP, button(GuiMaterials.book(), ChatColor.YELLOW + "" + ChatColor.BOLD + "Help",
                Collections.singletonList(ChatColor.GRAY + "Click for /njm help")));
        gui.setItem(U_JOIN_SOUND, button(Material.NOTE_BLOCK, ChatColor.GREEN + "Join Sound",
                Collections.singletonList(ChatColor.GRAY + "Set a sound name or off")));
        gui.setItem(U_ICON, button(Material.NAME_TAG, ChatColor.GOLD + "Icon",
                Collections.singletonList(ChatColor.GRAY + "Set an icon or off")));
        gui.setItem(U_PREVIEW, button(Material.COMPASS, ChatColor.AQUA + "Preview Messages",
                Collections.singletonList(ChatColor.GRAY + "See both messages now")));
        gui.setItem(U_TOGGLE, button(Material.LEVER, ChatColor.YELLOW + "Toggle Broadcasts",
                Collections.singletonList(ChatColor.GRAY + "Current: " + (
                        njm().isBroadcastEnabled(player) ? ChatColor.GREEN + "On" : ChatColor.RED + "Off"))));
        gui.setItem(U_LEAVE_SOUND, button(Material.NOTE_BLOCK, ChatColor.RED + "Leave Sound",
                Collections.singletonList(ChatColor.GRAY + "Set a sound name or off")));
        fill(gui, GuiMaterials.panePurple());
        player.openInventory(gui);
    }

    public void openAdminGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, TITLE_ADMIN);

        gui.setItem(A_PERSONAL, button(GuiMaterials.book(), ChatColor.GOLD + "" + ChatColor.BOLD + "Personal Settings",
                Collections.singletonList(ChatColor.GRAY + "Your own join/leave messages")));
        gui.setItem(A_JOIN, button(GuiMaterials.greenBlock(), ChatColor.GREEN + "" + ChatColor.BOLD + "Set Your Join Message",
                Collections.singletonList(ChatColor.GRAY + "Set your personal join message")));
        gui.setItem(A_LEAVE, button(GuiMaterials.redBlock(), ChatColor.RED + "" + ChatColor.BOLD + "Set Your Leave Message",
                Collections.singletonList(ChatColor.GRAY + "Set your personal leave message")));
        gui.setItem(A_MANAGEMENT, button(GuiMaterials.commandBlock(), ChatColor.DARK_RED + "" + ChatColor.BOLD + "Player Management",
                Collections.singletonList(ChatColor.GRAY + "Manage online players")));
        gui.setItem(A_SET_PLAYER_JOIN, button(GuiMaterials.greenBlock(), ChatColor.GREEN + "Set Player Join",
                Arrays.asList(ChatColor.GRAY + "Type player name, then message", ChatColor.DARK_GRAY + "Needs joinleave.setplayer")));
        gui.setItem(A_SET_PLAYER_LEAVE, button(GuiMaterials.redBlock(), ChatColor.RED + "Set Player Leave",
                Arrays.asList(ChatColor.GRAY + "Type player name, then message", ChatColor.DARK_GRAY + "Needs joinleave.setplayer")));
        gui.setItem(A_INFO, button(GuiMaterials.book(), ChatColor.YELLOW + "Player Info",
                Collections.singletonList(ChatColor.GRAY + "Type a player name to view messages")));
        gui.setItem(A_CLEAR_PLAYER, button(GuiMaterials.tnt(), ChatColor.DARK_RED + "Clear Player Messages",
                Collections.singletonList(ChatColor.GRAY + "Type a player name to clear")));
        gui.setItem(A_RELOAD, button(Material.COMPASS, ChatColor.GOLD + "" + ChatColor.BOLD + "Reload Plugin",
                Collections.singletonList(ChatColor.GRAY + "Reload NewJoinMessage config")));
        gui.setItem(A_CLOSE, button(GuiMaterials.barrier(), ChatColor.RED + "Close", Collections.<String>emptyList()));

        fill(gui, GuiMaterials.panePurple());
        player.openInventory(gui);
    }

    public void openPlayerManagement(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, TITLE_PLAYERS);
        Map<Integer, UUID> slots = new HashMap<Integer, UUID>();
        List<Player> online = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        int slot = P_FIRST_SLOT;
        for (Player target : online) {
            if (slot > P_LAST_SLOT) {
                break;
            }
            gui.setItem(slot, playerHead(target));
            slots.put(slot, target.getUniqueId());
            slot++;
        }
        gui.setItem(P_BACK, button(GuiMaterials.arrow(), ChatColor.GRAY + "Back",
                Collections.singletonList(ChatColor.DARK_GRAY + "Return to admin panel")));
        gui.setItem(P_CLOSE, button(GuiMaterials.barrier(), ChatColor.RED + "Close", Collections.<String>emptyList()));
        fill(gui, GuiMaterials.paneGray());
        playerSlots.put(player.getUniqueId(), slots);
        player.openInventory(gui);
    }

    private ItemStack playerHead(Player target) {
        ItemStack head = new ItemStack(GuiMaterials.playerHead(), 1);
        if ("SKULL_ITEM".equals(head.getType().name())) {
            head.setDurability((short) 3);
        }
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skull = (SkullMeta) meta;
            try {
                skull.setOwner(target.getName());
            } catch (Throwable ignored) {
                // Owner textures are optional; the name is what matters.
            }
            skull.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + target.getName());
            skull.setLore(Arrays.asList(
                    ChatColor.GRAY + "Join: " + ChatColor.WHITE + njm().getMessage(target, "join", "default-join-message"),
                    ChatColor.GRAY + "Leave: " + ChatColor.WHITE + njm().getMessage(target, "leave", "default-leave-message"),
                    "",
                    ChatColor.GREEN + "Left-Click: Set Join",
                    ChatColor.RED + "Right-Click: Set Leave",
                    ChatColor.DARK_RED + "Shift-Click: Clear All"
            ));
            head.setItemMeta(skull);
        }
        return head;
    }

    private String current(Player player, String type) {
        return njm().getMessage(player, type, "default-" + type + "-message");
    }

    private ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory gui, ItemStack filler) {
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + " ");
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler.clone());
            }
        }
    }

    private static String stripTitle(String title) {
        return title == null ? "" : ChatColor.stripColor(title);
    }

    private static boolean isOurGui(String title) {
        return TITLE_USER.equals(title) || TITLE_ADMIN.equals(title) || TITLE_PLAYERS.equals(title);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = stripTitle(event.getView().getTitle());
        if (!isOurGui(title)) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType().name().contains("GLASS_PANE")) {
            return;
        }

        try {
            if (TITLE_PLAYERS.equals(title)) {
                handlePlayers(player, slot, event.isRightClick(), event.isShiftClick());
            } else if (TITLE_ADMIN.equals(title)) {
                handleAdmin(player, slot);
            } else {
                handleUser(player, slot);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("GUI click failed: " + t.getMessage());
            player.sendMessage(ChatColor.RED + "GUI action failed. Check console.");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        String title = stripTitle(event.getView().getTitle());
        if (TITLE_PLAYERS.equals(title)) {
            playerSlots.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        // Never leave a half-finished prompt behind for an admin who disconnects.
        UUID id = event.getPlayer().getUniqueId();
        pendingType.remove(id);
        pendingTarget.remove(id);
        playerSlots.remove(id);
    }

    private void handleUser(Player player, int slot) {
        switch (slot) {
            case U_JOIN:
                if (!require(player, "joinleave.set.join")) {
                    return;
                }
                player.closeInventory();
                startSelfPrompt(player, "join");
                break;
            case U_LEAVE:
                if (!require(player, "joinleave.set.leave")) {
                    return;
                }
                player.closeInventory();
                startSelfPrompt(player, "leave");
                break;
            case U_JOIN_SOUND:
                if (!require(player, "joinleave.sound")) {
                    return;
                }
                player.closeInventory();
                startSelfPrompt(player, "join_sound");
                break;
            case U_LEAVE_SOUND:
                if (!require(player, "joinleave.sound")) {
                    return;
                }
                player.closeInventory();
                startSelfPrompt(player, "leave_sound");
                break;
            case U_ICON:
                if (!require(player, "joinleave.icon")) {
                    return;
                }
                player.closeInventory();
                startSelfPrompt(player, "icon");
                break;
            case U_PREVIEW:
                player.closeInventory();
                player.performCommand("njm preview");
                break;
            case U_TOGGLE:
                if (!require(player, "joinleave.toggle")) {
                    return;
                }
                player.performCommand("njm toggle");
                openUserGUI(player);
                break;
            case U_CLEAR:
                if (!njm().canCustomize(player, "join") || !njm().canCustomize(player, "leave")) {
                    deny(player);
                    break;
                }
                njm().clearMessage(player, "all");
                player.sendMessage(ChatColor.GREEN + "Your join/leave messages were cleared.");
                player.closeInventory();
                break;
            case U_HELP:
                player.closeInventory();
                player.performCommand("njm help");
                break;
            default:
                break;
        }
    }

    private void handleAdmin(Player player, int slot) {
        switch (slot) {
            case A_PERSONAL:
                openUserGUI(player);
                break;
            case A_JOIN:
                if (!require(player, "joinleave.set.join")) {
                    return;
                }
                player.closeInventory();
                startSelfPrompt(player, "join");
                break;
            case A_LEAVE:
                if (!require(player, "joinleave.set.leave")) {
                    return;
                }
                player.closeInventory();
                startSelfPrompt(player, "leave");
                break;
            case A_MANAGEMENT:
                if (!require(player, "joinleave.setplayer")) {
                    return;
                }
                openPlayerManagement(player);
                break;
            case A_SET_PLAYER_JOIN:
                if (!require(player, "joinleave.setplayer")) {
                    return;
                }
                player.closeInventory();
                askTargetThenMessage(player, "join");
                break;
            case A_SET_PLAYER_LEAVE:
                if (!require(player, "joinleave.setplayer")) {
                    return;
                }
                player.closeInventory();
                askTargetThenMessage(player, "leave");
                break;
            case A_INFO:
                if (!require(player, "joinleave.info")) {
                    return;
                }
                player.closeInventory();
                askTargetInfo(player);
                break;
            case A_CLEAR_PLAYER:
                if (!require(player, "joinleave.clearplayer")) {
                    return;
                }
                player.closeInventory();
                askTargetClear(player);
                break;
            case A_RELOAD:
                if (!require(player, "joinleave.reload")) {
                    return;
                }
                player.closeInventory();
                njm().reloadPlugin(player);
                break;
            case A_CLOSE:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    private void handlePlayers(Player player, int slot, boolean right, boolean shift) {
        if (slot == P_BACK) {
            openAdminGUI(player);
            return;
        }
        if (slot == P_CLOSE) {
            player.closeInventory();
            return;
        }
        Map<Integer, UUID> slots = playerSlots.get(player.getUniqueId());
        UUID targetId = slots == null ? null : slots.get(slot);
        if (targetId == null) {
            return;
        }
        String targetName = njm().displayNameFor(targetId, "unknown");

        if (shift) {
            if (!Perms.has(player, "joinleave.clearplayer")) {
                deny(player);
                return;
            }
            njm().clearMessage(targetId, "all");
            player.sendMessage(ChatColor.GREEN + "Cleared messages for " + targetName);
            openPlayerManagement(player);
            return;
        }
        if (!Perms.has(player, "joinleave.setplayer")) {
            deny(player);
            return;
        }
        player.closeInventory();
        startTargetPrompt(player, targetName, targetId, right ? "leave" : "join");
    }

    /** Returns true when the player may proceed; otherwise denies and returns false. */
    private boolean require(Player player, String permission) {
        if (!Perms.has(player, permission)) {
            deny(player);
            return false;
        }
        return true;
    }

    private void deny(Player player) {
        player.sendMessage(ChatColor.RED + "You don't have permission for that.");
    }

    private void startSelfPrompt(Player player, String messageType) {
        pendingType.put(player.getUniqueId(), messageType);
        pendingTarget.remove(player.getUniqueId());
        String label = messageType.replace('_', ' ');
        player.sendMessage(ChatColor.YELLOW + "Type your " + label + " in chat, or 'cancel'.");
        beginChatCapture(player);
    }

    private void startTargetPrompt(Player admin, String targetName, UUID targetId, String messageType) {
        pendingType.put(admin.getUniqueId(), messageType);
        pendingTarget.put(admin.getUniqueId(), targetName);
        admin.sendMessage(ChatColor.YELLOW + "Type " + messageType + " message for " + targetName + ", or 'cancel'.");
        beginChatCapture(admin, targetId);
    }

    private void beginChatCapture(Player player) {
        beginChatCapture(player, null);
    }

    private void beginChatCapture(final Player player, final UUID targetId) {
        final String type = pendingType.get(player.getUniqueId());
        final String targetName = pendingTarget.get(player.getUniqueId());
        if (type == null) {
            return;
        }
        ConversationFactory factory = new ConversationFactory(plugin)
                .withModality(true)
                .withLocalEcho(false)
                .withPrefix(context -> ChatColor.DARK_PURPLE + "[NJM] ")
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return ChatColor.YELLOW + "Enter the " + type + " message, or 'cancel':";
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        UUID id = player.getUniqueId();
                        pendingType.remove(id);
                        pendingTarget.remove(id);
                        if (input == null || input.equalsIgnoreCase("cancel")) {
                            context.getForWhom().sendRawMessage(ChatColor.RED + "Cancelled.");
                            return Prompt.END_OF_CONVERSATION;
                        }
                        handleCapturedInput(player, type, targetName, targetId, input);
                        return Prompt.END_OF_CONVERSATION;
                    }
                });
        Conversation conversation = factory.buildConversation(player);
        conversation.begin();
    }

    private void handleCapturedInput(Player player, String type, String targetName, UUID targetId, String input) {
        JoinleaveMessage plugin = njm();
        if ("icon".equals(type)) {
            if (input.length() > 16) {
                player.sendMessage(ChatColor.RED + "Icons can be at most 16 characters.");
            } else {
                plugin.setIcon(player, input);
                player.sendMessage(ChatColor.GREEN + "Your icon was updated.");
            }
            return;
        }
        if (type.endsWith("_sound")) {
            String soundType = type.substring(0, type.indexOf('_'));
            if (plugin.setSound(player, soundType, input)) {
                player.sendMessage(ChatColor.GREEN + "Your " + soundType + " sound was updated.");
            } else {
                player.sendMessage(ChatColor.RED + "That sound does not exist on this server version.");
            }
            return;
        }
        if (plugin.isTooLong(input)) {
            player.sendMessage(ChatColor.RED + "Messages can be at most "
                    + plugin.maxMessageLength() + " characters.");
            return;
        }
        if (!plugin.enforceCooldown(player)) {
            return;
        }
        if (targetId != null) {
            plugin.setMessage(targetId, type, input);
            plugin.noteChange(player);
            player.sendMessage(ChatColor.GREEN + "Set " + type + " for " + targetName);
            return;
        }
        plugin.setMessage(player, type, input);
        plugin.noteChange(player);
        player.sendMessage(ChatColor.GREEN + "Your " + type + " message has been set.");
    }

    private void askTargetThenMessage(final Player admin, final String messageType) {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withModality(true)
                .withLocalEcho(false)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return ChatColor.YELLOW + "Type the player name (or cancel):";
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        if (input == null || input.equalsIgnoreCase("cancel")) {
                            context.getForWhom().sendRawMessage(ChatColor.RED + "Cancelled.");
                            return Prompt.END_OF_CONVERSATION;
                        }
                        String targetName = input.trim();
                        UUID targetId = njm().resolvePlayerUuid(targetName);
                        if (targetId == null) {
                            context.getForWhom().sendRawMessage(ChatColor.RED + "Unknown player: " + targetName);
                            return Prompt.END_OF_CONVERSATION;
                        }
                        startTargetPrompt(admin, njm().displayNameFor(targetId, targetName), targetId, messageType);
                        return Prompt.END_OF_CONVERSATION;
                    }
                });
        factory.buildConversation(admin).begin();
    }

    private void askTargetInfo(final Player admin) {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withModality(true)
                .withLocalEcho(false)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return ChatColor.YELLOW + "Type player name for info (or cancel):";
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        if (input == null || input.equalsIgnoreCase("cancel")) {
                            return Prompt.END_OF_CONVERSATION;
                        }
                        admin.performCommand("njm info " + input.trim());
                        return Prompt.END_OF_CONVERSATION;
                    }
                });
        factory.buildConversation(admin).begin();
    }

    private void askTargetClear(final Player admin) {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withModality(true)
                .withLocalEcho(false)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return ChatColor.YELLOW + "Type player name to clear (or cancel):";
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        if (input == null || input.equalsIgnoreCase("cancel")) {
                            return Prompt.END_OF_CONVERSATION;
                        }
                        String targetName = input.trim();
                        UUID targetId = njm().resolvePlayerUuid(targetName);
                        if (targetId == null) {
                            context.getForWhom().sendRawMessage(ChatColor.RED + "Unknown player: " + targetName);
                        } else {
                            njm().clearMessage(targetId, "all");
                            context.getForWhom().sendRawMessage(ChatColor.GREEN
                                    + "Cleared " + njm().displayNameFor(targetId, targetName));
                        }
                        return Prompt.END_OF_CONVERSATION;
                    }
                });
        factory.buildConversation(admin).begin();
    }
}
