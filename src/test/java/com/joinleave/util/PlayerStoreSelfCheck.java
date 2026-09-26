package com.joinleave.util;

import com.joinleave.storage.MySqlBackend;
import com.joinleave.storage.PlayerStore;
import com.joinleave.storage.StorageBackend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exercises the storage layer without a running server: field round-trips, the
 * offline name index, empty-string handling, and flush retry behaviour.
 */
public final class PlayerStoreSelfCheck {

    /** In-memory backend that can be told to fail, to prove failures are retried. */
    private static final class FakeBackend implements StorageBackend {
        final Map<UUID, Map<String, String>> data = new HashMap<UUID, Map<String, String>>();
        boolean failNextPersist;
        boolean failLoad;
        int persistCalls;

        @Override
        public String name() {
            return "fake";
        }

        @Override
        public Map<UUID, Map<String, String>> loadAll() {
            if (failLoad) {
                throw new IllegalStateException("simulated load failure");
            }
            return new HashMap<UUID, Map<String, String>>(data);
        }

        @Override
        public void persist(Set<UUID> dirty, Map<UUID, Map<String, String>> snapshot) {
            persistCalls++;
            if (failNextPersist) {
                throw new IllegalStateException("simulated backend failure");
            }
            for (UUID uuid : dirty) {
                Map<String, String> record = snapshot.get(uuid);
                if (record == null) {
                    data.remove(uuid);
                } else {
                    data.put(uuid, new HashMap<String, String>(record));
                }
            }
        }

        @Override
        public void close() {
        }
    }

    public static void main(String[] args) throws IOException {
        File tmp = File.createTempFile("njm-store-check", ".yml");
        // PlayerStore only writes through the backend, so the file just needs to exist.
        new FileOutputStream(tmp).close();

        Logger logger = Logger.getLogger(PlayerStoreSelfCheck.class.getName());
        logger.setLevel(Level.OFF);

        FakeBackend backend = new FakeBackend();
        UUID alice = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID bob = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // Seed one record so loadAll has something to return.
        Map<String, String> seeded = new HashMap<String, String>();
        seeded.put(PlayerStore.KEY_JOIN, "&aWelcome");
        backend.data.put(alice, seeded);

        PlayerStore store = new PlayerStore(backend, logger);
        store.load();

        check("loaded record survives", "&aWelcome".equals(store.getString(alice, PlayerStore.KEY_JOIN)));
        check("unknown player reads as null", store.getString(bob, PlayerStore.KEY_JOIN) == null);

        // Every field the MySQL schema has to carry.
        store.setString(bob, PlayerStore.KEY_JOIN, "&ahello");
        store.setString(bob, PlayerStore.KEY_LEAVE, "&cbye");
        store.setBoolean(bob, PlayerStore.KEY_BROADCAST, false);
        store.setString(bob, PlayerStore.KEY_ICON, "&6★");
        store.setString(bob, PlayerStore.KEY_JOIN_SOUND, "LEVEL_UP");
        store.setString(bob, PlayerStore.KEY_LEAVE_SOUND, "off");
        store.setLong(bob, PlayerStore.KEY_LAST_CHANGE_JOIN, 1700000000000L);
        store.setName(bob, "Bob");

        check("join message", "&ahello".equals(store.getString(bob, PlayerStore.KEY_JOIN)));
        check("leave message", "&cbye".equals(store.getString(bob, PlayerStore.KEY_LEAVE)));
        check("broadcast flag", !store.getBoolean(bob, PlayerStore.KEY_BROADCAST, true));
        check("icon", "&6★".equals(store.getString(bob, PlayerStore.KEY_ICON)));
        check("join sound", "LEVEL_UP".equals(store.getString(bob, PlayerStore.KEY_JOIN_SOUND)));
        check("leave sound", "off".equals(store.getString(bob, PlayerStore.KEY_LEAVE_SOUND)));
        check("last change", store.getLong(bob, PlayerStore.KEY_LAST_CHANGE_JOIN, -1L) == 1700000000000L);
        check("broadcast default when unset", store.getBoolean(alice, PlayerStore.KEY_BROADCAST, true));

        // Offline lookup by name.
        check("resolve by name", bob.equals(store.resolveUuid("Bob")));
        check("resolve by lowercase name", bob.equals(store.resolveUuid("bob")));
        check("unknown name resolves to null", store.resolveUuid("Nobody") == null);
        store.setName(bob, "Robert");
        check("rename reindexes", bob.equals(store.resolveUuid("Robert")) && store.resolveUuid("Bob") == null);
        store.setName(bob, "Bob");
        check("rename back reindexes", bob.equals(store.resolveUuid("Bob")) && store.resolveUuid("Robert") == null);

        // Clearing a message stores an empty string, which must read back as absent so
        // the default message takes over again.
        store.setString(bob, PlayerStore.KEY_JOIN, "");
        check("empty string reads as absent", store.getString(bob, PlayerStore.KEY_JOIN) == null);

        // Flush writes through, and a failing flush keeps the work for the next attempt.
        check("dirty before flush", store.dirtyCount() > 0);
        store.flush();
        check("clean after flush", store.dirtyCount() == 0);
        check("backend received the write", backend.data.get(bob).get(PlayerStore.KEY_ICON) != null);

        store.setString(bob, PlayerStore.KEY_ICON, "&b*");
        backend.failNextPersist = true;
        store.flush();
        check("failed flush keeps work queued", store.dirtyCount() > 0);
        backend.failNextPersist = false;
        store.flush();
        check("retry succeeds", store.dirtyCount() == 0);
        check("retry persisted the value", "&b*".equals(backend.data.get(bob).get(PlayerStore.KEY_ICON)));

        // Reloading from the backend must reproduce the same view.
        PlayerStore reloaded = new PlayerStore(backend, logger);
        reloaded.load();
        check("reload keeps join sound", "LEVEL_UP".equals(reloaded.getString(bob, PlayerStore.KEY_JOIN_SOUND)));
        check("reload keeps name index", bob.equals(reloaded.resolveUuid("Bob")));
        check("reload keeps tracked count", reloaded.trackedPlayers() == 2);

        // /njm list must show only players with a real message, sorted by name.
        // alice has a seeded join message, bob only a leave message (join was cleared),
        // carol gets one here, and dave is name-only so he must never be listed.
        UUID carol = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID dave = UUID.fromString("44444444-4444-4444-4444-444444444444");
        reloaded.setString(carol, PlayerStore.KEY_NAME, "Carol");
        reloaded.setString(carol, PlayerStore.KEY_JOIN, "&ahello carol");
        reloaded.setString(dave, PlayerStore.KEY_NAME, "Dave");

        List<UUID> listed = reloaded.playersWithCustomData();
        check("list excludes name-only players", !listed.contains(dave));
        check("list includes a cleared-join player via leave", listed.contains(bob));
        check("list includes the seeded player", listed.contains(alice));
        check("list includes the new player", listed.contains(carol));
        check("list has one entry per player with a message", listed.size() == 3);
        check("list is sorted by name", listed.get(0).equals(alice)
                && listed.get(1).equals(bob) && listed.get(2).equals(carol));


        Set<UUID> empty = new HashSet<UUID>();
        check("snapshot is a copy", reloaded.snapshot() != reloaded.snapshot());

        // A backend that cannot be read must report failure, otherwise the plugin would
        // start on empty data and every player's messages would look deleted.
        FakeBackend broken = new FakeBackend();
        broken.failLoad = true;
        PlayerStore failing = new PlayerStore(broken, logger);
        check("load reports failure", !failing.load());
        check("load failure is recorded", failing.loadError() != null);
        check("failed load still counts as loaded", failing.isLoaded());

        // A rejected table name must be refused rather than interpolated into SQL.
        boolean rejected = false;
        try {
            new MySqlBackend("localhost", 3306, "db", "user", "pass", "bad`name; DROP TABLE x--", logger);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("unsafe table name rejected", rejected);
        check("valid table name accepted",
                new MySqlBackend("localhost", 3306, "db", "user", "pass", "player_messages", logger)
                        .name().contains("player_messages"));

        tmp.delete();
        System.out.println("PlayerStoreSelfCheck OK");
    }

    private static void check(String label, boolean condition) {
        if (!condition) {
            throw new AssertionError("failed: " + label);
        }
    }
}
