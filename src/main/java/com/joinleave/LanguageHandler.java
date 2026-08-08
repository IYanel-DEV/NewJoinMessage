package com.joinleave;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LanguageHandler {

    private final JoinleaveMessage plugin;
    private final Map<String, YamlConfiguration> languageFiles;

    public LanguageHandler(JoinleaveMessage plugin) {
        this.plugin = plugin;
        this.languageFiles = new HashMap<>();
        initializeLanguages();
    }

    private void initializeLanguages() {
        String[] languages = {
                "english", "germany", "french", "spanish", "italian",
                "chinese", "japanese", "korean", "russian"
        };

        for (String lang : languages) {
            loadLanguageFile(lang);
        }
    }

    private void loadLanguageFile(String language) {
        try {
            File langFile = new File(plugin.getDataFolder(), "Lang/" + language + ".yml");
            languageFiles.put(language, YamlConfiguration.loadConfiguration(langFile));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading language: " + language, e);
        }
    }

    public String getMessage(Player player, String key) {
        String playerLanguage = getPlayerLanguage(player);
        YamlConfiguration langConfig = languageFiles.get(playerLanguage);

        // Try player's language first
        if (langConfig != null) {
            String message = langConfig.getString(key);
            if (message != null && !message.isEmpty()) {
                return ChatColor.translateAlternateColorCodes('&', message);
            }
        }

        // Fallback 1: Try English
        if (!"english".equals(playerLanguage)) {
            YamlConfiguration englishConfig = languageFiles.get("english");
            if (englishConfig != null) {
                String message = englishConfig.getString(key);
                if (message != null && !message.isEmpty()) {
                    return ChatColor.translateAlternateColorCodes('&', message);
                }
            }
        }

        // Fallback 2: Try any available language
        for (YamlConfiguration config : languageFiles.values()) {
            String message = config.getString(key);
            if (message != null && !message.isEmpty()) {
                return ChatColor.translateAlternateColorCodes('&', message);
            }
        }

        // Final fallback
        return ChatColor.RED + "[" + key + "]";
    }

    private String getPlayerLanguage(Player player) {
        if (player == null) return "english";

        try {
            File dataLangFile = new File(plugin.getDataFolder(), "Lang/DataLang.yml");
            YamlConfiguration dataLangConfig = YamlConfiguration.loadConfiguration(dataLangFile);
            String playerUUID = player.getUniqueId().toString();
            return dataLangConfig.getString(playerUUID + ".Language", "english").toLowerCase();
        } catch (Exception e) {
            return "english";
        }
    }
}