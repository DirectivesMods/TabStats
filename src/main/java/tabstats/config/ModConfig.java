package tabstats.config;

import tabstats.util.Handler;
import net.minecraft.client.Minecraft;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;

import static tabstats.config.ModConfigNames.APIKEY;
import static tabstats.config.ModConfigNames.RENDER_HEADER_FOOTER;
import static tabstats.config.ModConfigNames.MOD_ENABLED;

public class ModConfig {
    private static final String CONFIG_FILENAME = "settings.json";
    private String apiKey;
    private String lastApiKey; // Track the last API key to detect changes
    private static ModConfig instance;
    private File configFile;
    private boolean renderHeaderFooter = true;
    private boolean modEnabled = true;

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
            if (!isModEnabled()) {
                return;
            }

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

    public boolean isRenderHeaderFooterEnabled() {
        return this.renderHeaderFooter;
    }

    public void setRenderHeaderFooterEnabled(boolean value) {
        this.renderHeaderFooter = value;
    }

    public boolean isModEnabled() {
        return this.modEnabled;
    }

    public void setModEnabled(boolean value) {
        this.modEnabled = value;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void makeFile() {
        File file = getFile();
        if (file.exists()) {
            return;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            if (file.createNewFile()) {
                JsonObject defaults = new JsonObject();
                defaults.addProperty(APIKEY.toString(), "");
                defaults.addProperty(RENDER_HEADER_FOOTER.toString(), true);
                defaults.addProperty(MOD_ENABLED.toString(), true);

                try (FileWriter writer = new FileWriter(file)) {
                    Handler.getGson().toJson(defaults, writer);
                    writer.flush();
                }
            }
        } catch (Exception ignored) {
            // Silently handle file creation errors
        }
    }

    public void loadConfigFromFile() {
        if (!getFile().exists()) {
            makeFile();
        }
        apiKey = getString(APIKEY);
        lastApiKey = apiKey;
        renderHeaderFooter = getBoolean(RENDER_HEADER_FOOTER, true);
        modEnabled = getBoolean(MOD_ENABLED, true);
    }

    public File getFile() {
        if (configFile != null) {
            return configFile;
        }

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

        if (!folder.exists()) {
            folder.mkdirs();
        }

        configFile = new File(folder, CONFIG_FILENAME);
        return configFile;
    }

    public void init() {
        loadConfigFromFile();
    }

    public void save() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(APIKEY.toString(), this.apiKey == null ? "" : this.apiKey); // Use the internal field, not getApiKey()
        map.put(RENDER_HEADER_FOOTER.toString(), this.renderHeaderFooter);
        map.put(MOD_ENABLED.toString(), this.modEnabled);
        File file = getFile();
        try (Writer writer = new FileWriter(file)) {
            Handler.getGson().toJson(map, writer);
            writer.flush(); // Ensure it's written to disk
        } catch (Exception ex) {
            // Silently handle save errors
        }
    }

    public String getString(ModConfigNames key) {
        File file = getFile();
        if (!file.exists()) {
            return "";
        }

        JsonParser parser = new JsonParser();

        try (FileReader reader = new FileReader(file)) {
            JsonObject object = parser.parse(reader).getAsJsonObject();
            if (!object.has(key.toString())) {
                return "";
            }
            return object.get(key.toString()).getAsString();
        } catch (Exception ex) {
            // Silently handle read errors
            return "";
        }
    }

    public boolean getBoolean(ModConfigNames key, boolean defaultValue) {
        File file = getFile();
        if (!file.exists()) {
            return defaultValue;
        }

        JsonParser parser = new JsonParser();

        try (FileReader reader = new FileReader(file)) {
            JsonObject object = parser.parse(reader).getAsJsonObject();
            if (!object.has(key.toString())) {
                return defaultValue;
            }
            return object.get(key.toString()).getAsBoolean();
        } catch (Exception ex) {
            // Silently handle read errors
            return defaultValue;
        }
    }
}
