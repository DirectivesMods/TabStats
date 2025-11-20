package tabstats.playerapi.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import tabstats.playerapi.exception.ApiRequestException;
import tabstats.playerapi.exception.BadJsonException;
import tabstats.util.References;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Minimal helper for the Urchin /player endpoint. The only things TabStats cares about are
 * the report type, its reason, and when it was added.
 */
public class UrchinAPI {
    private static final String PLAYER_ENDPOINT = "https://urchin.ws/player";
    private static final String USER_AGENT = "TabStats-Urchin/" + References.VERSION;

    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

    /**
     * Issues a POST to https://urchin.ws/player with the requested usernames and converts the response
     * into lightweight {@link UrchinReport} instances. Only Urchin report categories enumerated in
     * {@link UrchinReportType} are retained.
     *
     * @param apiKey    Urchin API key
     * @param usernames Player names or UUIDs to check
     * @return Map keyed by normalized username containing zero or more hits
     */
    public Map<String, List<UrchinReport>> fetchPlayerReports(String apiKey, Collection<String> usernames)
            throws ApiRequestException, BadJsonException {
        if (apiKey == null || apiKey.trim().isEmpty() || usernames == null || usernames.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> normalizedUsernames = usernames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (normalizedUsernames.isEmpty()) {
            return Collections.emptyMap();
        }

        JsonObject payload = new JsonObject();
        JsonArray usernameArray = new JsonArray();
        for (String username : normalizedUsernames) {
            usernameArray.add(new JsonPrimitive(username));
        }
        payload.add("usernames", usernameArray);

        URI uri = buildUri(apiKey.trim());
        JsonObject response = execute(uri, payload.toString());
        return parsePlayerReports(response, normalizedUsernames);
    }

    private JsonObject execute(URI uri, String body) throws ApiRequestException, BadJsonException {
        HttpPost request = new HttpPost(uri);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");
        request.setHeader("User-Agent", USER_AGENT);
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
            JsonObject payload = new JsonObject();

            if (!responseBody.isEmpty()) {
                try {
                    payload = new JsonParser().parse(responseBody).getAsJsonObject();
                } catch (IllegalStateException | JsonSyntaxException ex) {
                    throw new BadJsonException();
                }
            }

            if (statusCode >= 200 && statusCode < 300) {
                return payload;
            }

            String errorMessage = "Urchin API returned status code " + statusCode;
            if (payload != null) {
                if (payload.has("message") && payload.get("message").isJsonPrimitive()) {
                    errorMessage = payload.get("message").getAsString();
                } else if (payload.has("error") && payload.get("error").isJsonPrimitive()) {
                    errorMessage = payload.get("error").getAsString();
                }
            }
            throw new ApiRequestException(errorMessage);
        } catch (IOException ex) {
            throw new ApiRequestException("Unable to reach the Urchin API");
        }
    }

    private static Map<String, List<UrchinReport>> parsePlayerReports(JsonObject payload, List<String> requestedUsernames) {
        Map<String, List<UrchinReport>> results = requestedUsernames.stream()
                .collect(Collectors.toMap(name -> name, name -> Collections.emptyList(), (a, b) -> a, java.util.LinkedHashMap::new));

        if (payload == null || !payload.has("players") || !payload.get("players").isJsonObject()) {
            return results;
        }

        JsonObject players = payload.getAsJsonObject("players");
        for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }

            JsonArray reports = entry.getValue().getAsJsonArray();
            List<UrchinReport> parsedReports = new ArrayList<>();
            for (JsonElement reportElement : reports) {
                if (!reportElement.isJsonObject()) {
                    continue;
                }
                UrchinReport report = toReport(reportElement.getAsJsonObject());
                if (report != null) {
                    parsedReports.add(report);
                }
            }

            results.put(entry.getKey(), parsedReports);
        }

        return results;
    }

    private static UrchinReport toReport(JsonObject reportObject) {
        if (reportObject == null || !reportObject.has("type")) {
            return null;
        }

        UrchinReportType type = UrchinReportType.fromWireValue(getString(reportObject, "type", null));
        if (type == null) {
            return null;
        }

        String addedOn = getString(reportObject, "added_on", getString(reportObject, "addedOn", ""));
        String reason = getString(reportObject, "reason", "");
        return new UrchinReport(type, addedOn, reason);
    }

    private static URI buildUri(String apiKey) throws ApiRequestException {
        try {
            URIBuilder builder = new URIBuilder(PLAYER_ENDPOINT);
            builder.addParameter("key", apiKey);
            return builder.build();
        } catch (URISyntaxException ex) {
            throw new ApiRequestException("Failed to build Urchin API request URL");
        }
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if (object == null || key == null || !object.has(key)) {
            return fallback;
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            String value = element.getAsString();
            return value == null ? fallback : value;
        } catch (UnsupportedOperationException ex) {
            return fallback;
        }
    }

    /**
     * All Urchin report types.
     */
    public enum UrchinReportType {
        SNIPER("sniper"),
        POSSIBLE_SNIPER("possible_sniper"),
        LEGIT_SNIPER("legit_sniper"),
        CONFIRMED_CHEATER("confirmed_cheater"),
        BLATANT_CHEATER("blatant_cheater"),
        CLOSET_CHEATER("closet_cheater"),
        CAUTION("caution"),
        ACCOUNT("account"),
        INFO("info");

        private final String wireValue;

        UrchinReportType(String wireValue) {
            this.wireValue = wireValue;
        }

        static UrchinReportType fromWireValue(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }

            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (UrchinReportType type : values()) {
                if (type.wireValue.equals(normalized)) {
                    return type;
                }
            }
            return null;
        }

        public String getWireValue() {
            return wireValue;
        }
    }

    /**
     * Lightweight DTO for a single Urchin report entry.
     */
    public static final class UrchinReport {
        private final UrchinReportType type;
        private final String addedOn;
        private final String reason;

        private UrchinReport(UrchinReportType type, String addedOn, String reason) {
            this.type = type;
            this.addedOn = addedOn == null ? "" : addedOn;
            this.reason = reason == null ? "" : reason;
        }

        public UrchinReportType getType() {
            return type;
        }

        public String getAddedOn() {
            return addedOn;
        }

        public String getReason() {
            return reason;
        }
    }
}
