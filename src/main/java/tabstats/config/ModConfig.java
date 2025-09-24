package tabstats.config;

import tabstats.util.Handler;
import tabstats.util.References;
import net.minecraft.client.Minecraft;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;

import static tabstats.config.ModConfigNames.APIKEY;

public class ModConfig {
    private String apiKey;
    private String lastApiKey; // Track the last API key to detect changes
    private static ModConfig instance;

    public static ModConfig getInstance() {
        if (instance == null) instance = new ModConfig();
        return instance;

    }

    public String getApiKey() {
        // Always reload the API key from file to ensure we get the latest value
        if (getFile().exists()) {
            String freshApiKey = getString(APIKEY);
            if (freshApiKey != null && !freshApiKey.trim().isEmpty()) {
                // Check if API key has changed
                if (lastApiKey != null && !lastApiKey.equals(freshApiKey)) {
                    onApiKeyChanged();
                }
                this.lastApiKey = freshApiKey;
                this.apiKey = freshApiKey;
                return freshApiKey;
            }
        }
        return apiKey;
    }

    private void onApiKeyChanged() {
        // Clear cached player data when API key changes
        try {
            tabstats.TabStats tabStats = tabstats.TabStats.getTabStats();
            if (tabStats != null && tabStats.getStatWorld() != null) {
                tabStats.getStatWorld().recheckAllPlayers();
            }
        } catch (Exception e) {
            // Silent fail - don't spam console
        }
    }

    public void setApiKey(String key) {
        // Only update if the key actually changed
        String normalizedKey = key == null ? "" : key.trim();
        String currentKey = this.apiKey == null ? "" : this.apiKey.trim();
        
        if (!normalizedKey.equals(currentKey)) {
            this.apiKey = key;
            // Update lastApiKey to prevent duplicate change detection
            this.lastApiKey = key;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void makeFile() {
        try {
            if (!getFile().exists()) {
                getFile().getParentFile().mkdirs();
                getFile().createNewFile();
                try (FileWriter writer = new FileWriter(getFile())) {
                    // Write a placeholder apikey.json so users know the expected format
                    String placeholder = "{\n  \"ApiKey\": \"XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX\"\n}\n";
                    writer.write(placeholder);
                    writer.flush();
                } catch (Exception e) {
                    // Silently handle file write errors
                }
            }
        } catch (Exception e) {
            // Silently handle file creation errors
        }
    }

    public void loadConfigFromFile() {
        if (!getFile().exists()) makeFile();
        apiKey = getString(APIKEY);
    }

    public File getFile() {
        File folder = null;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.mcDataDir != null) {
                folder = new File(mc.mcDataDir, "tabstats");
            }
        } catch (Throwable ignored) {
            // ignored - we'll fall back to user home .minecraft
        }

        if (folder == null) {
            String userHome = System.getProperty("user.home");
            folder = new File(userHome + File.separator + ".minecraft", "tabstats");
        }

        if (!folder.exists()) folder.mkdirs();
        return new File(folder, "apikey.json");
    }

    public void init() {
        loadConfigFromFile();
    }

    public void save() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(APIKEY.toString(), this.apiKey); // Use the internal field, not getApiKey()
        try (Writer writer = new FileWriter(getFile())) {
            Handler.getGson().toJson(map, writer);
            writer.flush(); // Ensure it's written to disk
        } catch (Exception ex) {
            // Silently handle save errors
        }
    }

    public String getString(ModConfigNames key) {
        JsonParser parser = new JsonParser();
        String s = "";
        try {
            JsonObject object = (JsonObject) parser.parse(new FileReader(getFile()));
            s = object.get(key.toString()).getAsString();
        } catch (NullPointerException ignored) {
        } catch (Exception ex) {
            // Silently handle read errors
        }
        return s;
    }
}
