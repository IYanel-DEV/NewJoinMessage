package com.joinleave.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class ModernDataImporter {
    private ModernDataImporter() {}

    public static int importIfNeeded(JavaPlugin plugin, FileConfiguration playersConfig, File playersFile) {
        ConfigurationSection existing = playersConfig.getConfigurationSection("players");
        if (existing != null && !existing.getKeys(false).isEmpty()) {
            return 0;
        }
        File modernDir = new File(plugin.getDataFolder().getParentFile(), "ModernJoinMessage");
        File modernData = new File(modernDir, "data.yml");
        if (!modernData.isFile()) {
            return 0;
        }
        FileConfiguration modern = YamlConfiguration.loadConfiguration(modernData);
        int count = 0;
        count += copySection(modern, playersConfig, "custom-join-messages", "join_message");
        count += copySection(modern, playersConfig, "custom-leave-messages", "leave_message");
        if (count > 0) {
            try {
                playersConfig.save(playersFile);
                plugin.getLogger().info("Imported " + count + " messages from ModernJoinMessage/data.yml");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save imported ModernJoinMessage data: " + e.getMessage());
            }
        }
        return count;
    }

    private static int copySection(FileConfiguration modern, FileConfiguration dest,
                                   String modernPath, String legacyField) {
        ConfigurationSection section = modern.getConfigurationSection(modernPath);
        if (section == null) {
            return 0;
        }
        int n = 0;
        for (String uuid : section.getKeys(false)) {
            String msg = section.getString(uuid);
            if (msg == null || msg.isEmpty()) {
                continue;
            }
            dest.set("players." + uuid + "." + legacyField, msg);
            n++;
        }
        return n;
    }
}
