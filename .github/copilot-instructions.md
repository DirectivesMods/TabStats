# TabStats – AI Assistant Guide

TabStats is a Forge 1.8.9 client-only mod that overlays Hypixel stats inside the in-game tab list. Keep UI work on the render thread minimal, push everything else through the shared executor, and avoid noisy logging.

## Build & Run
- Build: `./gradlew build` → JARs land in `build/libs/`
- Dev client: `./gradlew runClient`

## Architecture in Brief
- Entry: `src/main/java/tabstats/TabStats.java` wires Forge lifecycle and owns singletons (config, `StatWorld`, overlay listener).
- Overlay: `listener/GameOverlayListener` installs `render/StatsTab.java` as the custom tab overlay and exposes scroll helpers.
- Player discovery: `playerapi/WorldLoader.java` scans `mc.theWorld.playerEntities` each tick, filters invalid entries, and queues async work.
- Cache & stats: `playerapi/StatWorld.java` owns the `ConcurrentHashMap<UUID, HPlayer>`, retry bookkeeping, and all Hypixel API interactions.
- Data model: `playerapi/HPlayer.java` + `playerapi/api/stats/*` wrap numerical/string stats per game mode.
- Utilities: `util/Handler` (fixed 16-thread daemon pool `TabStats-%d`), `util/ChatColor`, `util/NickDetector`, ratio/color helpers inside each game util.

## Player Lifecycle & UUID Rules
- `WorldLoader.loadOrRender(EntityPlayer)` rejects players with null UUIDs, invalid names, `[NPC]` prefixes, or UUID version 3 (holograms/NPCs).
- Accepted UUID versions:
  - **v4**: Real players → queue `fetchStats()` (API + nick detection).
  - **v2**: Hypixel lobby/replay inserts → also queue `fetchStats()` once. If the API returns `PlayerNullException`, treat as a lobby bot and do not keep retrying.
  - **v1**: Potentially nicked players → skip API calls; run `checkNickStatus()` (skin-hash nick detection with up to 200 tick retries while the skin loads).
- `WorldLoader` uses `statAssembly` to ensure only one outstanding task per player and calls `checkCacheSize()` to evict players once cache exceeds ~500 entries.
- On world changes or teardown (`onDelete`) it clears caches and calls `StatsTab.resetScroll()` so the overlay snaps back to the top.

## Stat Fetching & Nick Detection
- `StatWorld.fetchStatsWithRetry()` always runs inside `Handler.asExecutor()`.
- A single pass constructs `HPlayer`, registers aliases for raw usernames and formatted scoreboard names, and fires:
  - Hypixel API call (`HypixelAPI#getWholeObject`) to populate `Bedwars`, `Duels`, and `Skywars` objects.
  - Nick detection (`NickDetector.isPlayerNicked(uuid, skinHash)`).
- Success cases:
  - API success → player is real; cache stats, mark `nicked=false`.
  - Nick detection certainty with no API data → cache player with `nicked=true`.
- Failure handling:
  - `InvalidKeyException` aborts immediately (no retries).
  - v2 UUID + `PlayerNullException` → treat as lobby insert, drop aliases, leave unseen in the overlay.
  - Potential nick (v1 UUID + nick signature without skin yet) → rely on `nickRetryTicks` (≤200) and retry `checkNickStatus()` only.
  - Real UUID (v4) with transient API errors → exponential backoff (0ms, 250ms, 500ms … up to 8 attempts). After the cap, cache name-only data with `nicked=false`.
- `removePlayer()` and `clearPlayers()` always purge aliases, retry counters, and timers to prevent memory leaks.

## Tab Rendering Essentials (`render/StatsTab.java`)
- Player list derives from `NetHandlerPlayClient#getPlayerInfoMap()` sorted by team/game type. Filtering removes:
  - UUID versions not in {1,2,4}.
  - v2 players without a cached `HPlayer` entry.
  - Invalid usernames (regex `^[A-Za-z0-9_]{3,16}$`) and any name prefixed with `[NPC]`.
- Smooth scrolling:
  - Up to 80 entries rendered; `calculateMaxVisiblePlayers()` caps by screen height.
  - `handleMouseWheel()` shifts the target index by 1 per wheel notch with clamped bounds.
  - `updateScrollAnimation()` interpolates toward the target and the renderer uses scissor testing to clip rows above the header.
  - Arrow hints (`▲/▼`) show when additional players exist above or below the visible window.
- Headers take uppercase stat labels and align value columns via matching width calculations (`ChatColor.BOLD + label`).
- When stats are missing or the requested gamemode is absent, the renderer falls back to `BEDWARS` formatted stats.
- Name rendering respects rank prefixes from teams, preserves gray `[NON]` color, and forces `[NICKED]` label when `HPlayer.isNicked()` is true. Obfuscated names are restored using cached data.

## Conventions & Gotchas
- All async work must run through `Handler.asExecutor()`; never block the render/tick threads.
- Maintain silent failure where existing code does (config reads, API parsing). Prefer returning defaults over chat spam.
- Respect caching helpers: reuse `registerAlias`, `removeAliases`, and cache lookups before firing new API calls.
- When adding stats, keep color thresholds and ratio math in the respective game util classes.
- Reuse `ChatColor` helpers instead of hardcoding formatting codes.
- Ensure UI math stays integer-safe; `entryHeight` is 12px and spacing uses `entryHeight + 1` consistently.

## Typical Changes & Where to Look
- Add a game mode: extend `HypixelGames`, implement util/ratio helpers, wire into `HPlayer.addGames()` + `StatWorld.fetchStats()` serialization.
- Rendering tweak: `render/StatsTab.java` (honor scroll math, stat alignment, UUID filtering, and arrow indicators).
- Player flow/data changes: `playerapi/{WorldLoader,StatWorld,HPlayer}.java` (respect executor usage, retry limits, cache semantics, alias handling).
- Performance changes: audit `Handler.asExecutor()` usage, ensure UUID caching isn’t regressed, keep memory cleanup cases (`removePlayer`, nick retry maps).

## When in Doubt
- Unsure about API flow, tab layout, cache invalidation, or UUID filtering? Ask and we’ll extend this guide.
