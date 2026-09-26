package com.joinleave.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores player data in data.yml. The whole file is rewritten on flush because it is
 * a single document; the in-memory store in {@link PlayerStore} is what keeps reads cheap.
 */
public final class YamlBackend implements StorageBackend {

    private final File file;
    private final Logger logger;

    public YamlBackend(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public String name() {
        return "data.yml";
    }

    @Override
    public Map<UUID, Map<String, String>> loadAll() {
        Map<UUID, Map<String, String>> result = new LinkedHashMap<UUID, Map<String, String>>();
        if (!file.exists()) {
            return result;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return result;
        }
        for (String key : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                logger.warning("Skipping invalid UUID key in data.yml: " + key);
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Map<String, String> record = new HashMap<String, String>();
            for (String field : section.getKeys(true)) {
                Object value = section.get(field);
                if (value != null && !(value instanceof ConfigurationSection)) {
                    record.put(field, String.valueOf(value));
                }
            }
            if (!record.isEmpty()) {
                result.put(uuid, record);
            }
        }
        return result;
    }

    @Override
    public void persist(Set<UUID> dirty, Map<UUID, Map<String, String>> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, String>> entry : snapshot.entrySet()) {
            String base = "players." + entry.getKey();
            for (Map.Entry<String, String> field : entry.getValue().entrySet()) {
                yaml.set(base + "." + field.getKey(), field.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write " + file.getName(), e);
        }
    }

    @Override
    public void close() {
        // Nothing to release.
    }
}
