package tabstats.gui;

import net.minecraft.client.gui.GuiScreen;
import tabstats.config.ModConfig;

public class HypixelApiKeyGui extends AbstractApiKeyGui {

    public HypixelApiKeyGui(GuiScreen parent) {
        super(parent);
    }

    @Override
    protected String getScreenTitle() {
        return "Hypixel API";
    }

    @Override
    protected String getChatKeyName() {
        return "Hypixel API key";
    }

    @Override
    protected String readStoredKey(ModConfig cfg) {
        return cfg.getApiKey();
    }

    @Override
    protected void storeKey(ModConfig cfg, String value) {
        cfg.setApiKey(value);
    }
}
