package tabstats.gui;

import net.minecraft.client.gui.GuiScreen;
import tabstats.config.ModConfig;

public class UrchinApiKeyGui extends AbstractApiKeyGui {

    public UrchinApiKeyGui(GuiScreen parent) {
        super(parent);
    }

    @Override
    protected String getScreenTitle() {
        return "Urchin API";
    }

    @Override
    protected String readStoredKey(ModConfig cfg) {
        return cfg.getUrchinApiKey();
    }

    @Override
    protected void storeKey(ModConfig cfg, String value) {
        cfg.setUrchinApiKey(value);
    }
}
