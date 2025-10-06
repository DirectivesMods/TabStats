package tabstats.playerapi.api;

import tabstats.config.ModConfig;
import tabstats.playerapi.api.games.HypixelGames;
import tabstats.playerapi.exception.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class HypixelAPI {
    public JsonObject achievementObj;
    public JsonObject playerObject;

    private String getApiKey() {
        return ModConfig.getInstance().getApiKey();
    }

    /**
     * @param uuid Target player's UUID
     * @return JsonObject of the player's whole api result
     * @throws InvalidKeyException If Hypixel API Key is Invalid
     * @throws PlayerNullException If Target Player UUID is returned Null from the Hypixel API
     * @throws ApiRequestException If any other exception is thrown during the request
     */
    public JsonObject getWholeObject(String uuid) throws InvalidKeyException, PlayerNullException, ApiRequestException, BadJsonException {
        JsonObject obj = new JsonObject();
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidKeyException();
        } else {
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(new HttpGet(String.format("https://api.hypixel.net/v2/player?key=%s&uuid=%s", apiKey, uuid.replace("-", ""))))) {
                if (response.getEntity() == null) {
                    return obj;
                }

                try (InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)) {
                    obj = new JsonParser().parse(reader).getAsJsonObject();
                } catch (JsonSyntaxException ex) {
                    throw new BadJsonException();
                }

                if (obj.get("player") == null) {
                    if (obj.get("cause").getAsString().equalsIgnoreCase("Invalid API key")) throw new InvalidKeyException();
                    throw new PlayerNullException();
                }
                else if (obj.get("player").toString().equalsIgnoreCase("null"))
                    throw new PlayerNullException();
                else if (obj.get("success").getAsString().equals("false"))
                    throw new ApiRequestException();
            } catch (IOException ex) {
                // Silently handle IO errors
            }
        }

        JsonObject player = obj.get("player").getAsJsonObject();
        if (player.get("achievements") != null)
            this.achievementObj = player.get("achievements").getAsJsonObject();

        this.playerObject = player;
        return obj;
    }

    /**
     * @param wholeObject Target Player's Hypixel API Whole Object
     * @param game Game Stats to retrieve
     * @return JsonObject of the specified gameType's Stats
     */
    public JsonObject getGameData(JsonObject wholeObject, HypixelGames game) throws GameNullException {
        JsonObject player = wholeObject.get("player").getAsJsonObject();
        JsonObject stats = player.get("stats").getAsJsonObject();

        if (stats.get(game.getApiName()) != null) {
            return stats.get(game.getApiName()).getAsJsonObject();
        } else {
            throw new GameNullException(game);
        }
    }

    /**
     * @param name Target's Minecraft Name
     * @return UUID of Target using Mojang API
     */
    public static String getUUID(String name) {
        String uuid = "";
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(new HttpGet(String.format("https://api.mojang.com/users/profiles/minecraft/%s", name)))) {
            if (response.getEntity() == null) {
                return uuid;
            }

            try (InputStream is = response.getEntity().getContent();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject object = new JsonParser().parse(reader).getAsJsonObject();
                uuid = object.get("id").getAsString();
            } catch (NullPointerException ex) {
                // Silently handle null pointer errors
            }
        } catch (IOException ex) {
            // Silently handle IO errors
        }

        return uuid;
    }
}
