package com.joinleave.util;

import com.joinleave.storage.StorageBackend;
import com.joinleave.storage.YamlBackend;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses every bundled YAML file and reads the shipped data.yml through the real
 * backend. A syntax error in config.yml or a language file otherwise only shows
 * up as a broken server at runtime, so it is much cheaper to catch it here.
 */
public final class YamlResourcesSelfCheck {

    private static final String CONFIG_KEYS[] = {
            "about", "join-prefix", "leave-prefix", "default-join-message", "default-leave-message",
            "update-check", "mysql.enabled", "mysql.table", "sounds.enabled", "disabled-worlds",
            "allow-all-players", "use-modern-format", "default-icon", "welcome-message-enabled",
            "max-message-length", "change-cooldown-seconds", "server-name", "list-page-size",
    };

    /** english.yml is the reference: every other language must define the same keys. */
    private static final String REFERENCE_LANG = "Lang/english.yml";

    /**
     * Lang/DataLang.yml only stores player language preferences, so it is legitimately
     * empty; the per-language message files must carry translations.
     */
    private static final String LANG_RESOURCES[] = {
            "Lang/english.yml", "Lang/spanish.yml", "Lang/french.yml", "Lang/germany.yml",
            "Lang/italian.yml", "Lang/russian.yml", "Lang/chinese.yml", "Lang/japanese.yml",
            "Lang/korean.yml",
    };

    private static final String MAY_BE_EMPTY[] = {"Lang/DataLang.yml"};

    private YamlResourcesSelfCheck() {
    }

    public static void main(String[] args) throws Exception {
        List<String> failures = new java.util.ArrayList<String>();

        checkConfig(failures);
        checkDefaultDataFile(failures);
        checkLangs(failures);

        if (!failures.isEmpty()) {
            for (String failure : failures) {
                System.err.println("  " + failure);
            }
            throw new AssertionError(failures.size() + " YAML problem(s)");
        }
        System.out.println("YamlResourcesSelfCheck OK");
    }

    private static void checkConfig(List<String> failures) {
        YamlConfiguration yaml = parse(failures, "config.yml");
        if (yaml == null) {
            return;
        }
        for (String key : CONFIG_KEYS) {
            if (!yaml.contains(key)) {
                failures.add("config.yml is missing required key '" + key + "'");
            }
        }
        // The banner is a list, so a stray mapping key would silently drop entries.
        List<String> about = yaml.getStringList("about");
        if (about.isEmpty()) {
            failures.add("config.yml 'about' is not a readable list");
        } else if (!about.get(0).contains("NewJoinMessage")) {
            failures.add("config.yml 'about' first entry looks wrong: " + about.get(0));
        }
    }

    /** Reads the shipped data.yml through the real backend, exactly as enable does. */
    private static void checkDefaultDataFile(List<String> failures) throws Exception {
        File tmp = File.createTempFile("njm-default-data", ".yml");
        try {
            InputStream in = YamlResourcesSelfCheck.class.getClassLoader().getResourceAsStream("data.yml");
            if (in == null) {
                failures.add("data.yml is missing from the jar");
                return;
            }
            try {
                OutputStream out = new FileOutputStream(tmp);
                try {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }

            Logger logger = Logger.getLogger("yaml-check");
            logger.setLevel(Level.OFF);
            StorageBackend backend = new YamlBackend(tmp, logger);
            Map<UUID, Map<String, String>> loaded = backend.loadAll();
            if (loaded == null) {
                failures.add("YamlBackend.loadAll() returned null for the default data.yml");
            } else if (!loaded.isEmpty()) {
                failures.add("default data.yml should hold no players but had " + loaded.size());
            }
            backend.persist(java.util.Collections.<UUID>emptySet(),
                    java.util.Collections.<UUID, Map<String, String>>emptyMap());
            backend.close();
        } catch (Throwable t) {
            failures.add("YamlBackend could not read the shipped data.yml: " + t);
        } finally {
            tmp.delete();
        }
    }

    private static void checkLangs(List<String> failures) {
        int checked = 0;
        java.util.Set<String> referenceKeys = null;
        String referenceName = null;

        for (String resource : LANG_RESOURCES) {
            InputStream in = YamlResourcesSelfCheck.class.getClassLoader().getResourceAsStream(resource);
            if (in == null) {
                failures.add(resource + " is missing from the jar");
                continue;
            }
            YamlConfiguration yaml;
            try {
                yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, "UTF-8"));
            } catch (Throwable t) {
                failures.add(resource + " failed to parse: " + t.getMessage());
                close(in);
                continue;
            }
            close(in);

            if (yaml.getKeys(false).isEmpty()) {
                failures.add(resource + " parsed but contains no keys");
                continue;
            }
            java.util.Set<String> keys = new java.util.TreeSet<String>(yaml.getKeys(false));

            if (REFERENCE_LANG.equals(resource)) {
                referenceKeys = keys;
                referenceName = resource;
            } else if (referenceKeys != null) {
                // A key present in english but missing here would show up in game as a
                // raw "[message_too_long]" placeholder string.
                for (String key : referenceKeys) {
                    if (!keys.contains(key)) {
                        failures.add(resource + " is missing key '" + key + "' present in " + referenceName);
                    }
                }
                for (String key : keys) {
                    if (!referenceKeys.contains(key)) {
                        failures.add(resource + " has key '" + key + "' that " + referenceName
                                + " does not define (it can never be shown)");
                    }
                }
            }
            checked++;
        }

        for (String resource : MAY_BE_EMPTY) {
            if (parse(failures, resource) == null) {
                continue;
            }
            checked++;
        }
        if (checked == 0) {
            failures.add("no Lang/*.yml resources were found (looked for "
                    + Arrays.toString(LANG_RESOURCES) + ")");
        }
    }

    private static void close(InputStream in) {
        try {
            in.close();
        } catch (Exception ignored) {
            // nothing useful to do here
        }
    }

    private static YamlConfiguration parse(List<String> failures, String resource) {
        InputStream in = YamlResourcesSelfCheck.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            failures.add(resource + " is missing from the jar");
            return null;
        }
        try {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, "UTF-8"));
        } catch (Throwable t) {
            failures.add(resource + " is not valid YAML: " + t.getMessage());
            return null;
        } finally {
            close(in);
        }
    }
}
