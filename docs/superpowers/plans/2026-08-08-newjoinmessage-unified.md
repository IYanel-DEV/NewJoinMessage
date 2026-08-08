# NewJoinMessage Unified 1.8→Latest Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce one Maven-built `NewJoinMessage` 5.0.0 jar (Java 8) that merges Legacy features with Modern color polish and never crashes the server from 1.8 through latest.

**Architecture:** Copy Legacy (`com.joinleave`) as the base; add `ServerCompat` + safe `ColorUtils`; harden `onEnable`/join/quit/MySQL; omit `api-version`; one-time import from `plugins/ModernJoinMessage/` when NewJoinMessage data is empty. Publish to new GitHub repo `IYanel-DEV/NewJoinMessage`.

**Tech Stack:** Java 8, Maven, Spigot API 1.12-R0.1-SNAPSHOT (provided), existing Legacy sources under `_explore/Legacy-JoinLeaveMessage`, Modern reference under `_explore/ModernJoinMessage`.

## Global Constraints

- Plugin name: `NewJoinMessage`; version `5.0.0`; main `com.joinleave.JoinleaveMessage`
- Compile/target Java **1.8**; no Java 21 APIs in source
- **Omit** `api-version` from `plugin.yml` entirely
- Full Legacy feature set kept (MySQL, langs, fireworks/welcome, vanish, update check 110979, GUI, `/njm`)
- Hex colors only when `net.md_5.bungee.api.ChatColor.of(String)` exists
- Exceptions in enable/join/quit/MySQL/update/metrics must be caught and logged — never kill the process
- Workspace root: `C:\Users\FUFU\Downloads\myminecraftplugin` becomes the project (not `_explore`)
- Do not commit unless the user explicitly asks (user rule); skip plan “Commit” steps or pause and ask
- Ponytail: no new frameworks; one small assert self-check for color/compat helpers only

---

## File map

| Path | Responsibility |
|------|----------------|
| `pom.xml` | Java 8, Spigot 1.12 API, shade, version 5.0.0 |
| `src/main/resources/plugin.yml` | Clean Bukkit metadata; no `api-version`; no fake `listeners`/`resources` keys |
| `src/main/resources/config.yml` | Legacy keys + Modern color keys (`player-name-color`, `join-text-color`, `leave-text-color`, `join-text`, `leave-text`) |
| `src/main/java/com/joinleave/JoinleaveMessage.java` | Safe bootstrap, data load, MySQL fallback, join/quit formatting |
| `src/main/java/com/joinleave/util/ServerCompat.java` | Hex capability probe |
| `src/main/java/com/joinleave/util/ColorUtils.java` | `&` + optional hex |
| `src/main/java/com/joinleave/util/ModernDataImporter.java` | One-time Modern folder import |
| Existing Legacy handlers/GUI/lang/Metrics/UpdateChecker/VanishAPI/PlayerWelcome | Keep; only touch if crash-risk |
| `src/test/java/com/joinleave/util/ColorUtilsSelfCheck.java` | Assert-based self-check (no JUnit framework) |
| `README.md` | Install + 1.8–latest + migration notes |
| `_explore/*` | Read-only reference clones |

---

### Task 1: GitHub repo + project scaffold from Legacy

**Files:**
- Create: GitHub `IYanel-DEV/NewJoinMessage`
- Create: all project files by copying Legacy into workspace root
- Create: `.gitignore` (from Legacy)
- Modify later tasks only after this copy lands

**Interfaces:**
- Consumes: `_explore/Legacy-JoinLeaveMessage/**`
- Produces: workspace root Maven tree with `com.joinleave` sources

- [ ] **Step 1: Create the GitHub repository**

```bash
gh auth switch --user IYanel-DEV
gh repo create IYanel-DEV/NewJoinMessage --public --description "NewJoinMessage - custom join/leave messages for Minecraft 1.8 through latest" --confirm
```

Expected: repo URL `https://github.com/IYanel-DEV/NewJoinMessage`

- [ ] **Step 2: Copy Legacy sources into workspace root (exclude `.git`)**

```bash
cd /d C:\Users\FUFU\Downloads\myminecraftplugin
xcopy /E /I /Y _explore\Legacy-JoinLeaveMessage\src src
copy /Y _explore\Legacy-JoinLeaveMessage\pom.xml pom.xml
copy /Y _explore\Legacy-JoinLeaveMessage\.gitignore .gitignore
```

Expected: `src\main\java\com\joinleave\JoinleaveMessage.java` exists at workspace root.

- [ ] **Step 3: Init git and set remote (no commit yet unless user asks)**

```bash
cd /d C:\Users\FUFU\Downloads\myminecraftplugin
git init
git remote add origin https://github.com/IYanel-DEV/NewJoinMessage.git
git status
```

Expected: untracked `src/`, `pom.xml`, `docs/`.

- [ ] **Step 4: Point agent workspace at project if needed**

Use `cursor-app-control` `move_agent_to_root` only if cwd is wrong; target `C:\Users\FUFU\Downloads\myminecraftplugin`.

---

### Task 2: pom.xml + plugin.yml baseline (fixes “API still 1.21”)

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/plugin.yml`
- Test: `mvn -q -DskipTests package` after Task 3+; this task only needs XML validity

**Interfaces:**
- Produces: artifact `NewJoinMessage` version `5.0.0`, Java 8, single Spigot dependency

- [ ] **Step 1: Replace `pom.xml` with this content**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.joinleave</groupId>
    <artifactId>NewJoinMessage</artifactId>
    <version>5.0.0</version>
    <packaging>jar</packaging>
    <name>NewJoinMessage</name>
    <description>Custom join/leave messages per player for Minecraft 1.8 through latest</description>
    <url>https://www.spigotmc.org/resources/110979/</url>

    <properties>
        <java.version>1.8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <build>
        <finalName>NewJoinMessage-${project.version}</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.4</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>com.joinleave.util.ColorUtilsSelfCheck</mainClass>
                    <classpathScope>test</classpathScope>
                </configuration>
            </plugin>
        </plugins>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>

    <repositories>
        <repository>
            <id>spigotmc-repo</id>
            <url>https://hub.spigotmc.org/nexus/content/repositories/snapshots/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>org.spigotmc</groupId>
            <artifactId>spigot-api</artifactId>
            <version>1.12-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Replace `src/main/resources/plugin.yml` with this content**

```yaml
name: NewJoinMessage
version: '${project.version}'
main: com.joinleave.JoinleaveMessage
authors: [Yanel, oled]
description: Custom join/leave messages per player for Minecraft 1.8 through latest
website: https://www.spigotmc.org/resources/110979/
softdepend: [SuperVanish, PremiumVanish, VanishNoPacket]

commands:
  njm:
    description: NewJoinMessage main command
    usage: /njm [help|set|clear|gui|reload|setplayer|info|language]
    aliases: [modernjoinmessage, mjm, newjoinmessage]

permissions:
  joinleave.set.join:
    description: Allows players to set their join message.
    default: op
  joinleave.set.leave:
    description: Allows players to set their leave message.
    default: op
  joinleave.reload:
    description: Allows players to reload the plugin.
    default: op
  joinleave.gui:
    description: Allows players to open the GUI.
    default: op
  joinleave.setplayer:
    description: Allows setting join/leave messages for another player.
    default: op
  joinleave.clearplayer:
    description: Allows clearing join/leave messages for another player.
    default: op
  joinleave.info:
    description: Allows viewing player information.
    default: op
  joinleave.update:
    description: Check for update.
    default: op
  modernjoinmessage.use:
    description: Alias permission for ModernJoinMessage users.
    default: true
  modernjoinmessage.reload:
    description: Alias reload permission for ModernJoinMessage users.
    default: op
```

Note: **no** `api-version` key. Remove invalid `listeners:` / `resources:` blocks from Legacy.

- [ ] **Step 3: Verify YAML has no api-version**

```bash
findstr /I "api-version" src\main\resources\plugin.yml
```

Expected: no matches (exit code 1 on Windows `findstr` when not found is OK).

---

### Task 3: ServerCompat + ColorUtils + self-check

**Files:**
- Create: `src/main/java/com/joinleave/util/ServerCompat.java`
- Create: `src/main/java/com/joinleave/util/ColorUtils.java`
- Create: `src/test/java/com/joinleave/util/ColorUtilsSelfCheck.java`

**Interfaces:**
- Produces:
  - `ServerCompat.supportsHex(): boolean`
  - `ServerCompat.setHexSupportedForTest(Boolean override)` (nullable; null = use probe)
  - `ColorUtils.colorize(String message): String`
- Consumes: optional `net.md_5.bungee.api.ChatColor` at runtime

- [ ] **Step 1: Write the self-check (fails until utils exist)**

Create `src/test/java/com/joinleave/util/ColorUtilsSelfCheck.java`:

```java
package com.joinleave.util;

public final class ColorUtilsSelfCheck {
    public static void main(String[] args) {
        ServerCompat.setHexSupportedForTest(Boolean.FALSE);
        String noHex = ColorUtils.colorize("#FF0000Hello &aWorld");
        if (noHex.contains("#FF0000")) {
            throw new AssertionError("hex should be stripped when unsupported, got: " + noHex);
        }
        if (!noHex.contains("World")) {
            throw new AssertionError("text lost: " + noHex);
        }

        ServerCompat.setHexSupportedForTest(Boolean.TRUE);
        // Without ChatColor.of on classpath in unit run, of() may fail — colorize must not throw
        String safe = ColorUtils.colorize("#00FF00Hi &bThere");
        if (safe == null || !safe.contains("There")) {
            throw new AssertionError("colorize failed open: " + safe);
        }

        ServerCompat.setHexSupportedForTest(null);
        if (ColorUtils.colorize(null) == null || !ColorUtils.colorize(null).isEmpty()) {
            // colorize(null) must return ""
            if (!"".equals(ColorUtils.colorize(null))) {
                throw new AssertionError("null must become empty string");
            }
        }

        System.out.println("ColorUtilsSelfCheck OK");
    }
}
```

- [ ] **Step 2: Implement `ServerCompat.java`**

```java
package com.joinleave.util;

public final class ServerCompat {
    private static Boolean hexOverride = null;
    private static Boolean hexCached = null;

    private ServerCompat() {}

    public static void setHexSupportedForTest(Boolean override) {
        hexOverride = override;
    }

    public static boolean supportsHex() {
        if (hexOverride != null) {
            return hexOverride.booleanValue();
        }
        if (hexCached != null) {
            return hexCached.booleanValue();
        }
        try {
            net.md_5.bungee.api.ChatColor.class.getMethod("of", String.class);
            hexCached = Boolean.TRUE;
        } catch (Throwable t) {
            hexCached = Boolean.FALSE;
        }
        return hexCached.booleanValue();
    }
}
```

- [ ] **Step 3: Implement `ColorUtils.java`**

```java
package com.joinleave.util;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    private ColorUtils() {}

    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        if (ServerCompat.supportsHex()) {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String hex = matcher.group();
                String replacement;
                try {
                    replacement = ChatColor.of(hex).toString();
                } catch (Throwable t) {
                    replacement = "";
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            message = sb.toString();
        } else {
            message = HEX_PATTERN.matcher(message).replaceAll("");
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
```

- [ ] **Step 4: Compile main+test and run self-check**

```bash
mvn -q test-compile exec:java -Dexec.mainClass=com.joinleave.util.ColorUtilsSelfCheck -Dexec.classpathScope=test
```

Expected: console prints `ColorUtilsSelfCheck OK` and exit code 0.

---

### Task 4: Harden bootstrap (`JoinleaveMessage.onEnable` / MySQL)

**Files:**
- Modify: `src/main/java/com/joinleave/JoinleaveMessage.java`

**Interfaces:**
- Consumes: `ModernDataImporter` (Task 5), `ColorUtils` (Task 3)
- Produces: plugin enables even if MySQL/update/metrics fail

- [ ] **Step 1: Wrap entire `onEnable` body in try/catch**

At the start of `onEnable`, keep `instance = this;` then wrap the rest:

```java
@Override
public void onEnable() {
    instance = this;
    try {
        enablePlugin();
    } catch (Throwable t) {
        getLogger().severe("NewJoinMessage failed to enable safely: " + t.getMessage());
        t.printStackTrace();
        getServer().getPluginManager().disablePlugin(this);
    }
}

private void enablePlugin() {
    // move existing onEnable body here
}
```

- [ ] **Step 2: Fix duplicate `/njm` registration**

In `enablePlugin`, register the command **once**:

```java
JoinleaveCommand joinLeaveCommand = new JoinleaveCommand(this);
getCommand("njm").setExecutor(joinLeaveCommand);
getCommand("njm").setTabCompleter(joinLeaveCommand);
```

Delete the second `getCommand("njm").setExecutor(...)` call.

- [ ] **Step 3: MySQL failure must not leave a half-enabled SQL mode**

Replace MySQL setup block with:

```java
mysqlEnabled = getConfig().getBoolean("mysql.enabled");
if (mysqlEnabled) {
    if (setupMySQL()) {
        createTableIfNotExists();
    } else {
        mysqlEnabled = false;
        connection = null;
        getLogger().severe("MySQL failed — falling back to data.yml for this session.");
    }
}
```

- [ ] **Step 4: Guard `getMessage` / `setMessage` when `connection == null`**

At the top of both methods, if `mysqlEnabled && connection == null`, treat as file mode (`mysqlEnabled` effectively false for that call):

```java
boolean useMysql = mysqlEnabled && connection != null;
if (useMysql) {
    // existing SQL branch
} else {
    // existing YAML branch
}
```

- [ ] **Step 5: Soft-fail Metrics and UpdateChecker**

Wrap Metrics construction and UpdateChecker block each in their own try/catch that logs a warning and continues.

- [ ] **Step 6: Null-safe join/quit config reads**

In `handleJoin` / `handleLeave`, replace raw `getConfig().getString(...)` usage with defaults:

```java
private String cfg(String path, String def) {
    String v = getConfig().getString(path);
    return v == null ? def : v;
}
```

Use `cfg("join-prefix", "&d[&a+&d]")`, `cfg("default-join-prefix", "&7PLAYERNAME &5has joined")`, `cfg("default-join-message", "&dWelcome!")` (and leave equivalents). Format final strings with `ColorUtils.colorize(...)` instead of only `ChatColor.translateAlternateColorCodes`.

Example join assembly:

```java
String joinPrefix = ColorUtils.colorize(cfg("join-prefix", "&d[&a+&d]"));
String defaultJoinPrefix = ColorUtils.colorize(
    cfg("default-join-prefix", "&7PLAYERNAME &5has joined").replace("PLAYERNAME", playerName));
String joinEditable = getMessage(player, "join", "default-join-message");
String joinMessage = joinPrefix + " " + defaultJoinPrefix + ChatColor.GRAY + " - "
    + ColorUtils.colorize(parsePlaceholders(joinEditable, player));
event.setJoinMessage(joinMessage);
```

Extract placeholder replace (no color) into `parsePlaceholders` so colorize runs once on the editable part.

Wrap `handleJoin` / `handleLeave` bodies in try/catch; on failure log and set message to a plain fallback `"player joined"` / `"player left"` so the event still completes.

---

### Task 5: Modern data importer

**Files:**
- Create: `src/main/java/com/joinleave/util/ModernDataImporter.java`
- Modify: `src/main/java/com/joinleave/JoinleaveMessage.java` (`enablePlugin` after `playersConfig` load)

**Interfaces:**
- Produces: `ModernDataImporter.importIfNeeded(JavaPlugin plugin, FileConfiguration playersConfig, File playersFile): int` (messages imported count)
- Modern format keys: `custom-join-messages.<uuid>`, `custom-leave-messages.<uuid>`
- Legacy format keys: `players.<uuid>.join_message`, `players.<uuid>.leave_message`

- [ ] **Step 1: Implement importer**

```java
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
```

- [ ] **Step 2: Call after `playersConfig` load in `enablePlugin`**

```java
ModernDataImporter.importIfNeeded(this, playersConfig, playersFile);
```

- [ ] **Step 3: Manual sanity (no server required)**

Create a temp mental check: empty `players` + sample modern YAML in a unit-less review; importer returns >0 only when legacy empty and modern file exists. Optional: run a tiny main later if regressions appear.

---

### Task 6: Config merge (Modern color keys)

**Files:**
- Modify: `src/main/resources/config.yml`

**Interfaces:**
- Produces: config keys readable by join formatter (Task 4) and optional future GUI preview

- [ ] **Step 1: Append Modern-compatible keys to `config.yml` without removing Legacy keys**

Add at end of `src/main/resources/config.yml`:

```yaml
# Modern-style appearance (used when present; & codes or #RRGGBB)
join-text: "joined the server"
leave-text: "left the server"
player-name-color: "&a"
join-text-color: "&f"
leave-text-color: "&f"
```

Keep existing `join-prefix`, `default-join-prefix`, `default-join-message`, MySQL, welcome blocks unchanged.

- [ ] **Step 2: Optionally blend Modern format in join/leave when `use-modern-format: true`**

Add:

```yaml
use-modern-format: false
```

When `true`, build message like Modern:  
`colorize(player-name-color) + name + " " + colorize(join-text-color) + join-text + " - " + colorize(customOrDefault)`.  
When `false` (default), keep Legacy prefix format so existing servers look unchanged.

Implement the branch inside `handleJoin` / `handleLeave` using `getConfig().getBoolean("use-modern-format", false)`.

---

### Task 7: Crash-proof PlayerWelcome fireworks + dead references

**Files:**
- Modify: `src/main/java/com/joinleave/PlayerWelcome.java`
- Modify: `src/main/resources/plugin.yml` (already cleaned in Task 2 — confirm no `FireworkPlayerJoinListener`)

**Interfaces:**
- Consumes: Bukkit Firework API (present since early versions)

- [ ] **Step 1: Wrap `onPlayerJoin` firework/welcome logic in try/catch**

```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    try {
        // existing body
    } catch (Throwable t) {
        plugin.getLogger().warning("Welcome/firework failed for "
            + event.getPlayer().getName() + ": " + t.getMessage());
    }
}
```

- [ ] **Step 2: Guard world/location null before `launchFirework`**

```java
if (location == null || location.getWorld() == null) {
    return;
}
```

- [ ] **Step 3: Confirm plugin.yml does not reference missing `FireworkPlayerJoinListener`**

```bash
findstr /I "FireworkPlayerJoinListener" src\main\resources\plugin.yml
```

Expected: no matches.

---

### Task 8: README + archive notes on old repos

**Files:**
- Create/Replace: `README.md`
- Modify (via `gh`): Legacy + Modern repo README top notice (or create `MIGRATED.md`)

- [ ] **Step 1: Write root `README.md`**

```markdown
# NewJoinMessage

Custom join/leave messages per player for **Minecraft 1.8 → latest** (one jar).

Spigot: https://www.spigotmc.org/resources/110979/

## Build

Requires JDK 8+ and Maven:

```bash
mvn -q clean package
```

Jar: `target/NewJoinMessage-5.0.0.jar`

## Install

1. Drop the jar into `plugins/`
2. Restart
3. Use `/njm` (aliases: `/mjm`, `/modernjoinmessage`)

## Migrating from ModernJoinMessage

If `plugins/NewJoinMessage/data.yml` has no players yet, and `plugins/ModernJoinMessage/data.yml` exists, messages are imported once on enable.

## Legacy repos

- https://github.com/IYanel-DEV/Legacy-JoinLeaveMessage — superseded
- https://github.com/IYanel-DEV/ModernJoinMessage — superseded
```

- [ ] **Step 2: Add supersede notice to old repos**

```bash
gh api repos/IYanel-DEV/Legacy-JoinLeaveMessage/contents/README.md --jq .sha
# Then commit a short notice at top via web or gh, pointing to NewJoinMessage — only if user wants remote edits
```

Prefer: open PRs or direct commits on Legacy/Modern READMEs **only after user confirms** remote edits.

---

### Task 9: Full Maven package + verify jar

**Files:**
- Output: `target/NewJoinMessage-5.0.0.jar`

- [ ] **Step 1: Run self-check**

```bash
mvn -q test-compile exec:java -Dexec.mainClass=com.joinleave.util.ColorUtilsSelfCheck -Dexec.classpathScope=test
```

Expected: `ColorUtilsSelfCheck OK`

- [ ] **Step 2: Package**

```bash
mvn -q clean package
```

Expected: `BUILD SUCCESS`, jar at `target\NewJoinMessage-5.0.0.jar`

- [ ] **Step 3: Confirm plugin.yml inside jar has no api-version**

```bash
jar xf target\NewJoinMessage-5.0.0.jar plugin.yml
findstr /I "api-version" plugin.yml
del plugin.yml
```

Expected: no `api-version` line.

- [ ] **Step 4: Smoke checklist for user on a real server**

1. Fresh 1.8.8 or 1.12 server: jar loads, `/njm` works, join message shows  
2. 1.20+/1.21 server: jar loads without api-version errors, hex in config works if set  
3. Break MySQL credentials with `mysql.enabled: true` → plugin still enables on YAML  
4. Drop old Modern `data.yml` beside empty NewJoinMessage → import log line appears  

---

## Spec coverage checklist

| Spec requirement | Task |
|------------------|------|
| One jar 1.8→latest | 2, 3, 9 |
| Full Legacy features | 1 (copy), 4, 7 |
| Modern hex/colors | 3, 4, 6 |
| New repo NewJoinMessage | 1 |
| Omit api-version / fix 1.21 review | 2, 9 |
| Crash-proof enable/join/MySQL | 4, 7 |
| Modern data import | 5 |
| Java 8 build via Maven | 2, 9 |
| Update check 110979 | kept in Legacy copy (Task 1) + soft-fail Task 4 |
| README / supersede old repos | 8 |

## Plan self-review notes

- No TBD placeholders left in tasks  
- `ColorUtils.colorize` / `ServerCompat.supportsHex` names consistent across Task 3–4  
- Commit steps omitted from execution by default (user rule); ask before `git commit` / `git push`
