package tabstats.playerapi;

import tabstats.playerapi.api.HypixelAPI;
import tabstats.playerapi.api.games.bedwars.Bedwars;
import tabstats.playerapi.api.games.duels.Duels;
import tabstats.playerapi.api.games.skywars.Skywars;
import tabstats.playerapi.exception.ApiRequestException;
import tabstats.playerapi.exception.BadJsonException;
import tabstats.playerapi.exception.InvalidKeyException;
import tabstats.playerapi.exception.PlayerNullException;
import tabstats.util.ChatColor;
import tabstats.util.Handler;
import tabstats.util.NickDetector;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.Base64;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.player.EntityPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatWorld {
    private final ConcurrentHashMap<UUID, HPlayer> worldPlayers;
    private final Map<String, HPlayer> nameAliases;
    protected final List<UUID> statAssembly = new ArrayList<>();
    protected final List<UUID> existedMoreThan5Seconds = new ArrayList<>();
    protected final Map<UUID, Integer> timeCheck = new HashMap<>();
    // Track retries for nick detection similar to script waiting up to ~200 ticks when name has no color
    protected final Map<UUID, Integer> nickRetryTicks = new HashMap<>();

    public StatWorld() {
        worldPlayers = new ConcurrentHashMap<>();
        nameAliases = new ConcurrentHashMap<>();
    }

    public void removePlayer(UUID playerUUID) {
        HPlayer removed = worldPlayers.remove(playerUUID);
        // Clean up tracking maps to prevent memory leaks
        nickRetryTicks.remove(playerUUID);
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
        nickRetryTicks.clear();
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
        
        // Reset nick retry counters to allow fresh attempts during refresh
        nickRetryTicks.clear();
        
        // Preserve existence tracking for cached players to avoid 5-second delays
        Set<UUID> preservedUUIDs = new HashSet<>(worldPlayers.keySet());
        existedMoreThan5Seconds.clear();
        existedMoreThan5Seconds.addAll(preservedUUIDs);
        
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
        nickRetryTicks.remove(uuid);
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
        fetchStatsWithRetry(entityPlayer, 0);
    }
    
    private void fetchStatsWithRetry(EntityPlayer entityPlayer, int apiRetryAttempt) {
        Handler.asExecutor(() -> {
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

            // Fire both API call AND nick detection simultaneously for speed
            boolean apiSuccess = false;
            boolean isDefinitelyNicked = false;
            Exception apiException = null;
            int uuidVersion = uuid.version();
            
            // 1. Fire API call immediately
            try {
                JsonObject wholeObject = new HypixelAPI().getWholeObject(playerUUID);
                JsonObject playerObject = wholeObject.get("player").getAsJsonObject();

                hPlayer.setPlayerRank(playerObject);
                hPlayer.setPlayerName(playerObject.get("displayname").getAsString());
                registerAlias(hPlayer, hPlayer.getPlayerName());

                Bedwars bw = new Bedwars(playerName, playerUUID, wholeObject);
                Duels duels = new Duels(playerName, playerUUID, wholeObject);
                Skywars sw = new Skywars(playerName, playerUUID, wholeObject);

                hPlayer.addGames(bw, duels, sw);
                apiSuccess = true;
                
            } catch (PlayerNullException | ApiRequestException | InvalidKeyException | BadJsonException ex) {
                apiSuccess = false;
                apiException = ex;
            }
            
            // 2. Fire nick detection immediately (parallel to API)
            String skinHash = extractSkinHashFromEntity(entityPlayer);
            isDefinitelyNicked = NickDetector.isPlayerNicked(playerUUID, skinHash);

            // 3. Handle results based on what we got
            if (apiSuccess) {
                // API worked - player is definitely real, not nicked (API wouldn't return data for nicked players)
                hPlayer.setNicked(false);
                cachePlayer(uuid, hPlayer);
                return;
            }

            if (isDefinitelyNicked) {
                // Definitely nicked - display as nicked permanently, never check again
                hPlayer.setNicked(true);
                cachePlayer(uuid, hPlayer);
                return;
            }

            // 4. API failed - handle based on nick uncertainty
            if (!apiSuccess) {
                if (uuidVersion == 2 && apiException instanceof PlayerNullException) {
                    // Version 2 UUIDs with no API data are lobby bots/spoofs - leave in statAssembly so we don't re-fetch
                    removeAliases(hPlayer);
                    nickRetryTicks.remove(uuid);
                    return;
                }
                // Don't retry on certain permanent failures
                if (apiException instanceof InvalidKeyException) {
                    // Invalid API key - stop everything, don't waste calls
                    this.removeFromStatAssembly(uuid);
                    return;
                }
                
                // If UUID suggests possible nick but we're uncertain about skin, use nick retry system
                if (NickDetector.isNickedUuid(playerUUID)) {
                    // Uncertain about nick status - retry nick detection only (no API calls are made)
                    int ticks = nickRetryTicks.merge(uuid, 1, (a, b) -> a + b);
                    if (ticks < 200) {
                        // Retry nick detection only - don't retry API calls for nicked players
                        this.removeFromStatAssembly(uuid);
                        // Will be retried on next tick via WorldLoader.onTick -> checkNickStatus
                        return;
                    } else {
                        // Max nick retries reached - uncertain status, treat as regular player with no stats
                        // This ensures players like "WHOAPERJIS" show only their name, not [NICKED]
                        hPlayer.setNicked(false);
                        cachePlayer(uuid, hPlayer);
                        return;
                    }
                } else {
                    // Real UUID (v4) but API failed - use exponential backoff for API issues
                    if (apiRetryAttempt < 8) { // 0-7 = 8 attempts total
                        long delayMs = apiRetryAttempt == 0 ? 0 : Math.round(250 * Math.pow(2, apiRetryAttempt - 1));
                        
                        // Schedule retry with exponential backoff
                        Handler.asExecutor(() -> {
                            try {
                                Thread.sleep(delayMs);
                                fetchStatsWithRetry(entityPlayer, apiRetryAttempt + 1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                        return;
                    } else {
                        // Max API retries reached for real UUID - treat as regular player with no stats
                        // This ensures new players show only their name, not as nicked
                        hPlayer.setNicked(false);
                        cachePlayer(uuid, hPlayer);
                        return;
                    }
                }
            }

            // 5. API succeeded - player is definitely real, not nicked
            // (API wouldn't return valid data for nicked players)
            hPlayer.setNicked(false);
            cachePlayer(uuid, hPlayer);
        });
    }

    protected String extractSkinHashFromEntity(EntityPlayer entityPlayer) {
        try {
            GameProfile profile = entityPlayer.getGameProfile();
            Property textures = profile.getProperties().get("textures").stream().findFirst().orElse(null);
            if (textures == null) return null;
            String json = new String(Base64.getDecoder().decode(textures.getValue()), "UTF-8");
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            if (!root.has("textures")) return null;
            JsonObject texturesObj = root.getAsJsonObject("textures");
            if (!texturesObj.has("SKIN")) return null;
            JsonObject skinObj = texturesObj.getAsJsonObject("SKIN");
            if (!skinObj.has("url")) return null;
            String url = skinObj.get("url").getAsString();
            return NickDetector.extractSkinHash(url);
        } catch (Exception ignored) {
            return null;
        }
    }

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
        nickRetryTicks.remove(uuid);
    }
}
