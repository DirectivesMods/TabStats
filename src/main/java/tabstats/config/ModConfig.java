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
    private static ModConfig instance;

    public static ModConfig getInstance() {
        if (instance == null) instance = new ModConfig();
        return instance;

    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String key) {
        apiKey = key;
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
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        map.put(APIKEY.toString(), getApiKey());
        try (Writer writer = new FileWriter(getFile())) {
            Handler.getGson().toJson(map, writer);
        } catch (Exception ex) {
            System.out.println("Unable to save config file");
            ex.printStackTrace();
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
            ex.printStackTrace();
        }
        return s;
    }
}
