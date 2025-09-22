package tabstats.playerapi;

import tabstats.playerapi.api.HypixelAPI;
import tabstats.playerapi.api.games.bedwars.Bedwars;
import tabstats.playerapi.api.games.duels.Duels;
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
import com.google.gson.JsonObject;
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
    }

    public void addPlayer(UUID playerUUID, HPlayer player) {
        worldPlayers.put(playerUUID, player);
    }

    public void clearPlayers() {
        worldPlayers.clear();
        statAssembly.clear();
        existedMoreThan5Seconds.clear();
        timeCheck.clear();
        nickRetryTicks.clear();
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
        Handler.asExecutor(()-> {
            UUID uuid = entityPlayer.getUniqueID();
            String playerName = entityPlayer.getName();
            String playerUUID = entityPlayer.getUniqueID().toString().replace("-", "");
            String playerUUIDWithDashes = entityPlayer.getUniqueID().toString(); // Keep dashes for nick detection

            // Attempt to extract skin hash from the player's GameProfile textures
            String skinHash = extractSkinHashFromEntity(entityPlayer);

            boolean nicked = NickDetector.isPlayerNicked(playerUUIDWithDashes, skinHash);
            if (!nicked && NickDetector.isNickedUuid(playerUUIDWithDashes)) {
                // If UUID indicates possible nick but skin hash isn't matched yet, retry up to 200 ticks
                int ticks = nickRetryTicks.merge(uuid, 1, (a,b)->a+b);
                if (ticks < 200) {
                    // Requeue a lightweight retry
                    Handler.asExecutor(() -> fetchStats(entityPlayer));
                    return;
                } else {
                    nickRetryTicks.remove(uuid);
                }
            } else {
                nickRetryTicks.remove(uuid);
            }

            HPlayer hPlayer = new HPlayer(playerUUID, playerName);
            hPlayer.setNicked(nicked);

            if (!nicked) {
                try {
                    JsonObject wholeObject = new HypixelAPI().getWholeObject(playerUUID);
                    JsonObject playerObject = wholeObject.get("player").getAsJsonObject();

                    hPlayer.setPlayerRank(playerObject);
                    hPlayer.setPlayerName(playerObject.get("displayname").getAsString());

                    Bedwars bw = new Bedwars(playerName, playerUUID, wholeObject);
                    Duels duels = new Duels(playerName, playerUUID, wholeObject);

                    hPlayer.addGames(bw, duels);
                } catch (PlayerNullException | ApiRequestException | InvalidKeyException | BadJsonException ex) {
                    this.removeFromStatAssembly(uuid);
                    return;
                }
            }

            this.addPlayer(uuid, hPlayer);
            this.removeFromStatAssembly(uuid);
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
