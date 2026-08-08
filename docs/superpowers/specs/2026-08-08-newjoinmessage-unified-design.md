# NewJoinMessage — Unified 1.8→Latest Design

**Date:** 2026-08-08  
**Status:** Approved in conversation; awaiting final user review of this file  
**Spigot resource:** [110979](https://www.spigotmc.org/resources/newjoinmessage-custom-join-leave-messages-for-each-player-with-fully-customizable-configuration.110979/)  
**Sources:** [Legacy-JoinLeaveMessage](https://github.com/IYanel-DEV/Legacy-JoinLeaveMessage), [ModernJoinMessage](https://github.com/IYanel-DEV/ModernJoinMessage)

## Goal

Ship **one jar** named **NewJoinMessage** that runs on Minecraft **1.8 through latest** (Spigot/Paper/Purpur), with the full Legacy feature set plus Modern polish. Server owners must not download a separate “legacy” vs “modern” plugin.

## Decisions (locked)

| Topic | Choice |
|--------|--------|
| Feature scope | Full Legacy (MySQL, langs, fireworks, welcome, vanish, update check, GUI) + Modern hex/colors/GUI polish |
| Plugin name | `NewJoinMessage` |
| GitHub home | New repo `IYanel-DEV/NewJoinMessage` |
| Compat approach | Single codebase + runtime capability checks (not multi-module) |
| Data | Prefer `plugins/NewJoinMessage/`; if empty, one-time import from `plugins/ModernJoinMessage/` when present |
| Package | `com.joinleave` (Legacy continuity) |

## Architecture

One Maven project → one shaded jar.

| Piece | Responsibility |
|--------|----------------|
| `JoinleaveMessage` | Bootstrap only: config/data load, register commands/listeners; never let exceptions escape `onEnable` uncaught |
| Managers | Config, player data (YAML + optional MySQL), messages/lang, permissions |
| Listeners | Join/quit, GUI, welcome, vanish-aware hooks |
| Utils | `ColorUtils` (`&` always; hex only if `ChatColor.of` exists), `ServerCompat` (version/capability flags) |
| Commands | `/njm` (+ aliases) matching Spigot resource surface |

Legacy + Modern GitHub repos remain as archives; READMEs point to `NewJoinMessage`.

## Features (in scope)

- Per-player join/leave messages: set, clear, setplayer, reload, gui, help
- Config prefixes/defaults; player-name / join-text / leave-text colors (`&` + hex when available)
- YAML `data.yml`; optional MySQL with YAML fallback if DB unavailable
- Multi-language (`Lang/`), welcome message, fireworks, vanish soft-hook, bStats, update check (resource id **110979**)
- Admin + player GUI with chat-input flow

## Version compatibility

| Servers | Behavior |
|---------|----------|
| 1.8–1.15 | `&` colors only; hex not applied via `ChatColor.of`; GUI/materials use safe legacy IDs |
| 1.16+ | Hex via `ChatColor.of` when the method exists |
| 1.21+ | Same code path — **do not** pin `api-version: '1.21'` |

**Build:** Java **8** bytecode; compile against Spigot API ~**1.12** (provided). **Omit** `api-version` from `plugin.yml` entirely so 1.8–1.12 loaders are not rejected (this is the root cause of the “API still in 1.21” review).

**Release version:** `5.0.0` (unified merge; Spigot listing can show the same).

## Crash-proofing (review complaints)

Addresses: “API still in 1.21”, “crashes the whole server”, “lots of issues”.

1. No hardcoded `api-version: '1.21'` in `plugin.yml`
2. `onEnable` / `onDisable`: catch, log, disable cleanly — no process kill
3. Join/quit: null-safe config strings; no NPE on missing keys
4. MySQL connect failure → disable MySQL for the session, use YAML, warn in console
5. GUI/chat: cancel-safe; invalid input does not throw
6. Update checker / bStats failures ignored (never block enable)
7. Hex: reflect or try/catch around `ChatColor.of` — no `NoSuchMethodError`

## Data flow

1. Enable → load `config.yml` / `data.yml` / langs (create defaults if missing)
2. If NewJoinMessage data empty and ModernJoinMessage folder has player data → import once, log result
3. Player join/quit → resolve custom message (MySQL if enabled and healthy, else YAML) → format with `ColorUtils` → `setJoinMessage` / `setQuitMessage`
4. `/njm` / GUI → mutate data → persist YAML or MySQL

## Out of scope (v1 merge)

- Discord bot / website work
- New features not already in Legacy or Modern
- Separate per-version jars or multi-module shade

## Build & verification

- Tooling: Maven `clean package` (no IDE required)
- Output: shaded jar under `target/`
- Checks: compile must succeed; one small assert-style self-check for color/version helpers
- Smoke (manual): enable on test server, `/njm`, join/leave text shows, reload does not break

## Success criteria

- One download works from 1.8 to latest
- Existing Legacy-style configs under `NewJoinMessage/` keep working
- Modern data can be imported once when upgrading from the modern-only jar
- Plugin cannot take down the server on enable or on a bad join/leave path
- Spigot resource can ship this jar as the single supported build
