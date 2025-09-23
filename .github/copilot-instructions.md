# TabStats Copilot Instructions

## Project Overview
TabStats is a Minecraft 1.8.9 Forge mod that displays real-time Hypixel player statistics directly in the tab list. It integrates with the Hypixel API to show stats for Bedwars, Duels, and Skywars game modes with color-coded performance indicators.

## Architecture & Key Components

### Core Components
- **`TabStats.java`** - Main mod entry point with Forge lifecycle handlers (`preInit`, `init`, `postInit`)
- **`WorldLoader.java`** - Manages player detection and async stat fetching every tick
- **`StatsTab.java`** - Custom tab overlay renderer that displays stats in the player list
- **`ModConfig.java`** - Handles API key storage in `.minecraft/tabstats/apikey.json`
- **`HypixelAPI.java`** - HTTP client for Hypixel API requests with custom exceptions

### Game-Specific Implementation
- **Game classes inherit**: `HGameBase` → `{Bedwars|Duels|Skywars}Util` → `{Bedwars|Duels|Skywars}`
- **Stat system**: `Stat` base class with `StatInt`, `StatDouble`, `StatString` variants
- **Color coding**: Extensive performance-based color schemes in util classes (e.g., `getFkdrColor()`, `getStarWithColor()`, `getKdrColor()`)
- **Skywars-specific**: Complex emblem mapping system with 40+ Unicode symbols (`EMBLEM_MAP`)

### Asynchronous Processing
- All API requests use `Handler.asExecutor()` with named thread pool (`TabStats-%d`)
- **Pattern**: `setStatsAsync()` methods populate stat lists on background threads
- **Caching**: `StatWorld` manages `ConcurrentHashMap<UUID, HPlayer>` for player data

## Development Patterns

### Error Handling Convention
**Silent failure pattern** - Catch exceptions but don't spam console:
```java
try {
    // risky operation
} catch (Exception ex) {
    // Silently handle errors or return defaults
}
```
Used throughout config file I/O, API requests, and JSON parsing.

### Nick Detection System
**Full nick detection (UUID + skin hash)**
- **`NickDetector.java`** stores a set of known nick skin hashes and helpers: `isNickedUuid(uuid)`, `isKnownNickedSkin(hash)`, `extractSkinHash(url)`, and `isPlayerNicked(uuid, hash)`.
- **Logic**: A player is marked nicked only if BOTH are true:
    - UUID version is 1 (version 4 = real)
    - Skin hash matches the known nick skins list
- **Integration**:
    - `StatWorld.fetchStats()` decodes the player's GameProfile textures (base64), extracts the skin URL, derives the hash, and evaluates nick status using `NickDetector`.
    - `HPlayer.isNicked()` simply returns the precomputed flag. Rendering adds `[NICKED]` via `ChatColor`.
- **Notes**: Silent-fails on parsing/decoding; never attempts denicking (no name resolution), only displays nick status.

### Configuration Management
- API key auto-reloads from file on each `getApiKey()` call
- File structure: `{"ApiKey": "uuid-format-key"}` 
- Creates placeholder file with example format when missing
- **Important**: Config change detection triggers player cache refresh

### Custom Build System
- Uses Essential Loom plugin for Minecraft 1.8.9 + Forge 11.15.1.2318
- Version templating via Blossom plugin (`@VERSION@`, `@NAME@`, etc.)
- Client-side only mod (`clientSideOnly = true`)
- Java 8 compatibility required

### Game Mode Integration
- Each game mode has enum constants for API field names (see `DuelsModes`)
- Stat calculation methods with defensive programming (division by zero handling)
- **Color thresholds**: Hardcoded performance tiers for visual feedback

## Key Files to Understand

- **`/src/main/java/tabstats/playerapi/`** - Core API integration and player data management
- **`/src/main/java/tabstats/util/Handler.java`** - Centralized threading, JSON, and formatting utilities  
- **`/src/main/java/tabstats/render/StatsTab.java`** - Tab list rendering logic (heavily documented)
- **`build.gradle.kts`** - Essential Loom configuration for 1.8.9 Forge mods

## Development Workflow

### Building & Testing
```bash
./gradlew build          # Builds mod jar
./gradlew runClient      # Launches test client
```

### Adding New Game Modes
1. Create new enum in `HypixelGames` 
2. Extend `HGameBase` with game-specific util class
3. Implement stat calculation and color coding methods
4. Add to `StatWorld.fetchStats()` instantiation
5. For complex display formatting (like Skywars emblems), create helper methods in util class

### API Integration Notes
- **Rate limiting**: No explicit throttling - relies on caching
- **Player detection**: UUID version 4 = real players, version 1 = nicked players  
- **Cache management**: 5-second existence check before API fetching
- **Error types**: `InvalidKeyException`, `PlayerNullException`, `GameNullException`, `BadJsonException`
- **Nick detection**: Sophisticated dual-check system using UUID version analysis + skin hash comparison
- **Retry logic**: Exponential backoff for API failures, separate tick-based retry for nick uncertainty