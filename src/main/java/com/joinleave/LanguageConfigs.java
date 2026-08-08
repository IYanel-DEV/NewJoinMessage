package com.joinleave;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class LanguageConfigs {

    private final JavaPlugin plugin;

    public LanguageConfigs(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        File langDir = new File(plugin.getDataFolder(), "Lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // Ensure DataLang.yml exists
        saveResourceIfMissing("Lang/DataLang.yml");

        // Ensure all language files exist
        String[] languages = {
                "english", "germany", "french", "spanish", "italian",
                "chinese", "japanese", "korean", "russian"
        };

        for (String lang : languages) {
            saveResourceIfMissing("Lang/" + lang + ".yml");
        }
    }

    private void saveResourceIfMissing(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            try {
                // Create parent directories if needed
                File parent = file.getParentFile();
                if (!parent.exists()) parent.mkdirs();

                // Copy from JAR resources
                try (InputStream in = plugin.getResource(resourcePath)) {
                    if (in != null) {
                        Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        plugin.getLogger().info("Created default: " + resourcePath);
                    } else {
                        plugin.getLogger().warning("Missing resource: " + resourcePath);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create " + resourcePath + ": " + e.getMessage());
            }
        }
    }

    public YamlConfiguration getDataLangConfig() {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "Lang/DataLang.yml"));
    }
}