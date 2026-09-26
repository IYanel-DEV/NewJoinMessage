package com.joinleave.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Chooses a storage backend from config, falling back to data.yml when MySQL is unusable. */
public final class StorageFactory {

    private StorageFactory() {
    }

    public static PlayerStore create(JavaPlugin plugin) {
        Logger logger = plugin.getLogger();
        FileConfiguration config = plugin.getConfig();
        boolean mysqlRequested = config.getBoolean("mysql.enabled", false);

        if (mysqlRequested) {
            MySqlBackend mysql = null;
            try {
                // Built inside the try: a bad mysql.table throws here, and it must
                // degrade to data.yml rather than break plugin enable.
                mysql = new MySqlBackend(
                        config.getString("mysql.host", "localhost"),
                        config.getInt("mysql.port", 3306),
                        config.getString("mysql.database", ""),
                        config.getString("mysql.username", ""),
                        config.getString("mysql.password", ""),
                        config.getString("mysql.table", "player_messages"),
                        logger);
                mysql.connect();
                PlayerStore store = new PlayerStore(mysql, logger);
                // Honour the load result: an unreadable MySQL must fall back to data.yml
                // instead of running on empty data and looking like every message was lost.
                if (store.load()) {
                    logger.info("Player data backend: " + store.backendName()
                            + " (" + store.trackedPlayers() + " players)");
                    return store;
                }
                logger.severe("MySQL was reachable but its data could not be read; "
                        + "falling back to data.yml for this session.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "MySQL is enabled but unusable (" + e.getMessage()
                        + "). Falling back to data.yml for this session.", e);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "MySQL setup failed unexpectedly. Falling back to data.yml.", t);
            }
            if (mysql != null) {
                mysql.close();
            }
        }

        PlayerStore yamlStore = new PlayerStore(new YamlBackend(dataFile(plugin), logger), logger);
        if (!yamlStore.load()) {
            logger.severe("data.yml could not be read; starting with empty player data.");
        }
        logger.info("Player data backend: " + yamlStore.backendName()
                + " (" + yamlStore.trackedPlayers() + " players)");
        return yamlStore;
    }

    public static File dataFile(JavaPlugin plugin) {
        return new File(plugin.getDataFolder(), "data.yml");
    }
}
