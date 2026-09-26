package com.joinleave.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory view of every player's NJM data, with write-behind persistence.
 *
 * <p>Reads never touch the disk or the database, so the join/leave events stay off
 * blocking I/O. Writes update the snapshot immediately and mark the player dirty;
 * a background task flushes dirty players through the {@link StorageBackend}.
 */
public final class PlayerStore {

    public static final String KEY_JOIN = "join_message";
    public static final String KEY_LEAVE = "leave_message";
    public static final String KEY_BROADCAST = "broadcast_enabled";
    public static final String KEY_ICON = "icon";
    public static final String KEY_JOIN_SOUND = "join_sound";
    public static final String KEY_LEAVE_SOUND = "leave_sound";
    public static final String KEY_LAST_CHANGE_JOIN = "last_change_join";
    public static final String KEY_LAST_CHANGE_LEAVE = "last_change_leave";
    public static final String KEY_NAME = "name";

    private final StorageBackend backend;
    private final Logger logger;

    private final Map<UUID, Map<String, String>> data = new ConcurrentHashMap<UUID, Map<String, String>>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<String, UUID>();

    private volatile boolean loaded;
    private volatile Throwable loadError;

    public PlayerStore(StorageBackend backend, Logger logger) {
        this.backend = backend;
        this.logger = logger;
    }

    public String backendName() {
        return backend.name();
    }

    /**
     * One-time load. Safe to call from the main thread during enable.
     *
     * @return true when the backend was read successfully. A false return lets the
     *         caller fall back to another backend instead of silently running on
     *         empty data, which would look like every player's messages vanished.
     */
    public boolean load() {
        try {
            Map<UUID, Map<String, String>> loadedData = backend.loadAll();
            data.clear();
            nameIndex.clear();
            if (loadedData != null) {
                for (Map.Entry<UUID, Map<String, String>> entry : loadedData.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    Map<String, String> copy = new ConcurrentHashMap<String, String>(entry.getValue());
                    data.put(entry.getKey(), copy);
                    indexName(entry.getKey(), copy.get(KEY_NAME));
                }
            }
            dirty.clear();
            loaded = true;
            return true;
        } catch (Throwable t) {
            loaded = true;
            loadError = t;
            logger.log(Level.SEVERE, "Failed to load player data from " + backend.name(), t);
            return false;
        }
    }

    public Throwable loadError() {
        return loadError;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public int trackedPlayers() {
        return data.size();
    }

    /**
     * UUIDs that have a join or leave message of their own, sorted by name so
     * {@code /njm list} paginates in a stable order. Players whose only stored
     * value is a seen name are not included.
     */
    public List<UUID> playersWithCustomData() {
        List<UUID> result = new ArrayList<UUID>();
        for (Map.Entry<UUID, Map<String, String>> entry : data.entrySet()) {
            Map<String, String> record = entry.getValue();
            if (getString(entry.getKey(), KEY_JOIN) != null || getString(entry.getKey(), KEY_LEAVE) != null) {
                result.add(entry.getKey());
            }
        }
        Collections.sort(result, new Comparator<UUID>() {
            @Override
            public int compare(UUID left, UUID right) {
                String leftName = nameOf(left);
                String rightName = nameOf(right);
                int byName = leftName.compareToIgnoreCase(rightName);
                // Fall back to the UUID so two players sharing a name keep a fixed order.
                return byName != 0 ? byName : left.compareTo(right);
            }

            private String nameOf(UUID uuid) {
                String name = getString(uuid, KEY_NAME);
                return name == null ? "" : name;
            }
        });
        return result;
    }

    public String getString(UUID uuid, String key) {
        Map<String, String> record = data.get(uuid);
        if (record == null) {
            return null;
        }
        String value = record.get(key);
        return value == null || value.isEmpty() ? null : value;
    }

    public boolean getBoolean(UUID uuid, String key, boolean fallback) {
        String value = getString(uuid, key);
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }

    public long getLong(UUID uuid, String key, long fallback) {
        String value = getString(uuid, key);
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void setString(UUID uuid, String key, String value) {
        if (uuid == null) {
            return;
        }
        Map<String, String> record = recordFor(uuid);
        if (value == null) {
            record.remove(key);
        } else {
            record.put(key, value);
        }
        dirty.add(uuid);
    }

    public void setBoolean(UUID uuid, String key, boolean value) {
        setString(uuid, key, Boolean.toString(value));
    }

    public void setLong(UUID uuid, String key, long value) {
        setString(uuid, key, Long.toString(value));
    }

    public void setName(UUID uuid, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        String previous = getString(uuid, KEY_NAME);
        if (previous != null) {
            nameIndex.remove(previous.toLowerCase(Locale.ENGLISH));
        }
        setString(uuid, KEY_NAME, name);
        indexName(uuid, name);
    }

    /**
     * Resolves a player name to a stored UUID, covering players who are offline.
     * Only players who already have stored data can be resolved.
     */
    public UUID resolveUuid(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return nameIndex.get(name.toLowerCase(Locale.ENGLISH));
    }

    public String resolveName(UUID uuid) {
        return uuid == null ? null : getString(uuid, KEY_NAME);
    }

    /** Read-only snapshot for backends and diagnostics. */
    public Map<UUID, Map<String, String>> snapshot() {
        Map<UUID, Map<String, String>> copy = new HashMap<UUID, Map<String, String>>();
        for (Map.Entry<UUID, Map<String, String>> entry : data.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<String, String>(entry.getValue())));
        }
        return copy;
    }

    public void markAllDirty() {
        dirty.addAll(data.keySet());
    }

    public int dirtyCount() {
        return dirty.size();
    }

    /**
     * Flushes dirty players. Safe to call off the main thread.
     *
     * <p>Synchronized because the async flush task and {@code close()} (triggered by a
     * reload or shutdown) can otherwise write the same backend at the same time, which
     * for the YAML backend means two threads saving one file.
     */
    public synchronized void flush() {
        if (dirty.isEmpty()) {
            return;
        }
        Set<UUID> batch = new java.util.HashSet<UUID>(dirty);
        dirty.removeAll(batch);
        try {
            backend.persist(batch, snapshot());
        } catch (Throwable t) {
            // Put the work back so the next flush retries instead of losing it.
            dirty.addAll(batch);
            logger.log(Level.WARNING, "Failed to persist player data to " + backend.name()
                    + "; keeping changes for the next flush.", t);
        }
    }

    /** Synchronous flush for plugin disable, where async tasks are already cancelled. */
    public void flushBlocking() {
        flush();
    }

    public synchronized void close() {
        flushBlocking();
        backend.close();
    }

    private Map<String, String> recordFor(UUID uuid) {
        Map<String, String> record = data.get(uuid);
        if (record == null) {
            record = new ConcurrentHashMap<String, String>();
            Map<String, String> existing = data.putIfAbsent(uuid, record);
            if (existing != null) {
                record = existing;
            }
        }
        return record;
    }

    private void indexName(UUID uuid, String name) {
        if (uuid != null && name != null && !name.isEmpty()) {
            nameIndex.put(name.toLowerCase(Locale.ENGLISH), uuid);
        }
    }
}
