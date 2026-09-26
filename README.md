# NewJoinMessage

One custom join/leave-message plugin for **Minecraft 1.8 through 26.2** on Spigot, Paper, and compatible forks.

Spigot: https://www.spigotmc.org/resources/110979/

## Features

- Per-player join/leave messages, legacy `&` colors, and hex colors on servers that support them
- One 1.8-safe player/admin GUI, with personal icons and version-aware join/leave sounds
- YAML storage, optional MySQL fallback, languages, first-join welcome/fireworks, vanish suppression, metrics, and updates
- Preview and personal broadcast toggle
- `%player%`, `%displayname%`, `%world%`, `%online%`, `%player_count%`, `%max_players%`, `%server_name%`, `%motd%`, `%server_version%`, `%time%`, `%prefix%`/`%vault_prefix%`, and `PLAYERNAME` placeholders
- **11 popular plugin integrations**: PlaceholderAPI, LuckPerms, EssentialsX, Citizens, ProtocolLib, WorldGuard, Multiverse-Core, DiscordSRV, HolographicDisplays, MiniMessage (Adventure), MVdWPlaceholderAPI
- Random default join/leave pools and per-world broadcast suppression
- Message length cap and a change cooldown to stop chat spam
- `/njm list` to page through everyone with a custom message

## Commands

- `/njm` or `/njm gui` → open the player/admin GUI
- `/njm set <join|leave> <message>` → set your message
- `/njm clear <all|join|leave>` → clear your message
- `/njm preview` → preview both messages
- `/njm toggle` → enable or disable your broadcasts
- `/njm icon <icon|off>` → choose an icon
- `/njm sound <join|leave> <sound|off>` → choose a sound available on the current server
- `/njm list [page]` → list players with custom messages
- `/njm setplayer`, `/njm info`, `/njm reload`, `/njm language` → administration and language tools

## Build

Requires JDK 8+ and Maven:

```bash
mvn -q clean package
```

Jar: `target/NewJoinMessage-6.0.0.jar`

The jar targets Java 8 bytecode and intentionally omits `api-version`; declaring a modern API version would make 1.8-1.12 reject the same jar. Minecraft 26.2 servers still require the server's supported Java version (Java 25 for Paper 26.2).

## Install

1. Drop the jar into `plugins/`.
2. Restart the server.
3. Use `/njm` (aliases: `/mjm`, `/modernjoinmessage`, `/newjoinmessage`).

## Plugin Integrations

All integrations are **soft dependencies** — they work if the plugin is installed, and gracefully do nothing if not.

| Plugin | Placeholders / Features |
|--------|------------------------|
| **PlaceholderAPI** | All PAPI placeholders (`%player_health%`, `%server_tps%`, `%vault_eco_balance%`, etc.) |
| **LuckPerms** | `%luckperms_prefix%`, `%luckperms_suffix%`, `%luckperms_group%` |
| **EssentialsX** | `%essentials_nickname%`, `%essentials_displayname%`, `%essentials_vanished%`, `%essentials_afk%` |
| **Citizens** | `%citizens_is_npc%`, `%citizens_npc_name%` |
| **ProtocolLib** | Advanced packet manipulation for custom join/leave packets |
| **WorldGuard** | `%worldguard_regions%`, `%worldguard_region%` — disable/force messages in specific regions |
| **Multiverse-Core** | `%multiverse_world%`, `%multiverse_world_alias%`, `%multiverse_world_colored%` |
| **DiscordSRV** | `%discordsrv_verified%` — send join/leave messages to Discord |
| **HolographicDisplays** | Temporary holograms at player location on join/leave |
| **MiniMessage (Adventure)** | Full MiniMessage syntax: `<red>text</red>`, `<gradient:blue:green>text</gradient>`, `<rainbow>text</rainbow>` |
| **MVdWPlaceholderAPI** | All MVdW placeholders for broader compatibility |

### Configuration

All integrations are auto-detected. Optional config sections in `config.yml`:

```yaml
discordsrv:
  enabled: false
  join-message: "🟢 %player% joined the server"
  leave-message: "🔴 %player% left the server"

holographicdisplays:
  enabled: false
  join-duration-seconds: 3
  leave-duration-seconds: 3
  height-offset: 2.5

protocollib:
  enabled: false
  use-packets-for-messages: false

worldguard:
  enabled: false
  disabled-regions: []
  forced-regions: []
```

Enable the sections you want — the rest are no-ops if the plugin isn't installed.

## Migrating from ModernJoinMessage

If `plugins/NewJoinMessage/data.yml` has no players yet and `plugins/ModernJoinMessage/data.yml` exists, messages are imported once on enable.

## Legacy repos

- https://github.com/IYanel-DEV/Legacy-JoinLeaveMessage → superseded
- https://github.com/IYanel-DEV/ModernJoinMessage → superseded
