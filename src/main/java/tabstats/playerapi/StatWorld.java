package tabstats.playerapi;

import tabstats.playerapi.api.HypixelAPI;
import tabstats.playerapi.api.games.bedwars.Bedwars;
import tabstats.playerapi.api.games.duels.Duels;
import tabstats.playerapi.api.games.skywars.Skywars;
import tabstats.playerapi.exception.ApiRequestException;
import tabstats.playerapi.exception.BadJsonException;
import tabstats.playerapi.exception.InvalidKeyException;
import tabstats.playerapi.exception.PlayerNullException;
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
    protected final List<UUID> statAssembly = new ArrayList<>();
    protected final List<UUID> existedMoreThan5Seconds = new ArrayList<>();
    protected final Map<UUID, Integer> timeCheck = new HashMap<>();
    // Track retries for nick detection similar to script waiting up to ~200 ticks when name has no color
    protected final Map<UUID, Integer> nickRetryTicks = new HashMap<>();

    public StatWorld() {
        worldPlayers = new ConcurrentHashMap<>();
    }

    public void removePlayer(UUID playerUUID) {
        worldPlayers.remove(playerUUID);
        // Clean up tracking maps to prevent memory leaks
        nickRetryTicks.remove(playerUUID);
        timeCheck.remove(playerUUID);
        statAssembly.remove(playerUUID);
        existedMoreThan5Seconds.remove(playerUUID);
    }

    public void addPlayer(UUID playerUUID, HPlayer player) {
        worldPlayers.put(playerUUID, player);
    }

    public void clearPlayers() {
        worldPlayers.clear();
        // Clear all tracking maps to prevent memory leaks
        nickRetryTicks.clear();
        timeCheck.clear();
        statAssembly.clear();
        existedMoreThan5Seconds.clear();
    }

    public void refreshAllPlayers() {
        // Clear all cached data to force re-fetching with new API key
        clearPlayers();
    }

    public void refreshPlayer(UUID uuid) {
        // Remove specific player to force re-fetch
        worldPlayers.remove(uuid);
        statAssembly.remove(uuid);
        existedMoreThan5Seconds.remove(uuid);
        timeCheck.remove(uuid);
        nickRetryTicks.remove(uuid);
    }

    public ConcurrentHashMap<UUID, HPlayer> getWorldPlayers() {
        return this.worldPlayers;
    }

    public void removeFromStatAssembly(UUID uuid) { this.statAssembly.remove(uuid); }

    public HPlayer getPlayerByUUID(UUID uuid) {
        return this.worldPlayers.get(uuid);
    }

    public HPlayer getPlayerByName(String name) {
        for (Map.Entry<UUID,HPlayer> playerEntry : this.worldPlayers.entrySet()) {
            if (playerEntry.getValue().getNickname().equalsIgnoreCase(name))
                return playerEntry.getValue();
        }

        return null;
    }

    public void fetchStats(EntityPlayer entityPlayer) {
        fetchStatsWithRetry(entityPlayer, 0);
    }
    
    private void fetchStatsWithRetry(EntityPlayer entityPlayer, int apiRetryAttempt) {
        Handler.asExecutor(() -> {
            UUID uuid = entityPlayer.getUniqueID();
            String playerName = entityPlayer.getName();
            String playerUUID = entityPlayer.getUniqueID().toString().replace("-", "");

            HPlayer hPlayer = new HPlayer(playerUUID, playerName);

            // Fire both API call AND nick detection simultaneously for speed
            boolean apiSuccess = false;
            boolean isDefinitelyNicked = false;
            Exception apiException = null;
            
            // 1. Fire API call immediately
            try {
                JsonObject wholeObject = new HypixelAPI().getWholeObject(playerUUID);
                JsonObject playerObject = wholeObject.get("player").getAsJsonObject();

                hPlayer.setPlayerRank(playerObject);
                hPlayer.setPlayerName(playerObject.get("displayname").getAsString());

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
                this.addPlayer(uuid, hPlayer);
                this.removeFromStatAssembly(uuid);
                nickRetryTicks.remove(uuid);
                return;
            }
            
            if (isDefinitelyNicked) {
                // Definitely nicked - display as nicked permanently, never check again
                hPlayer.setNicked(true);
                this.addPlayer(uuid, hPlayer);
                this.removeFromStatAssembly(uuid);
                nickRetryTicks.remove(uuid);
                return;
            }
            
            // 4. API failed - handle based on nick uncertainty
            if (!apiSuccess) {
                // Don't retry on certain permanent failures
                if (apiException instanceof InvalidKeyException) {
                    // Invalid API key - stop everything, don't waste calls
                    this.removeFromStatAssembly(uuid);
                    return;
                }
                
                // If UUID suggests possible nick but we're uncertain about skin, use nick retry system
                if (NickDetector.isNickedUuid(playerUUID)) {
                    // Uncertain about nick status - use nick retry system (tick-based)
                    int ticks = nickRetryTicks.merge(uuid, 1, (a, b) -> a + b);
                    if (ticks < 200) {
                        // Retry entire process for nick detection 
                        Handler.asExecutor(() -> fetchStatsWithRetry(entityPlayer, apiRetryAttempt));
                        return;
                    } else {
                        // Max nick retries reached - uncertain status, treat as regular player with no stats
                        // This ensures players like "WHOAPERJIS" show only their name, not [NICKED]
                        hPlayer.setNicked(false);
                        this.addPlayer(uuid, hPlayer);
                        this.removeFromStatAssembly(uuid);
                        nickRetryTicks.remove(uuid);
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
                        this.addPlayer(uuid, hPlayer);
                        this.removeFromStatAssembly(uuid);
                        return;
                    }
                }
            }
            
            // 5. API succeeded - player is definitely real, not nicked
            // (API wouldn't return valid data for nicked players)
            hPlayer.setNicked(false);
            this.addPlayer(uuid, hPlayer);
            this.removeFromStatAssembly(uuid);
            nickRetryTicks.remove(uuid);
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
}
