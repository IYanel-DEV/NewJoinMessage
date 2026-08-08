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
