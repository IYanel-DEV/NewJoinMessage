package com.joinleave;

import com.joinleave.util.ColorUtils;
import com.joinleave.util.ModernDataImporter;
import com.joinleave.util.Perms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.command.ConsoleCommandSender;

import java.util.*;

import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;



public class JoinleaveMessage extends JavaPlugin implements Listener {

    private FileConfiguration playersConfig;
    private File playersFile;

    private Connection connection;
    private boolean mysqlEnabled;

    private static JoinleaveMessage instance;

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

        languageHandler = new LanguageHandler(this);

        File dataLangFile = new File(getDataFolder(), "Lang/DataLang.yml");
        languageManager = new LanguageManager(dataLangFile);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(languageManager), this);

        Bukkit.getScheduler().runTask(this, () -> {
            ConsoleCommandSender console = Bukkit.getConsoleSender();

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(ChatColor.LIGHT_PURPLE + "                           \n");
            messageBuilder.append(ChatColor.LIGHT_PURPLE + "  _   _                   _       _       __  __                                      \n");
            messageBuilder.append(ChatColor.LIGHT_PURPLE + " | \\ | |                 | |     (_)     |  \\/  |                                     \n");
            messageBuilder.append(ChatColor.LIGHT_PURPLE + " |  \\| | _____      __   | | ___  _ _ __ | \\  / | ___  ___ ___  __ _  __ _  ___  ___ \n");
            messageBuilder.append(ChatColor.LIGHT_PURPLE + " | . ` |/ _ \\ \\ /\\ / /   | |/ _ \\| | '_ \\| |\\/| |/ _ \\/ __/ __|/ _` |/ _` |/ _ \\/ __|\n");
            messageBuilder.append(ChatColor.LIGHT_PURPLE + " | |\\  |  __/\\ V  V / |__| | (_) | | | | | |  | |  __/\\__ \\__ \\ (_| | (_| |  __/\\__ \\\n");
            messageBuilder.append(ChatColor.LIGHT_PURPLE + " |_| \\_|\\___| \\_/\\_/ \\____/ \\___/|_|_| |_|_|  |_|\\___||___/___/\\__,_|\\__, |\\___||___/\n");
            messageBuilder.append(ChatColor.LIGHT_PURPLE + "                                                                      __/ |          \n");
            messageBuilder.append(ChatColor.LIGHT_PURPLE + "                                                                     |___/           \n");
            messageBuilder.append("\n");

            try {
                UpdateChecker.init(this, 110979).requestUpdateCheck().whenComplete((result, e) -> {
                    if (e != null || result == null) {
                        getLogger().warning("UpdateChecker failed: " + (e != null ? e.getMessage() : "null result"));
                        console.sendMessage(messageBuilder.toString());
                        return;
                    }
                    if (result.requiresUpdate()) {
                        String pluginName = "                       [" + getDescription().getName() + "]";
                        String updateMessage = pluginName + " " + ChatColor.RED + "An update is available! New version: " + result.getNewestVersion();
                        messageBuilder.append(updateMessage);
                        console.sendMessage(messageBuilder.toString());
                    } else {
                        String pluginName = "                        " + getDescription().getName() + " ";
                        String upToDateMessage = pluginName + " " + ChatColor.GREEN + "Plugin is up to date!";
                        messageBuilder.append(upToDateMessage);
                        console.sendMessage(messageBuilder.toString());
                    }
                });
            } catch (Throwable t) {
                getLogger().warning("UpdateChecker failed: " + t.getMessage());
                console.sendMessage(messageBuilder.toString());
            }
        });

        try {
            int pluginId = 33311;
            Metrics metrics = new Metrics(this, pluginId);
        } catch (Throwable t) {
            getLogger().warning("Metrics (bStats) failed to initialize: " + t.getMessage());
        }

        PlayerWelcome playerWelcome = new PlayerWelcome(this);
        Bukkit.getPluginManager().registerEvents(playerWelcome, this);

        String defaultEncoding = System.getProperty("file.encoding");
        getLogger().info("Default system encoding: " + defaultEncoding);

        this.gui = new JoinLeaveGUI(this);
        getServer().getPluginManager().registerEvents(this.gui, this);

        JoinleaveCommand joinLeaveCommand = new JoinleaveCommand(this);
        getCommand("njm").setExecutor(joinLeaveCommand);
        getCommand("njm").setTabCompleter(joinLeaveCommand);

        playersFile = new File(getDataFolder(), "data.yml");
        if (!playersFile.exists()) {
            saveResource("data.yml", false);
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        ModernDataImporter.importIfNeeded(this, playersConfig, playersFile);

        mysqlEnabled = getConfig().getBoolean("mysql.enabled");
        if (mysqlEnabled && (!setupMySQL() || !createTableIfNotExists())) {
            mysqlEnabled = false;
            closeMySQLConnection();
            getLogger().severe("MySQL failed — falling back to data.yml for this session.");
        }
    }

    @Override
    public void onDisable() {
        if (playersConfig != null) {
            savePlayersConfig();
        }
        closeMySQLConnection();
    }

    public void onPlayerJoin(Player player) {
        String defaultLanguage = "English";
        languageManager.setPlayerLanguage(player, defaultLanguage);
    }

    public static JoinleaveMessage getInstance() {
        return instance;
    }

    public JoinLeaveGUI getGui() {
        return gui;
    }

    private FileConfiguration getPlayersConfig() {
        return playersConfig;
    }

    private void savePlayersConfig() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }

    public boolean hasCustomMessage(Player player) {
        return getCustomMessage(player, "join") != null || getCustomMessage(player, "leave") != null;
    }

    public String getLastChange(Player player, String messageType) {
        FileConfiguration playersConfig = getPlayersConfig();

        String lastChangePath = "players." + player.getUniqueId() + ".last_change." + messageType;
        if (playersConfig.contains(lastChangePath)) {
            long lastChangeTimestamp = playersConfig.getLong(lastChangePath);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.format(new Date(lastChangeTimestamp));
        }

        return "N/A";
    }

    private boolean setupMySQL() {
        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String username = getConfig().getString("mysql.username");
        String password = getConfig().getString("mysql.password");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&connectTimeout=5000&socketTimeout=5000", username, password);
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            connection = null;
            getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
            return false;
        }
    }

    private void closeMySQLConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                getLogger().severe("Failed to close MySQL connection: " + e.getMessage());
            }
            connection = null;
        }
    }

    private boolean createTableIfNotExists() {
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_messages (uuid VARCHAR(36) PRIMARY KEY, join_message TEXT, leave_message TEXT)")) {
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            getLogger().severe("Failed to create player_messages table: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("setplayer");
            subCommands.add("set");
            subCommands.add("gui");
            subCommands.add("clear");
            subCommands.add("reload");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setplayer")) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setplayer")) {
            List<String> messageTypes = new ArrayList<>();
            messageTypes.add("join");
            messageTypes.add("leave");
            StringUtil.copyPartialMatches(args[2], messageTypes, completions);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("clear"))) {
            List<String> messageTypes = new ArrayList<>();
            messageTypes.add("join");
            messageTypes.add("leave");
            StringUtil.copyPartialMatches(args[1], messageTypes, completions);
        }

        Collections.sort(completions);
        return completions;
    }

    public void reloadPlugin(CommandSender sender) {
        List<String> configFiles = Arrays.asList("config.yml", "firework.yml", "players.yml", "data.yml");

        for (String configFile : configFiles) {
            File file = new File(getDataFolder(), configFile);

            if (!file.exists()) {
                saveResource(configFile, false);

                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Checking " + configFile + "...");
                    player.sendMessage(ChatColor.DARK_PURPLE + configFile + " not found, created default configuration." + ChatColor.GREEN + " ✔");
                } else {
                    getLogger().info("Checking " + configFile + "...");
                    getLogger().info(configFile + " not found, created default configuration.");
                }
            }
        }

        reloadConfig();

        File fireworkFile = new File(getDataFolder(), "firework.yml");
        if (fireworkFile.exists()) {
            YamlConfiguration fireworkConfig = new YamlConfiguration();
            try {
                fireworkConfig.load(fireworkFile);
            } catch (IOException | InvalidConfigurationException e) {
                getLogger().severe("Failed to reload firework.yml: " + e.getMessage());
            }
        } else {
            getLogger().warning("firework.yml not found to reload.");
        }

        boolean newMySQLStatus = getConfig().getBoolean("mysql.enabled");

        if (newMySQLStatus != mysqlEnabled) {
            if (newMySQLStatus) {
                closeMySQLConnection();
                if (setupMySQL() && createTableIfNotExists()) {
                    mysqlEnabled = true;
                    getLogger().info("MySQL has been enabled and connected successfully.");
                } else {
                    mysqlEnabled = false;
                    closeMySQLConnection();
                    getLogger().severe("MySQL failed — falling back to data.yml for this session.");
                }
            } else {
                closeMySQLConnection();
                mysqlEnabled = false;
                getLogger().info("MySQL has been disabled.");
            }
        }

        if (mysqlEnabled && connection == null) {
            if (setupMySQL() && createTableIfNotExists()) {
                getLogger().info("MySQL has been enabled and connected successfully.");
            } else {
                mysqlEnabled = false;
                closeMySQLConnection();
                getLogger().severe("MySQL failed — falling back to data.yml for this session.");
            }
        }

        if (!mysqlEnabled && connection != null) {
            closeMySQLConnection();
            getLogger().info("System is now on local files");
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Plugin reloaded" + ChatColor.GREEN + " ✔");
        } else {
            getLogger().info("Plugin reloaded successfully.");
        }
    }

    public void clearMessage(Player player, String messageType) {
        if (messageType.equals("all")) {
            setMessage(player, "join", "");
            setMessage(player, "leave", "");
        } else {
            setMessage(player, messageType, "");
        }
    }

    public void resetPlayerMessages(Player player) {
        setMessage(player, "join", getConfig().getString("default-join-message"));
        setMessage(player, "leave", getConfig().getString("default-leave-message"));
    }

    private String cfg(String path, String def) {
        String v = getConfig().getString(path);
        return v == null ? def : v;
    }

    public boolean canCustomize(Player player, String type) {
        return getConfig().getBoolean("allow-all-players", true)
                || Perms.has(player, "joinleave.set." + type);
    }

    public String parsePlaceholders(String message, Player player) {
        if (message == null) {
            return "";
        }
        return message.replace("PLAYERNAME", player.getName())
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName())
                .replace("%world%", player.getWorld().getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
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
        try {
            Player player = event.getPlayer();
            if (broadcastsDisabled(player)) {
                event.setJoinMessage(null);
                return;
            }
            event.setJoinMessage(renderMessage(player, "join"));
            playConfiguredSound(player, "join");
        } catch (Throwable t) {
            getLogger().warning("handleJoin failed for " + event.getPlayer().getName() + ": " + t.getMessage());
            event.setJoinMessage(event.getPlayer().getName() + " joined");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleLeave(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            if (broadcastsDisabled(player)) {
                event.setQuitMessage(null);
                return;
            }
            event.setQuitMessage(renderMessage(player, "leave"));
            playConfiguredSound(player, "leave");
        } catch (Throwable t) {
            getLogger().warning("handleLeave failed for " + event.getPlayer().getName() + ": " + t.getMessage());
            event.setQuitMessage(event.getPlayer().getName() + " left");
        }
    }

    private String playerPath(Player player, String key) {
        return "players." + player.getUniqueId() + "." + key;
    }

    private String getPlayerSetting(Player player, String key) {
        return playersConfig.getString(playerPath(player, key));
    }

    private void setPlayerSetting(Player player, String key, Object value) {
        playersConfig.set(playerPath(player, key), value);
        savePlayersConfig();
    }

    public boolean isBroadcastEnabled(Player player) {
        return playersConfig.getBoolean(playerPath(player, "broadcast_enabled"), true);
    }

    public boolean toggleBroadcast(Player player) {
        boolean enabled = !isBroadcastEnabled(player);
        setPlayerSetting(player, "broadcast_enabled", enabled);
        return enabled;
    }

    public String getIcon(Player player) {
        String icon = getPlayerSetting(player, "icon");
        if ("off".equalsIgnoreCase(icon)) {
            return "";
        }
        return icon == null ? cfg("default-icon", "") : icon;
    }

    public void setIcon(Player player, String icon) {
        setPlayerSetting(player, "icon", icon == null || icon.isEmpty() || "off".equalsIgnoreCase(icon) ? "off" : icon);
    }

    public String getSound(Player player, String type) {
        String sound = getPlayerSetting(player, type + "_sound");
        return sound == null ? cfg("sounds." + type, "") : sound;
    }

    public boolean setSound(Player player, String type, String soundName) {
        if ("off".equalsIgnoreCase(soundName)) {
            setPlayerSetting(player, type + "_sound", "off");
            return true;
        }
        Sound sound = resolveSound(soundName);
        if (sound == null) {
            return false;
        }
        setPlayerSetting(player, type + "_sound", sound.name());
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
        Sound sound = resolveSound(getSound(source, type));
        if (sound == null) {
            return;
        }
        float volume = (float) getConfig().getDouble("sounds.volume", 1.0D);
        float pitch = (float) getConfig().getDouble("sounds.pitch", 1.0D);
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            recipient.playSound(recipient.getLocation(), sound, volume, pitch);
        }
    }

    private void reloadPlayersConfig() {
        playersFile = new File(getDataFolder(), "data.yml");
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    public void setMessage(Player player, String column, String message) {
        if (!("join".equals(column) || "leave".equals(column))) {
            getLogger().warning("Ignored invalid message type: " + column);
            return;
        }
        boolean useMysql = mysqlEnabled && connection != null;
        if (useMysql) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO player_messages (uuid, " + column + "_message) VALUES (?, ?) ON DUPLICATE KEY UPDATE " + column + "_message = ?")) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, message);
                statement.setString(3, message);
                statement.executeUpdate();
                return;
            } catch (SQLException e) {
                getLogger().severe("MySQL write failed; using data.yml for this session: " + e.getMessage());
                mysqlEnabled = false;
                closeMySQLConnection();
            }
        }
        playersConfig.set("players." + player.getUniqueId() + "." + column + "_message", message);
        updateLastChange(player, column);
        savePlayersConfig();
    }

    private void updateLastChange(Player player, String messageType) {
        FileConfiguration playersConfig = getPlayersConfig();
        playersConfig.set("players." + player.getUniqueId() + ".last_change." + messageType, System.currentTimeMillis());
    }

    public String getCustomMessage(Player player, String messageType) {
        if (!("join".equals(messageType) || "leave".equals(messageType))) {
            return null;
        }
        boolean useMysql = mysqlEnabled && connection != null;
        if (useMysql) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + messageType + "_message FROM player_messages WHERE uuid = ?")) {
                statement.setString(1, player.getUniqueId().toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String message = resultSet.getString(messageType + "_message");
                        return message == null || message.isEmpty() ? null : message;
                    }
                }
            } catch (SQLException e) {
                getLogger().severe("MySQL read failed; using data.yml for this session: " + e.getMessage());
                mysqlEnabled = false;
                closeMySQLConnection();
            }
        }
        String message = playersConfig.getString(playerPath(player, messageType + "_message"));
        return message == null || message.isEmpty() ? null : message;
    }

    public String getMessage(Player player, String messageType, String defaultMessageType) {
        String custom = getCustomMessage(player, messageType);
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
