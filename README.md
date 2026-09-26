# NewJoinMessage

One custom join/leave-message plugin for **Minecraft 1.8 through 26.2** on Spigot, Paper, and compatible forks.

Spigot: https://www.spigotmc.org/resources/110979/

## Features

- Per-player join/leave messages, legacy `&` colors, and hex colors on servers that support them
- One 1.8-safe player/admin GUI, with personal icons and version-aware join/leave sounds
- YAML storage, optional MySQL fallback, languages, first-join welcome/fireworks, vanish suppression, metrics, and updates
- Preview and personal broadcast toggle
- `%player%`, `%displayname%`, `%world%`, `%online%`, `%max_players%`, and `PLAYERNAME` placeholders
- Random default join/leave pools and per-world broadcast suppression

## Commands

- `/njm` or `/njm gui` ? open the player/admin GUI
- `/njm set <join|leave> <message>` ? set your message
- `/njm clear <all|join|leave>` ? clear your message
- `/njm preview` ? preview both messages
- `/njm toggle` ? enable or disable your broadcasts
- `/njm icon <icon|off>` ? choose an icon
- `/njm sound <join|leave> <sound|off>` ? choose a sound available on the current server
- `/njm setplayer`, `/njm info`, `/njm reload`, `/njm language` ? administration and language tools

## Build

Requires JDK 8+ and Maven:

```bash
mvn -q clean package
```

Jar: `target/NewJoinMessage-5.1.1.jar`

The jar targets Java 8 bytecode and intentionally omits `api-version`; declaring a modern API version would make 1.8-1.12 reject the same jar. Minecraft 26.2 servers still require the server's supported Java version (Java 25 for Paper 26.2).

## Install

1. Drop the jar into `plugins/`.
2. Restart the server.
3. Use `/njm` (aliases: `/mjm`, `/modernjoinmessage`, `/newjoinmessage`).

## Migrating from ModernJoinMessage

If `plugins/NewJoinMessage/data.yml` has no players yet and `plugins/ModernJoinMessage/data.yml` exists, messages are imported once on enable.

## Legacy repos

- https://github.com/IYanel-DEV/Legacy-JoinLeaveMessage ? superseded
- https://github.com/IYanel-DEV/ModernJoinMessage ? superseded
