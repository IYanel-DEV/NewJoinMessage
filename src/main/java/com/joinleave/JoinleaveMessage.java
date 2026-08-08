package com.joinleave;

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

    // Configuration file for storing player messages
    private FileConfiguration playersConfig;
    private File playersFile;

    // Database connection for MySQL storage
    private Connection connection;
    private boolean mysqlEnabled;

    // Singleton instance of the plugin
    private static JoinleaveMessage instance;

    // Language management components
    private LanguageConfigs languageConfigs;
    private LanguageManager languageManager;
    private LanguageHandler languageHandler;

    // Called when the plugin is enabled
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // Save default config if it doesn't exist
        Bukkit.getPluginManager().registerEvents(this, this); // Register events

        // Initialize language system:
        // 1. Load default language files from JAR
        languageConfigs = new LanguageConfigs(this);
        languageConfigs.loadConfigs();

        // 2. Set up language handler for message translations
        languageHandler = new LanguageHandler(this);

        // Set up language manager with data file
        File dataLangFile = new File(getDataFolder(), "Lang/DataLang.yml");
        languageManager = new LanguageManager(dataLangFile);

        // Register join event listener with language support
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(languageManager), this);

        // Display fancy startup message and check for updates
        Bukkit.getScheduler().runTask(this, () -> {
            ConsoleCommandSender console = Bukkit.getConsoleSender();

            // Build ASCII art header for console
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

            // Check for plugin updates
            UpdateChecker.init(this, 110979).requestUpdateCheck().whenComplete((result, e) -> {
                if (result.requiresUpdate()) {
                    // Notify about available update
                    String pluginName = "                       [" + getDescription().getName() + "]";
                    String updateMessage = pluginName + " " + ChatColor.RED + "An update is available! New version: " + result.getNewestVersion();
                    messageBuilder.append(updateMessage);
                    console.sendMessage(messageBuilder.toString());
                } else {
                    // Plugin is up to date
                    String pluginName = "                        " + getDescription().getName() + " ";
                    String upToDateMessage = pluginName + " " + ChatColor.GREEN + "Plugin is up to date!";
                    messageBuilder.append(upToDateMessage);
                    console.sendMessage(messageBuilder.toString());
                }
            });
        });

        // Set up metrics (bStats) for plugin statistics
        int pluginId = 18952;
        Metrics Metrics = new Metrics(this, pluginId);

        // Register player welcome event
        PlayerWelcome playerWelcome = new PlayerWelcome(this);
        Bukkit.getPluginManager().registerEvents(playerWelcome, this);

        // Log system encoding for debugging
        String defaultEncoding = System.getProperty("file.encoding");
        getLogger().info("Default system encoding: " + defaultEncoding);

        // Register main command executor
        JoinleaveCommand joinLeaveCommand = new JoinleaveCommand(this);
        getCommand("njm").setExecutor(joinLeaveCommand);

        // Register GUI listener for join/leave messages
        JoinLeaveGUI guiListener = new JoinLeaveGUI(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        // Set up player data storage
        playersFile = new File(getDataFolder(), "data.yml");
        if (!playersFile.exists()) {
            saveResource("data.yml", false); // Create default data file if missing
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);

        // Set up MySQL if enabled in config
        mysqlEnabled = getConfig().getBoolean("mysql.enabled");
        if (mysqlEnabled) {
            if (setupMySQL()) {
                createTableIfNotExists(); // Ensure database table exists
            } else {
                getLogger().severe("Failed to connect to MySQL. Please check your configuration.");
            }
        }

        // Register the command executor for the '/njm' command
        getCommand("njm").setExecutor(new JoinleaveCommand(this));
    }

    // Called when plugin is disabled
    @Override
    public void onDisable() {
        // Save player data and close MySQL connection
        if (playersConfig != null) {
            savePlayersConfig();
        }
        closeMySQLConnection();
    }

    // Example method to handle player join event
    public void onPlayerJoin(Player player) {
        // Set default language for new players
        String defaultLanguage = "English";
        languageManager.setPlayerLanguage(player, defaultLanguage);
    }

    // Get plugin instance (singleton pattern)
    public static JoinleaveMessage getInstance() {
        return instance;
    }

    // Get player configuration data
    private FileConfiguration getPlayersConfig() {
        return playersConfig;
    }

    // Save player configuration to file
    private void savePlayersConfig() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }

    // Check if player has custom join/leave messages
    public boolean hasCustomMessage(Player player) {
        return getMessage(player, "join", "default-join-message") != null ||
                getMessage(player, "leave", "default-leave-message") != null;
    }

    // Get last change timestamp for a player's message
    public String getLastChange(Player player, String messageType) {
        FileConfiguration playersConfig = getPlayersConfig();

        String lastChangePath = "players." + player.getUniqueId() + ".last_change." + messageType;
        if (playersConfig.contains(lastChangePath)) {
            long lastChangeTimestamp = playersConfig.getLong(lastChangePath);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.format(new Date(lastChangeTimestamp));
        }

        return "N/A"; // Return if no timestamp found
    }

    // Set up MySQL connection
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
            getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
            return false;
        }
    }

    // Close MySQL connection
    private void closeMySQLConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                getLogger().severe("Failed to close MySQL connection: " + e.getMessage());
            }
        }
    }

    // Create MySQL table if it doesn't exist
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

    // Handle tab completion for commands
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Tab completion for main commands
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("setplayer"); // Set message for another player
            subCommands.add("set");       // Set your own message
            subCommands.add("gui");       // Open GUI editor
            subCommands.add("clear");    // Clear messages
            subCommands.add("reload");    // Reload plugin
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        }
        // Tab completion for player names when using setplayer
        else if (args.length == 2 && args[0].equalsIgnoreCase("setplayer")) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        }
        // Tab completion for message types (join/leave)
        else if (args.length == 3 && args[0].equalsIgnoreCase("setplayer")) {
            List<String> messageTypes = new ArrayList<>();
            messageTypes.add("join");
            messageTypes.add("leave");
            StringUtil.copyPartialMatches(args[2], messageTypes, completions);
        }
        // Tab completion for message types in set/clear commands
        else if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("clear"))) {
            List<String> messageTypes = new ArrayList<>();
            messageTypes.add("join");
            messageTypes.add("leave");
            StringUtil.copyPartialMatches(args[1], messageTypes, completions);
        }

        Collections.sort(completions);
        return completions;
    }

    // Reload plugin configuration
    public void reloadPlugin(CommandSender sender) {
        // List of config files to check
        List<String> configFiles = Arrays.asList("config.yml", "firework.yml", "players.yml", "data.yml");

        // Check each config file
        for (String configFile : configFiles) {
            File file = new File(getDataFolder(), configFile);

            if (!file.exists()) {
                // Create missing config file
                saveResource(configFile, false);

                if (sender instanceof Player) {
                    // Notify player about created file
                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Checking " + configFile + "...");
                    player.sendMessage(ChatColor.DARK_PURPLE + configFile + " not found, created default configuration." + ChatColor.GREEN + " ✔");
                } else {
                    // Log to console
                    getLogger().info("Checking " + configFile + "...");
                    getLogger().info(configFile + " not found, created default configuration.");
                }
            }
        }

        // Reload main configuration
        reloadConfig();

        // Special handling for firework config
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

        // Handle MySQL configuration changes
        boolean newMySQLStatus = getConfig().getBoolean("mysql.enabled");

        if (newMySQLStatus != mysqlEnabled) {
            if (newMySQLStatus) {
                // MySQL was enabled in config
                closeMySQLConnection();
                if (setupMySQL()) {
                    createTableIfNotExists();
                    getLogger().info("MySQL has been enabled and connected successfully.");
                } else {
                    getLogger().severe("Failed to connect to MySQL. Please check your configuration.");
                }
            } else {
                // MySQL was disabled in config
                closeMySQLConnection();
                getLogger().info("MySQL has been disabled.");
            }
            mysqlEnabled = newMySQLStatus;
        }

        // Handle connection state mismatches
        if (mysqlEnabled && connection == null) {
            if (setupMySQL()) {
                createTableIfNotExists();
                getLogger().info("MySQL has been enabled and connected successfully.");
            } else {
                getLogger().severe("Failed to connect to MySQL. Please check your configuration.");
            }
        }

        if (!mysqlEnabled && connection != null) {
            closeMySQLConnection();
            getLogger().info("System is now on local files");
        }

        // Send reload confirmation
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Plugin reloaded" + ChatColor.GREEN + " ✔");
        } else {
            getLogger().info("Plugin reloaded successfully.");
        }
    }

    // Clear player's custom messages
    public void clearMessage(Player player, String messageType) {
        if (messageType.equals("all")) {
            // Clear both join and leave messages
            setMessage(player, "join", "");
            setMessage(player, "leave", "");
        } else {
            // Clear specific message type
            setMessage(player, messageType, "");
        }
    }

    // Reset player's messages to defaults
    public void resetPlayerMessages(Player player) {
        setMessage(player, "join", getConfig().getString("default-join-message"));
        setMessage(player, "leave", getConfig().getString("default-leave-message"));
    }

    // Handle player join event to display custom message
    @EventHandler
    public void handleJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        // Build join message components:
        // 1. Join prefix from config
        String joinPrefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("join-prefix"));
        // 2. Default join prefix with player name
        String defaultJoinPrefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("default-join-prefix").replace("PLAYERNAME", playerName));
        // 3. Custom join message if set
        String joinEditable = getMessage(player, "join", "default-join-message");

        // Combine components to form final message
        String joinMessage = joinPrefix + " " + defaultJoinPrefix + ChatColor.GRAY + " - " + parseMessage(joinEditable, player);

        event.setJoinMessage(joinMessage);
    }

    // Handle player quit event to display custom message
    @EventHandler
    public void handleLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        // Build quit message components:
        // 1. Leave prefix from config
        String leavePrefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("leave-prefix"));
        // 2. Default leave prefix with player name
        String defaultLeavePrefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("default-leave-prefix").replace("PLAYERNAME", playerName));
        // 3. Custom leave message if set
        String leaveEditable = getMessage(player, "leave", "default-leave-message");

        // Combine components to form final message
        String leaveMessage = leavePrefix + " " + defaultLeavePrefix + ChatColor.GRAY + " - " + parseMessage(leaveEditable, player);

        event.setQuitMessage(leaveMessage);
    }

    // Parse message placeholders and color codes
    private String parseMessage(String message, Player player) {
        String parsedMessage = ChatColor.translateAlternateColorCodes('&', message);

        // Replace PLAYERNAME placeholder if present
        if (parsedMessage.contains("PLAYERNAME")) {
            parsedMessage = parsedMessage.replace("PLAYERNAME", player.getName());
        }

        return parsedMessage;
    }

    // Reload player configuration
    private void reloadPlayersConfig() {
        playersFile = new File(getDataFolder(), "data.yml");
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    // Set custom message for player (stored in MySQL or file)
    public void setMessage(Player player, String column, String message) {
        if (mysqlEnabled) {
            // Store in MySQL database
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
            // Store in local file
            playersConfig.set("players." + player.getUniqueId() + "." + column + "_message", message);
            updateLastChange(player, column);
            savePlayersConfig();
        }
    }

    // Update last change timestamp for a message
    private void updateLastChange(Player player, String messageType) {
        FileConfiguration playersConfig = getPlayersConfig();
        playersConfig.set("players." + player.getUniqueId() + ".last_change." + messageType, System.currentTimeMillis());
        savePlayersConfig();
    }

    // Get player's custom message or default if not set
    public String getMessage(Player player, String messageType, String defaultMessageType) {
        if (mysqlEnabled) {
            // Retrieve from MySQL
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
            // Retrieve from local file
            String message = playersConfig.getString("players." + player.getUniqueId() + "." + messageType + "_message");
            if (message != null && !message.isEmpty()) {
                return message;
            }
        }

        // Return default message if no custom message found
        return getConfig().getString(defaultMessageType);
    }
}