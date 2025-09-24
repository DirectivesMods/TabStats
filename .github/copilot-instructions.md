# TabStats – AI Assistant Guide

TabStats is a ## Typical changes and where
- Add a game mode: new enum in `HypixelGames`, extend `HGameBase`, create util with colors/ratios, wire in `StatWorld.fetchStats()`.
- Rendering tweaks: `render/StatsTab.java` (respect layout and color helpers; maintain UUID filtering).
- Player flow/data: `playerapi/{WorldLoader,StatWorld,HPlayer}.java` (use shared executor, preserve cache semantics, respect UUID-based filtering).
- Performance changes: Review `Handler.asExecutor()` usage, UUID caching in `WorldLoader`, and memory cleanup patterns.

## Code patterns to preserve
- **Always use UUID version checking**: `uuid.version() == 4` for real players, `== 1` for nicks, filter out `== 2/3`
- **Maintain dual processing**: `fetchStats()` for v4 UUIDs, `checkNickStatus()` for v1 UUIDs
- **UUID caching**: Cache `getUniqueID()` results per tick to avoid performance issues
- **Thread safety**: All stat processing via `Handler.asExecutor()`, never block main thread
- **Memory cleanup**: Always call `removeFromStatAssembly()` and proper map cleanup in `removePlayer()`

If anything is ambiguous (API flow, tab layout, cache invalidation, UUID filtering), ask and we'll add concrete pointers to this guide.aft 1.8.9 Forge client-only mod that overlays Hypixel stats in the tab list. Be async, avoid UI stalls and log spam, and reuse existing patterns.

## Architecture in brief
- Entry: `src/main/java/tabstats/TabStats.java` (Forge preInit/init/postInit).
- Player loop: `playerapi/WorldLoader.java` scans players each tick with UUID-based filtering and triggers async processing.
- Data: `playerapi/HPlayer.java` holds per-player stats made of `Stat{Int,Double,String}`.
- Cache: `playerapi/StatWorld.java` → `ConcurrentHashMap<UUID,HPlayer>`; 5s presence check pre-fetch; refresh on config changes.
- API: `playerapi/api/HypixelAPI.java` with custom exceptions (`InvalidKeyException`, `PlayerNullException`, `GameNullException`, `BadJsonException`).
- Threads: `util/Handler.asExecutor()` - **Fixed thread pool (16 daemon threads)** (pool name `TabStats-%d`); follow `setStatsAsync()`; never block tick/render.
- Render: `render/StatsTab.java` composes rows/columns and colors via util helpers and `ChatColor`; filters NPCs at render time.
- Game modes: `HGameBase` → `{Bedwars|Duels|Skywars}Util` → `{Bedwars|Duels|Skywars}` implementations; enums define API fields; defensive ratio math.
- Skywars: large Unicode emblem mapping via `EMBLEM_MAP` in util.
- Config: `config/ModConfig.java` stores key at `.minecraft/tabstats/apikey.json` and reloads each `getApiKey()`.

## UUID-based filtering (CRITICAL - v1.2.0 optimization)
- **Version 4 UUIDs**: Real players → Display + fetch stats via API calls
- **Version 1 UUIDs**: Potentially nicked players → Display + nick detection only (NO API calls)
- **Version 2/3 UUIDs**: NPCs → Filtered out completely (never displayed or processed)
- UUID caching per tick prevents excessive `getUniqueID()` calls (87% performance improvement)
- Implementation: `WorldLoader.loadOrRender()` + `WorldLoader.checkNickStatus()` for v1 UUIDs

## Nick detection (don't break this)
- A player is nicked only if BOTH: UUID version is 1 AND skin hash is in known nick skins.
- **Two flows**: 
  - Real players (v4): `StatWorld.fetchStats()` decodes GameProfile textures → API call + nick detection
  - Nicked players (v1): `WorldLoader.checkNickStatus()` → skin hash check only (no API call)
- `HPlayer.isNicked()` returns the precomputed flag; renderer prepends `[NICKED]`. No de-nicking attempts.
- `util/NickDetector.isPlayerNicked(uuid, skinHash)` handles dual-condition checking.

## Performance optimizations (v1.2.0)
- **Thread pool**: Fixed 16-thread pool instead of unbounded CachedThreadPool (prevents memory leaks)
- **UUID caching**: Per-tick UUID extraction cached to avoid 19,200 calls/sec → ~300 calls/sec
- **NPC filtering**: UUID-based filtering eliminates API spam for NPCs (version 2/3 UUIDs)
- **Memory management**: Proper cleanup in `removePlayer()` and tracking map management

## Conventions to follow
- Silent failure: catch + return defaults in config I/O, API, and JSON; avoid log spam.
- Retry/backoff: exponential backoff on API errors; nick uncertainty retried on tick cadence.
- Color thresholds: keep hardcoded tiers in util (e.g., `getFkdrColor()`, `getKdrColor()`, `getStarWithColor()`).
- Build system: Essential Loom (Forge 11.15.1.2318), Java 8; Blossom replaces `@VERSION@`, `@NAME@`.

## Build & run
- Build: `./gradlew build` → JARs in `build/libs/`.
- Dev client: `./gradlew runClient`.

## Typical changes and where
- Add a game mode: new enum in `HypixelGames`, extend `HGameBase`, create util with colors/ratios, wire in `StatWorld.fetchStats()`.
- Rendering tweaks: `render/StatsTab.java` (respect layout and color helpers).
- Player flow/data: `playerapi/{WorldLoader,StatWorld,HPlayer}.java` (use shared executor, preserve cache semantics).

If anything is ambiguous (API flow, tab layout, cache invalidation), ask and we’ll add concrete pointers to this guide.