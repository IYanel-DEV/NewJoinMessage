package com.joinleave;

import com.joinleave.storage.PlayerStore;
import com.joinleave.storage.StorageFactory;
import com.joinleave.storage.YamlBackend;
import com.joinleave.util.ColorUtils;
import com.joinleave.util.ModernDataImporter;
import com.joinleave.util.Perms;
import com.joinleave.util.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class JoinleaveMessage extends JavaPlugin implements Listener {

    private static final long FLUSH_INTERVAL_TICKS = 100L;
    private static final SimpleDateFormat CHANGE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static JoinleaveMessage instance;

    // Written by the main thread (reload) and read by the async flush task.
    private volatile PlayerStore store;
    private final Map<UUID, Long> lastChangeAt = new ConcurrentHashMap<UUID, Long>();
    private volatile VaultHook vaultHook;

    private LanguageConfigs languageConfigs;
    private LanguageManager languageManager;
    private LanguageHandler languageHandler;
    private JoinLeaveGUI gui;

    @Override
    public void onEnable() {
        instance = this;
        try {
            enablePlugin();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "NewJoinMessage failed to enable; only this plugin will be disabled.", t);
            try {
                getServer().getPluginManager().disablePlugin(this);
            } catch (Throwable disableFailure) {
                getLogger().log(Level.SEVERE, "NewJoinMessage could not disable itself cleanly.", disableFailure);
            }
        }
    }

    private void enablePlugin() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        languageConfigs = new LanguageConfigs(this);
        languageConfigs.loadConfigs();

        // One shared manager: every reader and writer goes through this instance so the
        // cached DataLang view can never drift from what is on disk.
        languageManager = new LanguageManager(this);
        languageHandler = new LanguageHandler(this, languageManager);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(languageManager), this);

        Bukkit.getScheduler().runTask(this, () -> announceUpdateStatus());

        try {
            new Metrics(this, 33311);
        } catch (Throwable t) {
            getLogger().warning("Metrics (bStats) failed to initialize: " + t.getMessage());
        }

        PlayerWelcome playerWelcome = new PlayerWelcome(this);
        Bukkit.getPluginManager().registerEvents(playerWelcome, this);

        VanishAPI.register(this);

        vaultHook = new VaultHook(getLogger());
        vaultHook.setup();

        this.gui = new JoinLeaveGUI(this);
        getServer().getPluginManager().registerEvents(this.gui, this);

        JoinleaveCommand joinLeaveCommand = new JoinleaveCommand(this);
        getCommand("njm").setExecutor(joinLeaveCommand);
        getCommand("njm").setTabCompleter(joinLeaveCommand);

        buildPlayerStore();
        startFlushTask();
    }

    private void announceUpdateStatus() {
        org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
        StringBuilder banner = new StringBuilder();
        banner.append(ChatColor.LIGHT_PURPLE + "                           \n");
        banner.append(ChatColor.LIGHT_PURPLE + "  _   _                   _       _       __  __                                      \n");
        banner.append(ChatColor.LIGHT_PURPLE + " | \\ | |                 | |     (_)     |  \\/  |                                     \n");
        banner.append(ChatColor.LIGHT_PURPLE + " |  \\| | _____      __   | | ___  _ _ __ | \\  / | ___  ___ ___  __ _  __ _  ___  ___ \n");
        banner.append(ChatColor.LIGHT_PURPLE + " | . ` |/ _ \\ \\ /\\ / /   | |/ _ \\| | '_ \\| |\\/| |/ _ \\/ __/ __|/ _` |/ _` |/ _ \\/ __|\n");
        banner.append(ChatColor.LIGHT_PURPLE + " | |\\  |  __/\\ V  V / |__| | (_) | | | | | |  | |  __/\\__ \\__ \\ (_| | (_| |  __/\\__ \\\n");
        banner.append(ChatColor.LIGHT_PURPLE + " |_| \\_|\\___| \\_/\\_/ \\____/ \\___/|_|_| |_|_|  |_|\\___||___/___/\\__,_|\\__, |\\___||___/\n");
        banner.append(ChatColor.LIGHT_PURPLE + "                                                                      __/ |          \n");
        banner.append(ChatColor.LIGHT_PURPLE + "                                                                     |___/           \n");
        banner.append("\n");

        // Gated by config, not by a permission: this runs once at startup and prints to
        // the console, and the console always holds every permission, so a permission
        // check here could never deny anything.
        if (!getConfig().getBoolean("update-check", true)) {
            console.sendMessage(banner.toString());
            return;
        }

        try {
            UpdateChecker.init(this, 110979).requestUpdateCheck().whenComplete((result, e) -> {
                if (e != null || result == null) {
                    getLogger().warning("UpdateChecker failed: " + (e != null ? e.getMessage() : "null result"));
                    console.sendMessage(banner.toString());
                    return;
                }
                if (result.requiresUpdate()) {
                    String newest = result.getNewestVersion();
                    banner.append("                       [" + getDescription().getName() + "] " + ChatColor.RED
                            + "An update is available! New version: " + newest);
                    notifyAdmins(ChatColor.RED + "An update is available for " + getDescription().getName()
                            + "! New version: " + newest);
                } else {
                    banner.append("                        " + getDescription().getName() + " " + ChatColor.GREEN
                            + "Plugin is up to date!");
                }
                console.sendMessage(banner.toString());
            });
        } catch (Throwable t) {
            getLogger().warning("UpdateChecker failed: " + t.getMessage());
            console.sendMessage(banner.toString());
        }
    }

    /** Tells online admins who hold joinleave.update about an available update. */
    private void notifyAdmins(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Perms.has(player, "joinleave.update")) {
                player.sendMessage(message);
            }
        }
    }

    private void buildPlayerStore() {
        ModernDataImporter.importIfNeeded(this, StorageFactory.dataFile(this));
        store = StorageFactory.create(this);
    }

    private void startFlushTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> {
                    try {
                        store.flush();
                    } catch (Throwable t) {
                        getLogger().log(Level.WARNING, "Background data flush failed.", t);
                    }
                },
                FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS);
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        if (store != null) {
            store.close();
        }
    }

    public static JoinleaveMessage getInstance() {
        return instance;
    }

    public JoinLeaveGUI getGui() {
        return gui;
    }

    public PlayerStore getStore() {
        return store;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public LanguageHandler getLanguageHandler() {
        return languageHandler;
    }

    public String getLastChange(Player player, String messageType) {
        return getLastChange(player.getUniqueId(), messageType);
    }

    public String getLastChange(UUID uuid, String messageType) {
        String key = "join".equals(messageType) ? PlayerStore.KEY_LAST_CHANGE_JOIN : PlayerStore.KEY_LAST_CHANGE_LEAVE;
        long timestamp = store.getLong(uuid, key, -1L);
        if (timestamp < 0L) {
            return "N/A";
        }
        return CHANGE_TIME_FORMAT.format(new Date(timestamp));
    }

    public void reloadPlugin(CommandSender sender) {
        List<String> configFiles = Arrays.asList("config.yml", "firework.yml", "players.yml", "data.yml");

        for (String configFile : configFiles) {
            java.io.File file = new java.io.File(getDataFolder(), configFile);
            if (!file.exists()) {
                saveResource(configFile, false);
                String message = configFile + " not found, created default configuration.";
                if (sender instanceof Player) {
                    ((Player) sender).sendMessage(ChatColor.LIGHT_PURPLE + "Checking " + configFile + "...");
                    ((Player) sender).sendMessage(ChatColor.DARK_PURPLE + message + ChatColor.GREEN + " ✔");
                } else {
                    getLogger().info("Checking " + configFile + "...");
                    getLogger().info(message);
                }
            }
        }

        reloadConfig();
        languageHandler.reloadLanguages();
        languageManager.reload();

        // Rebuild the store so a backend change in config actually takes effect.
        // The old store is flushed and closed *before* the new one is built, otherwise
        // the rebuild would read the backend before those pending writes landed and
        // in-memory state would silently disagree with what is on disk.
        PlayerStore previous = store;
        if (previous != null) {
            previous.close();
        }
        PlayerStore rebuilt = null;
        try {
            rebuilt = StorageFactory.create(this);
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Failed to rebuild the player data backend; "
                    + "falling back to data.yml.", t);
        }
        if (rebuilt == null) {
            rebuilt = new PlayerStore(new YamlBackend(StorageFactory.dataFile(this), getLogger()), getLogger());
            rebuilt.load();
        }
        store = rebuilt;
        getLogger().info("Reloaded player data backend: " + store.backendName());

        if (sender instanceof Player) {
            ((Player) sender).sendMessage(ChatColor.LIGHT_PURPLE + "Plugin reloaded" + ChatColor.GREEN + " ✔");
        } else {
            getLogger().info("Plugin reloaded successfully.");
        }
    }

    public void clearMessage(Player player, String messageType) {
        clearMessage(player.getUniqueId(), messageType);
    }

    public void clearMessage(UUID uuid, String messageType) {
        if ("all".equalsIgnoreCase(messageType)) {
            setMessage(uuid, "join", "");
            setMessage(uuid, "leave", "");
        } else {
            setMessage(uuid, messageType, "");
        }
    }

    public void resetPlayerMessages(Player player) {
        setMessage(player.getUniqueId(), "join", cfg("default-join-message", ""));
        setMessage(player.getUniqueId(), "leave", cfg("default-leave-message", ""));
    }

    private String cfg(String path, String def) {
        String v = getConfig().getString(path);
        return v == null ? def : v;
    }

    public boolean canCustomize(Player player, String type) {
        return canCustomize(player.getUniqueId(), player, type);
    }

    public boolean canCustomize(UUID uuid, Player online, String type) {
        if (getConfig().getBoolean("allow-all-players", true)) {
            return true;
        }
        return online != null && Perms.has(online, "joinleave.set." + type);
    }

    /**
     * Resolves a name to a UUID, covering players who are currently offline.
     * Online players always win; otherwise the stored name index is consulted.
     */
    public UUID resolvePlayerUuid(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        return store.resolveUuid(name);
    }

    public String displayNameFor(UUID uuid, String fallback) {
        String stored = store.resolveName(uuid);
        if (stored != null) {
            return stored;
        }
        Player online = uuid == null ? null : Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        return fallback;
    }

    public String parsePlaceholders(String message, Player player) {
        if (message == null) {
            return "";
        }
        // Order matters: %prefix% can itself contain a % character, so it is resolved
        // last to avoid a stored prefix re-triggering substitution.
        int online = Bukkit.getOnlinePlayers().size();
        String serverName = cfg("server-name", "");
        if (serverName.isEmpty()) {
            serverName = Bukkit.getServer().getName();
        }
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());

        return message.replace("PLAYERNAME", player.getName())
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName())
                .replace("%world%", player.getWorld().getName())
                .replace("%online%", String.valueOf(online))
                .replace("%player_count%", String.valueOf(online))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%server_name%", serverName)
                .replace("%motd%", Bukkit.getServer().getMotd())
                .replace("%server_version%", Bukkit.getServer().getBukkitVersion())
                .replace("%time%", time)
                .replace("%vault_prefix%", vaultPrefix(player))
                .replace("%prefix%", vaultPrefix(player));
    }

    private String vaultPrefix(Player player) {
        if (vaultHook == null) {
            return "";
        }
        vaultHook.setup();
        return vaultHook.prefix(player);
    }

    /** Longest custom message a player may set; 0 means unlimited. */
    public int maxMessageLength() {
        int configured = getConfig().getInt("max-message-length", 100);
        return configured < 0 ? 0 : configured;
    }

    public boolean isTooLong(String message) {
        int max = maxMessageLength();
        return max > 0 && message != null && message.length() > max;
    }

    public int cooldownSeconds() {
        int configured = getConfig().getInt("change-cooldown-seconds", 3);
        return configured < 0 ? 0 : configured;
    }

    /**
     * Blocks a rapid second change. The console is never limited and
     * joinleave.cooldown.bypass skips the wait.
     *
     * @return seconds the sender still has to wait, 0 when they may continue.
     */
    public int cooldownRemaining(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return 0;
        }
        Player player = (Player) sender;
        if (Perms.has(player, "joinleave.cooldown.bypass")) {
            return 0;
        }
        int seconds = cooldownSeconds();
        if (seconds <= 0) {
            return 0;
        }
        Long last = lastChangeAt.get(player.getUniqueId());
        if (last == null) {
            return 0;
        }
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        int remaining = seconds - (int) elapsed;
        return remaining > 0 ? remaining : 0;
    }

    /** Sends the wait notice when a cooldown is active. */
    public boolean enforceCooldown(CommandSender sender) {
        int remaining = cooldownRemaining(sender);
        if (remaining <= 0) {
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Please wait " + remaining + " more second"
                + (remaining == 1 ? "" : "s") + " before changing your message again.");
        return false;
    }

    /** Called after a change is accepted so the cooldown starts. */
    public void noteChange(CommandSender sender) {
        if (sender instanceof Player) {
            lastChangeAt.put(((Player) sender).getUniqueId(), System.currentTimeMillis());
        }
    }

    /** Every player who has stored custom data, for /njm list. */
    public List<UUID> playersWithMessages() {
        return store.playersWithCustomData();
    }

    public String renderMessage(Player player, String type) {
        boolean join = "join".equals(type);
        String editable = getMessage(player, type, "default-" + type + "-message");
        String icon = getIcon(player);
        String iconPrefix = icon.isEmpty() ? "" : ColorUtils.colorize(icon) + " ";
        if (getConfig().getBoolean("use-modern-format", false)) {
            return iconPrefix + ColorUtils.colorize(cfg("player-name-color", "&a")) + player.getName()
                    + " " + ColorUtils.colorize(cfg(type + "-text-color", "&f"))
                    + ColorUtils.colorize(parsePlaceholders(cfg(type + "-text", join ? "joined the server" : "left the server"), player))
                    + " - " + ColorUtils.colorize(parsePlaceholders(editable, player));
        }
        String prefix = ColorUtils.colorize(cfg(type + "-prefix", join ? "&d[&a+&d]" : "&7[&c-&7]"));
        String action = ColorUtils.colorize(parsePlaceholders(
                cfg("default-" + type + "-prefix", join ? "&7PLAYERNAME &5has joined" : "&4PLAYERNAME has left"), player));
        return prefix + " " + iconPrefix + action + ChatColor.GRAY + " - "
                + ColorUtils.colorize(parsePlaceholders(editable, player));
    }

    public boolean broadcastsDisabled(Player player) {
        if (VanishAPI.isVanished(player) || !isBroadcastEnabled(player)) {
            return true;
        }
        for (String world : getConfig().getStringList("disabled-worlds")) {
            if (player.getWorld().getName().equalsIgnoreCase(world)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Remember the name so admins can still find this player while they are offline.
        store.setName(player.getUniqueId(), player.getName());
        try {
            if (broadcastsDisabled(player)) {
                event.setJoinMessage(null);
                return;
            }
            event.setJoinMessage(renderMessage(player, "join"));
            playConfiguredSound(player, "join");
        } catch (Throwable t) {
            getLogger().warning("handleJoin failed for " + player.getName() + ": " + t.getMessage());
            event.setJoinMessage(player.getName() + " joined");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        store.setName(player.getUniqueId(), player.getName());
        try {
            if (broadcastsDisabled(player)) {
                event.setQuitMessage(null);
                return;
            }
            event.setQuitMessage(renderMessage(player, "leave"));
            playConfiguredSound(player, "leave");
        } catch (Throwable t) {
            getLogger().warning("handleLeave failed for " + player.getName() + ": " + t.getMessage());
            event.setQuitMessage(player.getName() + " left");
        }
    }

    public boolean isBroadcastEnabled(Player player) {
        return isBroadcastEnabled(player.getUniqueId());
    }

    public boolean isBroadcastEnabled(UUID uuid) {
        return store.getBoolean(uuid, PlayerStore.KEY_BROADCAST, true);
    }

    public boolean toggleBroadcast(Player player) {
        return toggleBroadcast(player.getUniqueId());
    }

    public boolean toggleBroadcast(UUID uuid) {
        boolean enabled = !isBroadcastEnabled(uuid);
        store.setBoolean(uuid, PlayerStore.KEY_BROADCAST, enabled);
        return enabled;
    }

    public String getIcon(Player player) {
        return getIcon(player.getUniqueId());
    }

    public String getIcon(UUID uuid) {
        String icon = store.getString(uuid, PlayerStore.KEY_ICON);
        if ("off".equalsIgnoreCase(icon)) {
            return "";
        }
        return icon == null ? cfg("default-icon", "") : icon;
    }

    public void setIcon(Player player, String icon) {
        setIcon(player.getUniqueId(), icon);
    }

    public void setIcon(UUID uuid, String icon) {
        store.setString(uuid, PlayerStore.KEY_ICON,
                icon == null || icon.isEmpty() || "off".equalsIgnoreCase(icon) ? "off" : icon);
    }

    public String getSound(Player player, String type) {
        return getSound(player.getUniqueId(), type);
    }

    public String getSound(UUID uuid, String type) {
        String key = "join".equals(type) ? PlayerStore.KEY_JOIN_SOUND : PlayerStore.KEY_LEAVE_SOUND;
        String sound = store.getString(uuid, key);
        return sound == null ? cfg("sounds." + type, "") : sound;
    }

    public boolean setSound(Player player, String type, String soundName) {
        return setSound(player.getUniqueId(), type, soundName);
    }

    public boolean setSound(UUID uuid, String type, String soundName) {
        String key = "join".equals(type) ? PlayerStore.KEY_JOIN_SOUND : PlayerStore.KEY_LEAVE_SOUND;
        if ("off".equalsIgnoreCase(soundName)) {
            store.setString(uuid, key, "off");
            return true;
        }
        Sound sound = resolveSound(soundName);
        if (sound == null) {
            return false;
        }
        store.setString(uuid, key, sound.name());
        return true;
    }

    private Sound resolveSound(String names) {
        if (names == null || names.trim().isEmpty() || "off".equalsIgnoreCase(names.trim())) {
            return null;
        }
        for (String name : names.split(",")) {
            try {
                return Sound.valueOf(name.trim().toUpperCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_'));
            } catch (IllegalArgumentException ignored) {
                // Try the next cross-version alias.
            }
        }
        return null;
    }

    private void playConfiguredSound(Player source, String type) {
        if (!getConfig().getBoolean("sounds.enabled", false)) {
            return;
        }
        Sound sound = resolveSound(getSound(source.getUniqueId(), type));
        if (sound == null) {
            return;
        }
        float volume = (float) getConfig().getDouble("sounds.volume", 1.0D);
        float pitch = (float) getConfig().getDouble("sounds.pitch", 1.0D);
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            recipient.playSound(recipient.getLocation(), sound, volume, pitch);
        }
    }

    public void setMessage(Player player, String column, String message) {
        setMessage(player.getUniqueId(), column, message);
    }

    public void setMessage(UUID uuid, String column, String message) {
        if (!("join".equals(column) || "leave".equals(column))) {
            getLogger().warning("Ignored invalid message type: " + column);
            return;
        }
        String value = message == null ? "" : message;
        int max = maxMessageLength();
        if (max > 0 && value.length() > max) {
            // Entry points validate first and show the player an error; this is the
            // backstop that guarantees storage never holds an oversized value.
            getLogger().warning("Truncated an over-long " + column + " message for " + uuid
                    + " to " + max + " characters.");
            value = value.substring(0, max);
        }
        store.setString(uuid, column + "_message", value);
        store.setLong(uuid, "join".equals(column)
                ? PlayerStore.KEY_LAST_CHANGE_JOIN : PlayerStore.KEY_LAST_CHANGE_LEAVE, System.currentTimeMillis());
    }

    public String getCustomMessage(Player player, String messageType) {
        return getCustomMessage(player.getUniqueId(), messageType);
    }

    public String getCustomMessage(UUID uuid, String messageType) {
        if (!("join".equals(messageType) || "leave".equals(messageType))) {
            return null;
        }
        return store.getString(uuid, messageType + "_message");
    }

    public String getMessage(Player player, String messageType, String defaultMessageType) {
        return getMessage(player.getUniqueId(), messageType, defaultMessageType);
    }

    public String getMessage(UUID uuid, String messageType, String defaultMessageType) {
        String custom = getCustomMessage(uuid, messageType);
        if (custom != null) {
            return custom;
        }
        List<String> pool = getConfig().getStringList("default-" + messageType + "-messages");
        if (!pool.isEmpty()) {
            return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        }
        return cfg(defaultMessageType, "");
    }
}
