package com.joinleave.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores player data in MySQL. The table carries every field the plugin knows about,
 * so icons, sounds, the broadcast toggle and change timestamps survive a restart
 * exactly like the join/leave messages do.
 *
 * <p>The table is created and migrated on connect: an older install that only has
 * uuid/join_message/leave_message gains the missing columns automatically.
 */
public final class MySqlBackend implements StorageBackend {

    private static final String[] COLUMNS = {
            "uuid", "name", "join_message", "leave_message", "broadcast_enabled",
            "icon", "join_sound", "leave_sound", "last_change_join", "last_change_leave"
    };

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String table;
    private final Logger logger;

    private Connection connection;

    public MySqlBackend(String host, int port, String database, String username, String password,
                        String table, Logger logger) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.table = sanitizeTable(table);
        this.logger = logger;
    }

    /**
     * Table names cannot be passed as JDBC bind parameters, so they are interpolated
     * into SQL. Restrict the value to characters MySQL allows in an unquoted
     * identifier so a typo in config.yml cannot produce a broken statement.
     */
    private static String sanitizeTable(String table) {
        if (table == null) {
            return "player_messages";
        }
        String trimmed = table.trim();
        if (trimmed.isEmpty()) {
            return "player_messages";
        }
        if (!trimmed.matches("[A-Za-z0-9_$]{1,64}")) {
            throw new IllegalArgumentException("mysql.table must be 1-64 characters of "
                    + "letters, digits, '_' or '$' (got: " + trimmed + ")");
        }
        return trimmed;
    }

    @Override
    public String name() {
        return "MySQL (" + database + "." + table + ")";
    }

    /** Opens the connection and makes sure the schema matches this plugin version. */
    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver missing from the plugin jar", e);
        }
        connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&connectTimeout=5000&socketTimeout=5000", username, password);
        ensureSchema();
    }

    private void ensureSchema() throws SQLException {
        Set<String> existing = new LinkedHashSet<String>();
        Statement statement = connection.createStatement();
        try {
            ResultSet rs = statement.executeQuery("SHOW COLUMNS FROM `" + table + "`");
            try {
                while (rs.next()) {
                    existing.add(rs.getString("Field"));
                }
            } finally {
                rs.close();
            }
        } catch (SQLException e) {
            // Table does not exist yet; create it below.
            logger.log(Level.INFO, "Creating table " + table + " in MySQL.");
        } finally {
            statement.close();
        }

        if (existing.isEmpty()) {
            statement = connection.createStatement();
            try {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` ("
                        + "`uuid` VARCHAR(36) NOT NULL PRIMARY KEY, "
                        + "`name` VARCHAR(16) NULL, "
                        + "`join_message` TEXT NULL, "
                        + "`leave_message` TEXT NULL, "
                        + "`broadcast_enabled` VARCHAR(5) NULL, "
                        + "`icon` VARCHAR(64) NULL, "
                        + "`join_sound` VARCHAR(128) NULL, "
                        + "`leave_sound` VARCHAR(128) NULL, "
                        + "`last_change_join` BIGINT NULL, "
                        + "`last_change_leave` BIGINT NULL"
                        + ")");
            } finally {
                statement.close();
            }
            return;
        }

        // Migrate: add any column an older install never had.
        addColumnIfMissing(statement, existing, "name", "VARCHAR(16) NULL");
        addColumnIfMissing(statement, existing, "broadcast_enabled", "VARCHAR(5) NULL");
        addColumnIfMissing(statement, existing, "icon", "VARCHAR(64) NULL");
        addColumnIfMissing(statement, existing, "join_sound", "VARCHAR(128) NULL");
        addColumnIfMissing(statement, existing, "leave_sound", "VARCHAR(128) NULL");
        addColumnIfMissing(statement, existing, "last_change_join", "BIGINT NULL");
        addColumnIfMissing(statement, existing, "last_change_leave", "BIGINT NULL");
    }

    private void addColumnIfMissing(Statement statement, Set<String> existing, String column, String definition)
            throws SQLException {
        if (existing.contains(column)) {
            return;
        }
        logger.log(Level.INFO, "Migrating MySQL table: adding missing column " + column + ".");
        statement.executeUpdate("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
        existing.add(column);
    }

    @Override
    public Map<UUID, Map<String, String>> loadAll() throws SQLException {
        Map<UUID, Map<String, String>> result = new LinkedHashMap<UUID, Map<String, String>>();
        connect();
        StringBuilder sql = new StringBuilder("SELECT ");
        for (int i = 0; i < COLUMNS.length; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append('`').append(COLUMNS[i]).append('`');
        }
        sql.append(" FROM `").append(table).append('`');

        PreparedStatement statement = connection.prepareStatement(sql.toString());
        try {
            ResultSet rs = statement.executeQuery();
            try {
                while (rs.next()) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(rs.getString("uuid"));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Skipping invalid UUID in " + table + ": " + rs.getString("uuid"));
                        continue;
                    }
                    Map<String, String> record = new HashMap<String, String>();
                    for (String column : COLUMNS) {
                        if ("uuid".equals(column)) {
                            continue;
                        }
                        String value = rs.getString(column);
                        if (value != null && !value.isEmpty()) {
                            record.put(column, value);
                        }
                    }
                    if (!record.isEmpty()) {
                        result.put(uuid, record);
                    }
                }
            } finally {
                rs.close();
            }
        } finally {
            statement.close();
        }
        logger.info("Loaded " + result.size() + " player record(s) from MySQL.");
        return result;
    }

    @Override
    public void persist(Set<UUID> dirty, Map<UUID, Map<String, String>> snapshot) throws SQLException {
        if (dirty.isEmpty()) {
            return;
        }
        connect();

        StringBuilder sql = new StringBuilder("INSERT INTO `").append(table).append("` (");
        for (int i = 0; i < COLUMNS.length; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append('`').append(COLUMNS[i]).append('`');
        }
        sql.append(") VALUES (");
        for (int i = 0; i < COLUMNS.length; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append('?');
        }
        sql.append(") ON DUPLICATE KEY UPDATE ");
        for (int i = 1; i < COLUMNS.length; i++) {
            if (i > 1) {
                sql.append(", ");
            }
            sql.append('`').append(COLUMNS[i]).append("` = VALUES(`").append(COLUMNS[i]).append("`)");
        }

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        PreparedStatement statement = connection.prepareStatement(sql.toString());
        try {
            List<UUID> batch = new ArrayList<UUID>(dirty);
            for (UUID uuid : batch) {
                Map<String, String> record = snapshot.get(uuid);
                for (int i = 0; i < COLUMNS.length; i++) {
                    String column = COLUMNS[i];
                    if ("uuid".equals(column)) {
                        statement.setString(i + 1, uuid.toString());
                    } else {
                        statement.setString(i + 1, record == null ? null : record.get(column));
                    }
                }
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            try {
                statement.close();
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to close the MySQL connection.", e);
            }
            connection = null;
        }
    }
}
