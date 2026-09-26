package com.joinleave.storage;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistence for per-player NJM data. Implementations own all disk/network I/O;
 * reads are served from the in-memory snapshot held by {@link PlayerStore}.
 */
public interface StorageBackend {

    /** Human-readable backend name for logs and /njm info. */
    String name();

    /** Loads every stored player. Called once, off the main thread, during enable. */
    Map<UUID, Map<String, String>> loadAll() throws Exception;

    /**
     * Persists pending changes.
     *
     * @param dirty    players whose data changed since the last flush
     * @param snapshot current full view, safe to read from another thread
     */
    void persist(Set<UUID> dirty, Map<UUID, Map<String, String>> snapshot) throws Exception;

    /** Releases connections. Must tolerate being called twice. */
    void close();
}
