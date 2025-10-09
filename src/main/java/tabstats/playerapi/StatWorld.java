package tabstats.playerapi;

import tabstats.config.ModConfig;
import tabstats.playerapi.api.HypixelAPI;
import tabstats.playerapi.api.games.bedwars.Bedwars;
import tabstats.playerapi.api.games.duels.Duels;
import tabstats.playerapi.api.games.skywars.Skywars;
import tabstats.playerapi.exception.ApiRequestException;
import tabstats.playerapi.exception.ApiThrottleException;
import tabstats.playerapi.exception.BadJsonException;
import tabstats.playerapi.exception.InvalidKeyException;
import tabstats.playerapi.exception.PlayerNullException;
import tabstats.util.ChatColor;
import tabstats.util.Handler;
import tabstats.util.NickDetector;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.EntityPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatWorld {
    private final ConcurrentHashMap<UUID, HPlayer> worldPlayers;
    private final Map<String, HPlayer> nameAliases;
    protected final Set<UUID> statAssembly = ConcurrentHashMap.newKeySet();
    protected final Set<UUID> existedMoreThan5Seconds = ConcurrentHashMap.newKeySet();
    protected final Map<UUID, Integer> timeCheck = new HashMap<>();

    public StatWorld() {
        worldPlayers = new ConcurrentHashMap<>();
        nameAliases = new ConcurrentHashMap<>();
    }

    public void removePlayer(UUID playerUUID) {
        HPlayer removed = worldPlayers.remove(playerUUID);
        // Clean up tracking maps to prevent memory leaks
        timeCheck.remove(playerUUID);
        statAssembly.remove(playerUUID);
        existedMoreThan5Seconds.remove(playerUUID);
        removeAliases(removed);
    }

    public void addPlayer(UUID playerUUID, HPlayer player) {
        worldPlayers.put(playerUUID, player);
        registerAlias(player, player.getPlayerName());
    }

    public void clearPlayers() {
        worldPlayers.clear();
        // Clear all tracking maps to prevent memory leaks
        timeCheck.clear();
        statAssembly.clear();
        existedMoreThan5Seconds.clear();
        nameAliases.clear();
    }

    /**
     * Re-render tab list: For each player check if they're in cache, if yes display cached data,
     * if not in cache then fetch stats for that player only
     */
    public void rerenderTabList() {
        // Clear tracking to allow fresh processing but preserve cached players
        timeCheck.clear();
        statAssembly.clear();
        
        // Preserve existence tracking for cached players to avoid 5-second delays
        existedMoreThan5Seconds.clear();
        existedMoreThan5Seconds.addAll(worldPlayers.keySet());
        
        // The actual re-rendering logic happens in WorldLoader.onTick():
        // - Cached players display immediately
        // - Non-cached players trigger fetchStatsWithRetry()
    }

    /**
     * Recheck all players: Force all players through fetchStatsWithRetry regardless of cache status
     */
    public void recheckAllPlayers() {
        // Clear all cached data to force re-fetching for everyone
        clearPlayers();
    }

    /**
     * Force recheck a specific player: Remove from cache and trigger fresh fetchStatsWithRetry
     */
    public void recheckPlayer(UUID uuid) {
        // Remove specific player to force re-fetch
        HPlayer removed = worldPlayers.remove(uuid);
        statAssembly.remove(uuid);
        existedMoreThan5Seconds.remove(uuid);
        timeCheck.remove(uuid);
        removeAliases(removed);
    }

    public ConcurrentHashMap<UUID, HPlayer> getWorldPlayers() {
        return this.worldPlayers;
    }

    public void removeFromStatAssembly(UUID uuid) { this.statAssembly.remove(uuid); }

    public HPlayer getPlayerByUUID(UUID uuid) {
        return this.worldPlayers.get(uuid);
    }

    public HPlayer getPlayerByIdentity(UUID uuid, String... fallbackName) {
        HPlayer player = this.worldPlayers.get(uuid);
        if (player != null) {
            return player;
        }

        if (fallbackName != null) {
            for (String candidate : fallbackName) {
                if (candidate == null) {
                    continue;
                }

                String normalized = candidate.trim();
                if (normalized.isEmpty()) {
                    continue;
                }

                HPlayer aliased = nameAliases.get(normalized.toLowerCase(Locale.ROOT));
                if (aliased != null) {
                    return aliased;
                }
            }
        }

        return null;
    }

    public HPlayer getPlayerByName(String name) {
        for (Map.Entry<UUID,HPlayer> playerEntry : this.worldPlayers.entrySet()) {
            if (playerEntry.getValue().getNickname().equalsIgnoreCase(name))
                return playerEntry.getValue();
        }

        return null;
    }

    /**
     * Fetch stats for a specific player using the retry system
     */
    public void fetchStats(EntityPlayer entityPlayer) {
        if (!ModConfig.getInstance().isModEnabled()) {
            return;
        }
        fetchStatsWithRetry(entityPlayer, 0);
    }
    
    private void fetchStatsWithRetry(EntityPlayer entityPlayer, int apiRetryAttempt) {
        Handler.asExecutor(() -> {
            if (!ModConfig.getInstance().isModEnabled()) {
                this.statAssembly.remove(entityPlayer.getUniqueID());
                return;
            }
            UUID uuid = entityPlayer.getUniqueID();
            String playerName = entityPlayer.getName();
            String playerUUID = entityPlayer.getUniqueID().toString().replace("-", "");

            String displayComponent = entityPlayer.getDisplayName() != null ? entityPlayer.getDisplayName().getFormattedText() : null;
            HPlayer existing = getPlayerByIdentity(uuid, displayComponent, playerName);
            if (existing != null) {
                cachePlayer(uuid, existing);
                registerAlias(existing, displayComponent);
                return;
            }

            HPlayer hPlayer = new HPlayer(playerUUID, playerName);
            registerAlias(hPlayer, playerName);
            registerAlias(hPlayer, displayComponent);

            // Fire API call; nick status is inferred instantly from UUID version (v1 = nicked)
            boolean apiSuccess = false;
            Exception apiException = null;
            boolean throttleTriggered = false;
            boolean globalThrottle = false;
            int uuidVersion = uuid.version();
            
            // 1. Attempt API call
            try {
                JsonObject wholeObject = new HypixelAPI().getWholeObject(playerUUID);
                JsonObject playerObject = wholeObject.get("player").getAsJsonObject();

                hPlayer.setPlayerRank(playerObject);
                hPlayer.setPlayerName(playerObject.get("displayname").getAsString());
                registerAlias(hPlayer, hPlayer.getPlayerName());

                hPlayer.addGames(
                        new Bedwars(playerName, playerUUID, wholeObject),
                        new Duels(playerName, playerUUID, wholeObject),
                        new Skywars(playerName, playerUUID, wholeObject)
                );
                apiSuccess = true;
                
            } catch (ApiThrottleException ex) {
                apiSuccess = false;
                apiException = ex;
                throttleTriggered = true;
                globalThrottle = ex.isGlobal();
            } catch (PlayerNullException | ApiRequestException | InvalidKeyException | BadJsonException ex) {
                apiSuccess = false;
                apiException = ex;
            }
            
            // 2. Determine nick status purely from UUID version (v1 = nicked)
            boolean isNicked = NickDetector.isNickedUuid(playerUUID);

            // 3. Handle results based on outcomes
            if (apiSuccess) {
                // API worked - player is definitely real, not nicked (API wouldn't return data for nicked players)
                hPlayer.setNicked(false);
                cachePlayer(uuid, hPlayer);
                return;
            }
            
            if (isNicked) {
                // Nicked player (UUID v1) - no API data expected, mark as nicked and cache
                hPlayer.setNicked(true);
                cachePlayer(uuid, hPlayer);
                return;
            }

            // 4. API failed - handle based on nick uncertainty
            if (!apiSuccess) {
                if (throttleTriggered) {
                    if (apiRetryAttempt < 8) {
                        long baseDelay = globalThrottle ? 5_000L : 2_000L;
                        long delay = baseDelay * Math.max(1, apiRetryAttempt + 1);
                        Handler.asExecutor(() -> {
                            if (!ModConfig.getInstance().isModEnabled()) {
                                this.statAssembly.remove(entityPlayer.getUniqueID());
                                return;
                            }
                            try {
                                Thread.sleep(delay);
                                fetchStatsWithRetry(entityPlayer, apiRetryAttempt + 1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                        return;
                    }
                    throttleTriggered = false; // fall through to cache fallback below
                }

                if (uuidVersion == 2 && apiException instanceof PlayerNullException) {
                    // Version 2 UUIDs with no API data are lobby bots/spoofs - leave in statAssembly so we don't re-fetch
                    removeAliases(hPlayer);
                    return;
                }
                // Don't retry on certain permanent failures
                if (apiException instanceof InvalidKeyException) {
                    // Invalid API key - stop everything, don't waste calls
                    this.removeFromStatAssembly(uuid);
                    return;
                }
                
                // Real UUID (v4 or v2) but API failed - use exponential backoff for API issues
                if (apiRetryAttempt < 8) { // 0-7 = 8 attempts total
                    // Schedule retry with exponential backoff
                    Handler.asExecutor(() -> {
                        if (!ModConfig.getInstance().isModEnabled()) {
                            this.statAssembly.remove(entityPlayer.getUniqueID());
                            return;
                        }
                        try {
                            Thread.sleep(apiRetryAttempt == 0 ? 0 : Math.round(250 * Math.pow(2, apiRetryAttempt - 1)));
                            fetchStatsWithRetry(entityPlayer, apiRetryAttempt + 1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    return;
                } else {
                    // Max API retries reached for real UUID - treat as regular player with no stats
                    hPlayer.setNicked(false);
                    cachePlayer(uuid, hPlayer);
                    return;
                }
            }

            // 5. API succeeded - player is definitely real, not nicked
            // (API wouldn't return valid data for nicked players)
            hPlayer.setNicked(false);
            cachePlayer(uuid, hPlayer);
        });
    }

    // Skin hash extraction removed – no longer needed for nick detection

    private void registerAlias(HPlayer player, String name) {
        if (player == null || name == null) {
            return;
        }

        String normalized = name.trim();
        if (normalized.isEmpty()) {
            return;
        }

        storeAlias(normalized, player);

        String stripped = ChatColor.stripColor(normalized);
        if (stripped != null && !stripped.equalsIgnoreCase(normalized)) {
            storeAlias(stripped, player);
        }
    }

    private void removeAliases(HPlayer player) {
        if (player == null) {
            return;
        }

        nameAliases.entrySet().removeIf(entry -> entry.getValue() == player);
    }

    private void storeAlias(String name, HPlayer player) {
        if (name == null) {
            return;
        }

        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        nameAliases.put(trimmed.toLowerCase(Locale.ROOT), player);
    }

    private void cachePlayer(UUID uuid, HPlayer player) {
        this.addPlayer(uuid, player);
        this.removeFromStatAssembly(uuid);
    }
}
