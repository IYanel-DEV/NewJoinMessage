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

public class JoinLeaveGUI implements Listener {

    public static final String TITLE_USER = "NJM Settings";
    public static final String TITLE_ADMIN = "NJM Admin Panel";
    public static final String TITLE_PLAYERS = "NJM Player Management";

    private final JavaPlugin plugin;
    private final Map<UUID, String> pendingType = new HashMap<UUID, String>();
    private final Map<UUID, String> pendingTarget = new HashMap<UUID, String>();

    public JoinLeaveGUI(JavaPlugin plugin) {
        this.plugin = plugin;
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
        gui.setItem(11, button(GuiMaterials.greenBlock(), ChatColor.GREEN + "" + ChatColor.BOLD + "Set Join Message",
                Arrays.asList(ChatColor.GRAY + "Click to set your join message", ChatColor.YELLOW + "Current: " + current(player, "join"))));
        gui.setItem(15, button(GuiMaterials.redBlock(), ChatColor.RED + "" + ChatColor.BOLD + "Set Leave Message",
                Arrays.asList(ChatColor.GRAY + "Click to set your leave message", ChatColor.YELLOW + "Current: " + current(player, "leave"))));
        gui.setItem(22, button(GuiMaterials.barrier(), ChatColor.DARK_RED + "" + ChatColor.BOLD + "Clear My Messages",
                Collections.singletonList(ChatColor.GRAY + "Clear your join and leave messages")));
        gui.setItem(4, button(GuiMaterials.book(), ChatColor.YELLOW + "" + ChatColor.BOLD + "Help",
                Collections.singletonList(ChatColor.GRAY + "Click for /njm help")));
        gui.setItem(10, button(Material.NOTE_BLOCK, ChatColor.GREEN + "Join Sound",
                Collections.singletonList(ChatColor.GRAY + "Set a sound name or off")));
        gui.setItem(12, button(Material.NAME_TAG, ChatColor.GOLD + "Icon",
                Collections.singletonList(ChatColor.GRAY + "Set an icon or off")));
        gui.setItem(13, button(Material.COMPASS, ChatColor.AQUA + "Preview Messages",
                Collections.singletonList(ChatColor.GRAY + "See both messages now")));
        gui.setItem(14, button(Material.LEVER, ChatColor.YELLOW + "Toggle Broadcasts",
                Collections.singletonList(ChatColor.GRAY + "Current: " + (
                        ((JoinleaveMessage) plugin).isBroadcastEnabled(player) ? ChatColor.GREEN + "On" : ChatColor.RED + "Off"))));
        gui.setItem(16, button(Material.NOTE_BLOCK, ChatColor.RED + "Leave Sound",
                Collections.singletonList(ChatColor.GRAY + "Set a sound name or off")));
        fill(gui, GuiMaterials.panePurple());
        player.openInventory(gui);
    }

    public void openAdminGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, TITLE_ADMIN);

        gui.setItem(4, button(GuiMaterials.book(), ChatColor.GOLD + "" + ChatColor.BOLD + "Personal Settings",
                Collections.singletonList(ChatColor.GRAY + "Your own join/leave messages")));
        gui.setItem(10, button(GuiMaterials.greenBlock(), ChatColor.GREEN + "" + ChatColor.BOLD + "Set Your Join Message",
                Collections.singletonList(ChatColor.GRAY + "Set your personal join message")));
        gui.setItem(12, button(GuiMaterials.redBlock(), ChatColor.RED + "" + ChatColor.BOLD + "Set Your Leave Message",
                Collections.singletonList(ChatColor.GRAY + "Set your personal leave message")));

        gui.setItem(22, button(GuiMaterials.commandBlock(), ChatColor.DARK_RED + "" + ChatColor.BOLD + "Player Management",
                Collections.singletonList(ChatColor.GRAY + "Manage online players")));

        gui.setItem(28, button(GuiMaterials.greenBlock(), ChatColor.GREEN + "Set Player Join",
                Arrays.asList(ChatColor.GRAY + "Type player name, then message", ChatColor.DARK_GRAY + "Needs joinleave.setplayer")));
        gui.setItem(30, button(GuiMaterials.redBlock(), ChatColor.RED + "Set Player Leave",
                Arrays.asList(ChatColor.GRAY + "Type player name, then message", ChatColor.DARK_GRAY + "Needs joinleave.setplayer")));
        gui.setItem(32, button(GuiMaterials.book(), ChatColor.YELLOW + "Player Info",
                Collections.singletonList(ChatColor.GRAY + "Type a player name to view messages")));
        gui.setItem(34, button(GuiMaterials.tnt(), ChatColor.DARK_RED + "Clear Player Messages",
                Collections.singletonList(ChatColor.GRAY + "Type a player name to clear")));

        gui.setItem(40, button(Material.COMPASS, ChatColor.GOLD + "" + ChatColor.BOLD + "Reload Plugin",
                Collections.singletonList(ChatColor.GRAY + "Reload NewJoinMessage config")));
        gui.setItem(44, button(GuiMaterials.barrier(), ChatColor.RED + "Close", Collections.emptyList()));

        fill(gui, GuiMaterials.panePurple());
        player.openInventory(gui);
    }

    public void openPlayerManagement(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, TITLE_PLAYERS);
        List<Player> online = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        int slot = 0;
        for (Player target : online) {
            if (slot >= 45) {
                break;
            }
            gui.setItem(slot++, playerHead(target));
        }
        gui.setItem(45, button(GuiMaterials.arrow(), ChatColor.GRAY + "Back", Collections.singletonList(ChatColor.DARK_GRAY + "Return to admin panel")));
        gui.setItem(53, button(GuiMaterials.barrier(), ChatColor.RED + "Close", Collections.emptyList()));
        fill(gui, GuiMaterials.paneGray());
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
                // ignore
            }
            skull.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + target.getName());
            JoinleaveMessage njm = (JoinleaveMessage) plugin;
            String join = njm.getMessage(target, "join", "default-join-message");
            String leave = njm.getMessage(target, "leave", "default-leave-message");
            skull.setLore(Arrays.asList(
                    ChatColor.GRAY + "Join: " + ChatColor.WHITE + join,
                    ChatColor.GRAY + "Leave: " + ChatColor.WHITE + leave,
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
        JoinleaveMessage njm = (JoinleaveMessage) plugin;
        return njm.getMessage(player, type, "default-" + type + "-message");
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
        String t = stripTitle(title);
        return TITLE_USER.equals(t) || TITLE_ADMIN.equals(t) || TITLE_PLAYERS.equals(t)
                || t.contains("NJM Admin") || t.contains("NJM Settings") || t.contains("NJM Player");
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
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getItemMeta() == null) {
            return;
        }
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (name == null) {
            name = "";
        }
        String typeName = clicked.getType().name();
        if (typeName.contains("GLASS_PANE")) {
            return;
        }

        try {
            if (TITLE_PLAYERS.equals(title) || title.contains("Player Management")) {
                handlePlayers(player, clicked, name, event.isRightClick(), event.isShiftClick());
            } else if (TITLE_ADMIN.equals(title) || title.contains("Admin Panel")) {
                handleAdmin(player, name);
            } else {
                handleUser(player, name);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("GUI click failed: " + t.getMessage());
            player.sendMessage(ChatColor.RED + "GUI action failed. Check console.");
        }
    }

    private void handleUser(Player player, String name) {
        String n = name.toLowerCase();
        if (n.contains("set join")) {
            if (!Perms.has(player, "joinleave.set.join")) {
                deny(player);
                return;
            }
            player.closeInventory();
            startSelfPrompt(player, "join");
        } else if (n.contains("set leave")) {
            if (!Perms.has(player, "joinleave.set.leave")) {
                deny(player);
                return;
            }
            player.closeInventory();
            startSelfPrompt(player, "leave");
        } else if (n.contains("join sound")) {
            if (!Perms.has(player, "joinleave.sound")) { deny(player); return; }
            player.closeInventory();
            startSelfPrompt(player, "join_sound");
        } else if (n.contains("leave sound")) {
            if (!Perms.has(player, "joinleave.sound")) { deny(player); return; }
            player.closeInventory();
            startSelfPrompt(player, "leave_sound");
        } else if (n.equals("icon") || n.endsWith(" icon")) {
            if (!Perms.has(player, "joinleave.icon")) { deny(player); return; }
            player.closeInventory();
            startSelfPrompt(player, "icon");
        } else if (n.contains("preview")) {
            player.closeInventory();
            player.performCommand("njm preview");
        } else if (n.contains("toggle broadcast")) {
            player.performCommand("njm toggle");
            openUserGUI(player);
        } else if (n.contains("clear")) {
            JoinleaveMessage njm = (JoinleaveMessage) plugin;
            if (!njm.canCustomize(player, "join") || !njm.canCustomize(player, "leave")) { deny(player); return; }
            njm.clearMessage(player, "all");
            player.sendMessage(ChatColor.GREEN + "Your join/leave messages were cleared.");
            player.closeInventory();
        } else if (n.contains("help")) {
            player.closeInventory();
            player.performCommand("njm help");
        }
    }

    private void handleAdmin(Player player, String name) {
        String n = name.toLowerCase();
        if (n.contains("player management")) {
            if (!Perms.has(player, "joinleave.setplayer")) {
                deny(player);
                return;
            }
            openPlayerManagement(player);
        } else if (n.contains("set your join") || (n.contains("set join") && n.contains("your"))) {
            player.closeInventory();
            startSelfPrompt(player, "join");
        } else if (n.contains("set your leave") || (n.contains("set leave") && n.contains("your"))) {
            player.closeInventory();
            startSelfPrompt(player, "leave");
        } else if (n.contains("set player join")) {
            if (!Perms.has(player, "joinleave.setplayer")) { deny(player); return; }
            player.closeInventory();
            askTargetThenMessage(player, "join");
        } else if (n.contains("set player leave")) {
            if (!Perms.has(player, "joinleave.setplayer")) { deny(player); return; }
            player.closeInventory();
            askTargetThenMessage(player, "leave");
        } else if (n.contains("player info")) {
            if (!Perms.has(player, "joinleave.info")) { deny(player); return; }
            player.closeInventory();
            askTargetInfo(player);
        } else if (n.contains("clear player")) {
            if (!Perms.has(player, "joinleave.clearplayer")) { deny(player); return; }
            player.closeInventory();
            askTargetClear(player);
        } else if (n.contains("reload")) {
            if (!Perms.has(player, "joinleave.reload")) {
                deny(player);
                return;
            }
            player.closeInventory();
            ((JoinleaveMessage) plugin).reloadPlugin(player);
        } else if (n.contains("close")) {
            player.closeInventory();
        } else if (n.contains("personal")) {
            openUserGUI(player);
        }
    }

    private void handlePlayers(Player player, ItemStack clicked, String name, boolean right, boolean shift) {
        String n = name.toLowerCase();
        if (n.equals("back") || n.contains("back")) {
            openAdminGUI(player);
            return;
        }
        if (n.contains("close")) {
            player.closeInventory();
            return;
        }
        Player target = Bukkit.getPlayerExact(ChatColor.stripColor(clicked.getItemMeta().getDisplayName()));
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not online.");
            return;
        }
        if (shift) {
            if (!Perms.has(player, "joinleave.clearplayer")) {
                deny(player);
                return;
            }
            ((JoinleaveMessage) plugin).clearMessage(target, "all");
            player.sendMessage(ChatColor.GREEN + "Cleared messages for " + target.getName());
            openPlayerManagement(player);
            return;
        }
        if (!Perms.has(player, "joinleave.setplayer")) {
            deny(player);
            return;
        }
        player.closeInventory();
        startTargetPrompt(player, target.getName(), right ? "leave" : "join");
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

    private void startTargetPrompt(Player admin, String targetName, String messageType) {
        pendingType.put(admin.getUniqueId(), messageType);
        pendingTarget.put(admin.getUniqueId(), targetName);
        admin.sendMessage(ChatColor.YELLOW + "Type " + messageType + " message for " + targetName + ", or 'cancel'.");
        beginChatCapture(admin);
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
                        startTargetPrompt(admin, input.trim(), messageType);
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
                        Player target = Bukkit.getPlayerExact(input.trim());
                        if (target == null) {
                            context.getForWhom().sendRawMessage(ChatColor.RED + "Player not online.");
                        } else {
                            ((JoinleaveMessage) plugin).clearMessage(target, "all");
                            context.getForWhom().sendRawMessage(ChatColor.GREEN + "Cleared " + target.getName());
                        }
                        return Prompt.END_OF_CONVERSATION;
                    }
                });
        factory.buildConversation(admin).begin();
    }

    private void beginChatCapture(Player player) {
        // Use conversation for the message body (works 1.8+)
        final String type = pendingType.get(player.getUniqueId());
        final String targetName = pendingTarget.get(player.getUniqueId());
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
                        JoinleaveMessage njm = (JoinleaveMessage) plugin;
                        if ("icon".equals(type)) {
                            if (input.length() > 16) {
                                context.getForWhom().sendRawMessage(ChatColor.RED + "Icons can be at most 16 characters.");
                            } else {
                                njm.setIcon(player, input);
                                context.getForWhom().sendRawMessage(ChatColor.GREEN + "Your icon was updated.");
                            }
                        } else if (type.endsWith("_sound")) {
                            String soundType = type.substring(0, type.indexOf('_'));
                            if (njm.setSound(player, soundType, input)) {
                                context.getForWhom().sendRawMessage(ChatColor.GREEN + "Your " + soundType + " sound was updated.");
                            } else {
                                context.getForWhom().sendRawMessage(ChatColor.RED + "That sound does not exist on this server version.");
                            }
                        } else if (targetName != null) {
                            Player target = Bukkit.getPlayerExact(targetName);
                            if (target == null) {
                                context.getForWhom().sendRawMessage(ChatColor.RED + "Player not online: " + targetName);
                            } else {
                                njm.setMessage(target, type, input);
                                context.getForWhom().sendRawMessage(ChatColor.GREEN + "Set " + type + " for " + target.getName());
                            }
                        } else {
                            njm.setMessage(player, type, input);
                            context.getForWhom().sendRawMessage(ChatColor.GREEN + "Your " + type + " message has been set.");
                        }
                        return Prompt.END_OF_CONVERSATION;
                    }
                });
        Conversation conversation = factory.buildConversation(player);
        conversation.begin();
    }
}
