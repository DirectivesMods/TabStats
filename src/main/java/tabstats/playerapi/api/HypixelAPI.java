package tabstats.playerapi.api;

import tabstats.config.ModConfig;
import tabstats.playerapi.api.games.HypixelGames;
import tabstats.playerapi.exception.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class HypixelAPI {
    public JsonObject achievementObj;
    public JsonObject playerObject;
    private static final String PLAYER_ENDPOINT = "https://api.hypixel.net/v2/player?key=%s&uuid=%s";
    private static final String MOJANG_UUID_ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final PoolingHttpClientConnectionManager HTTP_CONN_MANAGER;
    private static final CloseableHttpClient HTTP_CLIENT;
    private static final RequestConfig REQUEST_CONFIG;

    static {
        HTTP_CONN_MANAGER = new PoolingHttpClientConnectionManager();
        HTTP_CONN_MANAGER.setMaxTotal(32);
        HTTP_CONN_MANAGER.setDefaultMaxPerRoute(16);
        REQUEST_CONFIG = RequestConfig.custom()
                .setConnectTimeout(5_000)
                .setSocketTimeout(5_000)
                .setConnectionRequestTimeout(5_000)
                .build();

        HTTP_CLIENT = HttpClients.custom()
                .setConnectionManager(HTTP_CONN_MANAGER)
                .setDefaultRequestConfig(REQUEST_CONFIG)
                .build();
    }

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
            HttpGet request = new HttpGet(String.format(PLAYER_ENDPOINT, apiKey, uuid.replace("-", "")));
            request.addHeader("Accept", "application/json");
            try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return obj;
                }

                try (InputStreamReader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8)) {
                    obj = new JsonParser().parse(reader).getAsJsonObject();
                } catch (JsonSyntaxException ex) {
                    throw new BadJsonException();
                } finally {
                    EntityUtils.consumeQuietly(entity);
                }

                boolean throttle = obj.has("throttle") && obj.get("throttle").getAsBoolean();
                boolean globalThrottle = obj.has("global") && obj.get("global").getAsBoolean();
                String cause = "";

                if (obj.has("cause") && !obj.get("cause").isJsonNull()) {
                    try {
                        cause = obj.get("cause").getAsString();
                    } catch (UnsupportedOperationException ignored) {
                        cause = "";
                    }
                }

                boolean success = obj.has("success") && obj.get("success").getAsBoolean();
                if (!success) {
                    if (throttle || globalThrottle) {
                        throw new ApiThrottleException(globalThrottle);
                    }
                    if ("Invalid API key".equalsIgnoreCase(cause)) {
                        throw new InvalidKeyException();
                    }
                    throw cause.isEmpty() ? new ApiRequestException() : new ApiRequestException(cause);
                }

                JsonElement playerElement = obj.get("player");
                if (playerElement == null || playerElement.isJsonNull()) {
                    if (throttle || globalThrottle) {
                        throw new ApiThrottleException(globalThrottle);
                    }
                    if ("Invalid API key".equalsIgnoreCase(cause)) {
                        throw new InvalidKeyException();
                    }
                    throw new PlayerNullException();
                }
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
        HttpGet request = new HttpGet(String.format(MOJANG_UUID_ENDPOINT, name));
        request.addHeader("Accept", "application/json");
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return uuid;
            }

            try (InputStream is = entity.getContent();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject object = new JsonParser().parse(reader).getAsJsonObject();
                uuid = object.get("id").getAsString();
            } catch (NullPointerException ex) {
                // Silently handle null pointer errors
            } finally {
                EntityUtils.consumeQuietly(entity);
            }
        } catch (IOException ex) {
            // Silently handle IO errors
        }

        return uuid;
    }
}
