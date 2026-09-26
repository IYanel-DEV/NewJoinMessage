package com.joinleave;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Resolves translated UI strings. Player language comes from the shared
 * {@link LanguageManager}, so a lookup costs a map read rather than a file read.
 */
public class LanguageHandler {

    private final JavaPlugin plugin;
    private final LanguageManager languageManager;
    private final Map<String, YamlConfiguration> languageFiles = new HashMap<String, YamlConfiguration>();

    private static final String[] LANGUAGES = {
            "english", "germany", "french", "spanish", "italian",
            "chinese", "japanese", "korean", "russian"
    };

    public LanguageHandler(JavaPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        reloadLanguages();
    }

    /** Reloads every Lang/*.yml file so language edits apply without a restart. */
    public void reloadLanguages() {
        languageFiles.clear();
        for (String language : LANGUAGES) {
            loadLanguageFile(language);
        }
        plugin.getLogger().info("Loaded " + languageFiles.size() + " language file(s).");
    }

    private void loadLanguageFile(String language) {
        try {
            File langFile = new File(plugin.getDataFolder(), "Lang/" + language + ".yml");
            if (!langFile.isFile()) {
                return;
            }
            languageFiles.put(language, YamlConfiguration.loadConfiguration(langFile));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading language: " + language, e);
        }
    }

    public String getMessage(Player player, String key) {
        String playerLanguage = languageManager.getPlayerLanguage(player);
        YamlConfiguration langConfig = languageFiles.get(playerLanguage);

        // Try the player's language first.
        if (langConfig != null) {
            String message = langConfig.getString(key);
            if (message != null && !message.isEmpty()) {
                return ChatColor.translateAlternateColorCodes('&', message);
            }
        }

        // Fallback 1: English.
        if (!"english".equals(playerLanguage)) {
            YamlConfiguration englishConfig = languageFiles.get("english");
            if (englishConfig != null) {
                String message = englishConfig.getString(key);
                if (message != null && !message.isEmpty()) {
                    return ChatColor.translateAlternateColorCodes('&', message);
                }
            }
        }

        // Fallback 2: any language that happens to define the key.
        for (YamlConfiguration config : languageFiles.values()) {
            String message = config.getString(key);
            if (message != null && !message.isEmpty()) {
                return ChatColor.translateAlternateColorCodes('&', message);
            }
        }

        return ChatColor.RED + "[" + key + "]";
    }
}
