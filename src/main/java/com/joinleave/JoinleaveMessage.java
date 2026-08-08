package com.joinleave;

import com.joinleave.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
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



public class JoinleaveMessage extends JavaPlugin implements Listener {

    private FileConfiguration playersConfig;
    private File playersFile;

    private Connection connection;
    private boolean mysqlEnabled;

    private static JoinleaveMessage instance;

    private LanguageConfigs languageConfigs;
    private LanguageManager languageManager;
    private LanguageHandler languageHandler;

    @Override
    public void onEnable() {
        instance = this;
        try {
            enablePlugin();
        } catch (Throwable t) {
            getLogger().severe("NewJoinMessage failed to enable safely: " + t.getMessage());
            t.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
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
            int pluginId = 18952;
            Metrics metrics = new Metrics(this, pluginId);
        } catch (Throwable t) {
            getLogger().warning("Metrics (bStats) failed to initialize: " + t.getMessage());
        }

        PlayerWelcome playerWelcome = new PlayerWelcome(this);
        Bukkit.getPluginManager().registerEvents(playerWelcome, this);

        String defaultEncoding = System.getProperty("file.encoding");
        getLogger().info("Default system encoding: " + defaultEncoding);

        JoinleaveCommand joinLeaveCommand = new JoinleaveCommand(this);
        getCommand("njm").setExecutor(joinLeaveCommand);
        getCommand("njm").setTabCompleter(joinLeaveCommand);

        JoinLeaveGUI guiListener = new JoinLeaveGUI(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        playersFile = new File(getDataFolder(), "data.yml");
        if (!playersFile.exists()) {
            saveResource("data.yml", false);
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);

        mysqlEnabled = getConfig().getBoolean("mysql.enabled");
        if (mysqlEnabled) {
            if (setupMySQL()) {
                createTableIfNotExists();
            } else {
                mysqlEnabled = false;
                connection = null;
                getLogger().severe("MySQL failed — falling back to data.yml for this session.");
            }
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
        return getMessage(player, "join", "default-join-message") != null ||
                getMessage(player, "leave", "default-leave-message") != null;
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
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false", username, password);
            return true;
        } catch (SQLException e) {
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

    private void createTableIfNotExists() {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_messages (uuid VARCHAR(36) PRIMARY KEY, join_message TEXT, leave_message TEXT)");
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            getLogger().severe("Failed to create player_messages table: " + e.getMessage());
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
                if (setupMySQL()) {
                    createTableIfNotExists();
                    mysqlEnabled = true;
                    getLogger().info("MySQL has been enabled and connected successfully.");
                } else {
                    mysqlEnabled = false;
                    connection = null;
                    getLogger().severe("MySQL failed — falling back to data.yml for this session.");
                }
            } else {
                closeMySQLConnection();
                mysqlEnabled = false;
                getLogger().info("MySQL has been disabled.");
            }
        }

        if (mysqlEnabled && connection == null) {
            if (setupMySQL()) {
                createTableIfNotExists();
                getLogger().info("MySQL has been enabled and connected successfully.");
            } else {
                mysqlEnabled = false;
                connection = null;
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

    private String parsePlaceholders(String message, Player player) {
        if (message == null) return "";
        return message.contains("PLAYERNAME") ? message.replace("PLAYERNAME", player.getName()) : message;
    }

    @EventHandler
    public void handleJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            String playerName = player.getName();
            String joinPrefix = ColorUtils.colorize(cfg("join-prefix", "&d[&a+&d]"));
            String defaultJoinPrefix = ColorUtils.colorize(
                    cfg("default-join-prefix", "&7PLAYERNAME &5has joined").replace("PLAYERNAME", playerName));
            String joinEditable = getMessage(player, "join", "default-join-message");
            String joinMessage = joinPrefix + " " + defaultJoinPrefix + ChatColor.GRAY + " - "
                    + ColorUtils.colorize(parsePlaceholders(joinEditable, player));
            event.setJoinMessage(joinMessage);
        } catch (Throwable t) {
            getLogger().warning("handleJoin failed for " + event.getPlayer().getName() + ": " + t.getMessage());
            event.setJoinMessage(event.getPlayer().getName() + " joined");
        }
    }

    @EventHandler
    public void handleLeave(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            String playerName = player.getName();
            String leavePrefix = ColorUtils.colorize(cfg("leave-prefix", "&d[&c-&d]"));
            String defaultLeavePrefix = ColorUtils.colorize(
                    cfg("default-leave-prefix", "&7PLAYERNAME &5has left").replace("PLAYERNAME", playerName));
            String leaveEditable = getMessage(player, "leave", "default-leave-message");
            String leaveMessage = leavePrefix + " " + defaultLeavePrefix + ChatColor.GRAY + " - "
                    + ColorUtils.colorize(parsePlaceholders(leaveEditable, player));
            event.setQuitMessage(leaveMessage);
        } catch (Throwable t) {
            getLogger().warning("handleLeave failed for " + event.getPlayer().getName() + ": " + t.getMessage());
            event.setQuitMessage(event.getPlayer().getName() + " left");
        }
    }

    private void reloadPlayersConfig() {
        playersFile = new File(getDataFolder(), "data.yml");
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    public void setMessage(Player player, String column, String message) {
        boolean useMysql = mysqlEnabled && connection != null;
        if (useMysql) {
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO player_messages (uuid, " + column + "_message) VALUES (?, ?) ON DUPLICATE KEY UPDATE " + column + "_message = ?");
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, message);
                statement.setString(3, message);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                getLogger().severe("Failed to set " + column + " message for player: " + e.getMessage());
            }
        } else {
            playersConfig.set("players." + player.getUniqueId() + "." + column + "_message", message);
            updateLastChange(player, column);
            savePlayersConfig();
        }
    }

    private void updateLastChange(Player player, String messageType) {
        FileConfiguration playersConfig = getPlayersConfig();
        playersConfig.set("players." + player.getUniqueId() + ".last_change." + messageType, System.currentTimeMillis());
        savePlayersConfig();
    }

    public String getMessage(Player player, String messageType, String defaultMessageType) {
        boolean useMysql = mysqlEnabled && connection != null;
        if (useMysql) {
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + messageType + "_message FROM player_messages WHERE uuid = ?");
                statement.setString(1, player.getUniqueId().toString());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    String message = resultSet.getString(messageType + "_message");
                    if (message != null && !message.isEmpty()) {
                        return message;
                    }
                }

                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                getLogger().severe("Failed to retrieve " + messageType + " message for player: " + e.getMessage());
            }
        } else {
            String message = playersConfig.getString("players." + player.getUniqueId() + "." + messageType + "_message");
            if (message != null && !message.isEmpty()) {
                return message;
            }
        }

        return getConfig().getString(defaultMessageType);
    }
}
