package tabstats.gui;

import tabstats.TabStats;
import tabstats.config.ModConfig;
import tabstats.listener.GameOverlayListener;
import tabstats.render.StatsTab;
import tabstats.util.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import java.io.IOException;

public class TabStatsGui extends GuiScreen {
    private MaskedGuiTextField apiField;
    private boolean showingApiKey = false;
    private GuiButton headerFooterButton;
    private GuiButton modToggleButton;
    private int fieldX;
    private int fieldY;
    private int titleY;
    private int fieldWidth;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int centerX = this.width / 2;
        this.titleY = this.height / 2 - 80;
        int toggleY = this.titleY + 20;
        int headerToggleY = toggleY + 24;

        int fieldPadding = 10;
        int desiredWidth = this.fontRendererObj.getStringWidth("WWWWWWWW-WWWW-WWWW-WWWW-WWWWWWWWWWWW") + fieldPadding;
        this.fieldWidth = Math.max(220, desiredWidth);
        this.fieldX = centerX - this.fieldWidth / 2;
        this.fieldY = headerToggleY + 36;
        int buttonRowY = this.fieldY + 32;

        ModConfig cfg = ModConfig.getInstance();

        boolean modEnabled = cfg.isModEnabled();
        this.modToggleButton = new GuiButton(6, this.fieldX, toggleY, this.fieldWidth, 20, formatModToggleLabel(modEnabled));
        this.buttonList.add(this.modToggleButton);

        boolean headerFooterEnabled = cfg.isRenderHeaderFooterEnabled();
        this.headerFooterButton = new GuiButton(5, this.fieldX, headerToggleY, this.fieldWidth, 20, formatHeaderFooterLabel(headerFooterEnabled));
        this.buttonList.add(this.headerFooterButton);

        this.apiField = new MaskedGuiTextField(3, this.fontRendererObj, this.fieldX, this.fieldY, this.fieldWidth, 20);
        this.apiField.setMaxStringLength(50);
        String current = cfg.getApiKey();
        this.apiField.setText(current == null ? "" : current);
        this.showingApiKey = false;
        this.apiField.setRevealing(this.showingApiKey);

        int showButtonX = this.fieldX + this.fieldWidth + 10;
        this.buttonList.add(new GuiButton(4, showButtonX, this.fieldY, 60, 20, "Show"));

        int buttonSpacing = 6;
        int actionButtonWidth = (this.fieldWidth - buttonSpacing * 2) / 3;
        int firstButtonX = this.fieldX;
        this.buttonList.add(new GuiButton(0, firstButtonX, buttonRowY, actionButtonWidth, 20, "Save"));
        this.buttonList.add(new GuiButton(1, firstButtonX + actionButtonWidth + buttonSpacing, buttonRowY, actionButtonWidth, 20, "Clear"));
        this.buttonList.add(new GuiButton(2, firstButtonX + (actionButtonWidth + buttonSpacing) * 2, buttonRowY, actionButtonWidth, 20, "Close"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ModConfig cfg = ModConfig.getInstance();
        if (button.id == 0) {
            String key = this.apiField.getText().trim();
            String currentKey = cfg.getApiKey();
            // Only update and refresh if the key actually changed
            if (!key.equals(currentKey == null ? "" : currentKey)) {
                cfg.setApiKey(key);
                cfg.save();
                this.apiField.setText(key);
                this.apiField.setRevealing(this.showingApiKey);
                TabStats instance = TabStats.getTabStats();
                if (instance != null && instance.isModEnabled() && instance.getStatWorld() != null) {
                    try {
                        instance.getStatWorld().recheckAllPlayers();
                    } catch (Exception ignored) {
                        // Silent fail - don't spam console
                    }
                }
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "[TabStats] " + ChatColor.WHITE + "API key updated."));
            } else {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "[TabStats] " + ChatColor.WHITE + "API key unchanged."));
            }
        } else if (button.id == 1) {
            String currentKey = cfg.getApiKey();
            // Only clear and refresh if there's actually a key to clear
            if (currentKey != null && !currentKey.trim().isEmpty()) {
                cfg.setApiKey("");
                cfg.save();
                this.apiField.setText("");
                this.apiField.setRevealing(this.showingApiKey);
                TabStats instance = TabStats.getTabStats();
                if (instance != null && instance.isModEnabled() && instance.getStatWorld() != null) {
                    try {
                        instance.getStatWorld().recheckAllPlayers();
                    } catch (Exception ignored) {
                        // Silent fail - don't spam console
                    }
                }
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "[TabStats] " + ChatColor.WHITE + "API key cleared."));
            } else {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "[TabStats] " + ChatColor.WHITE + "API key is already empty."));
            }
        } else if (button.id == 2) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        } else if (button.id == 4) {
            this.showingApiKey = !this.showingApiKey;
            button.displayString = this.showingApiKey ? "Hide" : "Show";
            this.apiField.setRevealing(this.showingApiKey);
        } else if (button.id == 5) {
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

            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "[TabStats] " + ChatColor.WHITE + "Header/footer " + (newValue ? "enabled." : "disabled.")));
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

            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "[TabStats] " + ChatColor.WHITE + "Mod " + (newValue ? "enabled." : "disabled.")));
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        if (this.apiField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.apiField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, "TabStats", this.width / 2, this.titleY, 0xFFFFFF);
        drawString(this.fontRendererObj, "Hypixel API Key:", this.fieldX, this.fieldY - 12, 0xAAAAAA);
        this.apiField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String formatHeaderFooterLabel(boolean enabled) {
        return "Header/Footer: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled");
    }

    private String formatModToggleLabel(boolean enabled) {
        return "Mod: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled");
    }
}
