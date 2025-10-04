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

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int centerX = this.width / 2;
        int y = this.height / 2 - 20;

        int fieldPadding = 10;
        int desiredWidth = this.fontRendererObj.getStringWidth("WWWWWWWW-WWWW-WWWW-WWWW-WWWWWWWWWWWW") + fieldPadding;
        int fieldWidth = Math.max(220, desiredWidth);
        int fieldX = centerX - fieldWidth / 2;

        ModConfig cfg = ModConfig.getInstance();

        this.apiField = new MaskedGuiTextField(3, this.fontRendererObj, fieldX, y - 24, fieldWidth, 20);
        this.apiField.setMaxStringLength(50);
        String current = cfg.getApiKey();
        this.apiField.setText(current == null ? "" : current);
        this.showingApiKey = false;
        this.apiField.setRevealing(this.showingApiKey);

        int showButtonX = fieldX + fieldWidth + 10;
        this.buttonList.add(new GuiButton(4, showButtonX, y - 24, 40, 20, "Show"));
        this.buttonList.add(new GuiButton(0, centerX - 100, y + 8, 60, 20, "Save"));
        this.buttonList.add(new GuiButton(1, centerX - 30, y + 8, 60, 20, "Clear"));
        this.buttonList.add(new GuiButton(2, centerX + 40, y + 8, 60, 20, "Close"));

        boolean headerFooterEnabled = cfg.isRenderHeaderFooterEnabled();
        this.headerFooterButton = new GuiButton(5, centerX - 100, y + 34, 200, 20, formatHeaderFooterLabel(headerFooterEnabled));
        this.buttonList.add(this.headerFooterButton);
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
                try {
                    tabstats.TabStats.getTabStats().getStatWorld().recheckAllPlayers();
                } catch (Exception ignored) {
                    // Silent fail - don't spam console
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
                try {
                    tabstats.TabStats.getTabStats().getStatWorld().recheckAllPlayers();
                } catch (Exception ignored) {
                    // Silent fail - don't spam console
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
        drawCenteredString(this.fontRendererObj, "Hypixel API Key", this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        drawString(this.fontRendererObj, "Enter your Hypixel API key below:", this.apiField.xPosition, this.height / 2 - 45, 0xAAAAAA);
        this.apiField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String formatHeaderFooterLabel(boolean enabled) {
        return "Header/Footer: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled");
    }
}
