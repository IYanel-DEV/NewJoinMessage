package com.joinleave;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Single owner of Lang/DataLang.yml. The file is held in memory and written through,
 * so language lookups never hit the disk and no two components can disagree about
 * which language a player picked.
 */
public class LanguageManager {

    private final JavaPlugin plugin;
    private final File dataLangFile;
    private final Map<UUID, String> languages = new HashMap<UUID, String>();

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataLangFile = new File(plugin.getDataFolder(), "Lang/DataLang.yml");
        reload();
    }

    /** Re-reads DataLang.yml from disk, discarding the cached view. */
    public final void reload() {
        languages.clear();
        if (!dataLangFile.isFile()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataLangFile);
        for (String key : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            String language = config.getString(key + ".Language");
            if (language != null && !language.isEmpty()) {
                languages.put(uuid, language.toLowerCase(Locale.ENGLISH));
            }
        }
    }

    public String getPlayerLanguage(Player player) {
        if (player == null) {
            return "english";
        }
        String stored = languages.get(player.getUniqueId());
        return stored == null ? "english" : stored;
    }

    public boolean isLanguageSet(Player player) {
        return player != null && languages.containsKey(player.getUniqueId());
    }

    /** Sets a language and persists it immediately. */
    public void setPlayerLanguage(Player player, String language) {
        if (player != null) {
            setLanguage(player.getUniqueId(), language);
        }
    }

    public void setLanguage(UUID uuid, String language) {
        if (uuid == null || language == null || language.isEmpty()) {
            return;
        }
        languages.put(uuid, language.toLowerCase(Locale.ENGLISH));
        write();
    }

    private void write() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : languages.entrySet()) {
            config.set(entry.getKey() + ".Language", entry.getValue());
        }
        try {
            File parent = dataLangFile.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create the Lang folder.");
            }
            config.save(dataLangFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player language data", e);
        }
    }
}
