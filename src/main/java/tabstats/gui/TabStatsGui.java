package tabstats.gui;

import tabstats.TabStats;
import tabstats.config.ModConfig;
import tabstats.listener.GameOverlayListener;
import tabstats.render.StatsTab;
import tabstats.util.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;

public class TabStatsGui extends GuiScreen {
    private GuiButton headerFooterButton;
    private GuiButton modToggleButton;
    private int titleY;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        this.titleY = this.height / 2 - 70;
        int buttonHeight = 20;
        int rowSpacing = 24;
        int singleButtonWidth = 200;
        int centerX = this.width / 2;
        int toggleX = centerX - singleButtonWidth / 2;
        int modToggleY = this.titleY + rowSpacing;
        int headerToggleY = modToggleY + rowSpacing;

        ModConfig cfg = ModConfig.getInstance();

        this.modToggleButton = new GuiButton(6, toggleX, modToggleY, singleButtonWidth, buttonHeight, formatModToggleLabel(cfg.isModEnabled()));
        this.buttonList.add(this.modToggleButton);

        this.headerFooterButton = new GuiButton(5, toggleX, headerToggleY, singleButtonWidth, buttonHeight, formatHeaderFooterLabel(cfg.isRenderHeaderFooterEnabled()));
        this.buttonList.add(this.headerFooterButton);

        int halfWidth = 98;
        int buttonSpacing = 4;
        int apiRowWidth = halfWidth * 2 + buttonSpacing;
        int apiStartX = centerX - apiRowWidth / 2;
        int apiButtonY = headerToggleY + rowSpacing;

        this.buttonList.add(new GuiButton(7, apiStartX, apiButtonY, halfWidth, buttonHeight, "Hypixel API"));
        this.buttonList.add(new GuiButton(8, apiStartX + halfWidth + buttonSpacing, apiButtonY, halfWidth, buttonHeight, "Urchin API"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ModConfig cfg = ModConfig.getInstance();
        if (button.id == 5) {
            boolean newValue = !cfg.isRenderHeaderFooterEnabled();
            cfg.setRenderHeaderFooterEnabled(newValue);
            cfg.save();
            if (this.headerFooterButton != null) {
                this.headerFooterButton.displayString = formatHeaderFooterLabel(newValue);
            }

            TabStats instance = TabStats.getTabStats();
            if (instance != null) {
                GameOverlayListener overlayListener = instance.getGameOverlayListener();
                if (overlayListener != null) {
                    StatsTab statsTab = overlayListener.getStatsTab();
                    if (statsTab != null) {
                        statsTab.setRenderHeaderFooter(newValue);
                    }
                }
            }

        } else if (button.id == 6) {
            boolean newValue = !cfg.isModEnabled();
            cfg.setModEnabled(newValue);
            cfg.save();

            if (this.modToggleButton != null) {
                this.modToggleButton.displayString = formatModToggleLabel(newValue);
            }

            TabStats instance = TabStats.getTabStats();
            if (instance != null) {
                instance.applyModEnabled(newValue);
            }

        } else if (button.id == 7) {
            Minecraft.getMinecraft().displayGuiScreen(new HypixelApiKeyGui(this));
        } else if (button.id == 8) {
            Minecraft.getMinecraft().displayGuiScreen(new UrchinApiKeyGui(this));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, "TabStats", this.width / 2, this.titleY, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String formatHeaderFooterLabel(boolean enabled) {
        return "Header/Footer: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled");
    }

    private String formatModToggleLabel(boolean enabled) {
        return "Mod: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled");
    }
}
