package tabstats.gui;

import tabstats.config.ModConfig;
import tabstats.util.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatComponentText;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;

public class TabStatsGui extends GuiScreen {
    private GuiTextField apiField;
    private String actualApiKey = "";
    private boolean showingApiKey = false;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int centerX = this.width / 2;
        int y = this.height / 2 - 20;

        // GuiTextField constructor for 1.8.9: GuiTextField(id, FontRenderer, x, y, width, height)
        // Made slightly smaller for better proportions
        this.apiField = new GuiTextField(3, this.fontRendererObj, centerX - 110, y - 24, 220, 20);
        this.apiField.setMaxStringLength(50); // Allow enough characters for UUID format
        String current = ModConfig.getInstance().getApiKey();
        this.actualApiKey = current == null ? "" : current;
        this.showingApiKey = false;
        updateFieldDisplay();

        // Add show/hide button next to the text field
        this.buttonList.add(new GuiButton(4, centerX + 120, y - 24, 40, 20, "Show"));

        this.buttonList.add(new GuiButton(0, centerX - 100, y + 8, 60, 20, "Save"));
        this.buttonList.add(new GuiButton(1, centerX - 30, y + 8, 60, 20, "Clear"));
        this.buttonList.add(new GuiButton(2, centerX + 40, y + 8, 60, 20, "Close"));
    }

    private void updateFieldDisplay() {
        if (showingApiKey) {
            this.apiField.setText(actualApiKey);
        } else {
            // Show asterisks but keep last 4 digits visible for identification
            String masked = "";
            if (actualApiKey.length() <= 4) {
                // If key is 4 characters or less, show it all
                masked = actualApiKey;
            } else {
                // Show asterisks for all but last 4 characters
                for (int i = 0; i < actualApiKey.length() - 4; i++) {
                    masked += "*";
                }
                masked += actualApiKey.substring(actualApiKey.length() - 4);
            }
            this.apiField.setText(masked);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ModConfig cfg = ModConfig.getInstance();
        if (button.id == 0) {
            String key = this.actualApiKey.trim();
            cfg.setApiKey(key);
            cfg.save();
            // Clear all cached stats when API key changes
            try {
                tabstats.TabStats.getTabStats().getStatWorld().refreshAllPlayers();
            } catch (Exception e) {
                // Silent fail - don't spam console
            }
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "TabStats API key updated!"));
        } else if (button.id == 1) {
            cfg.setApiKey("");
            cfg.save();
            this.actualApiKey = "";
            updateFieldDisplay();
            // Clear all cached stats when API key is cleared
            try {
                tabstats.TabStats.getTabStats().getStatWorld().refreshAllPlayers();
            } catch (Exception e) {
                // Silent fail - don't spam console
            }
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "TabStats API key cleared."));
        } else if (button.id == 2) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        } else if (button.id == 4) { // Show/Hide toggle
            this.showingApiKey = !this.showingApiKey;
            button.displayString = this.showingApiKey ? "Hide" : "Show";
            updateFieldDisplay();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        
        if (this.apiField.isFocused()) {
            // Handle paste - check for both Ctrl+V and Cmd+V
            boolean isPaste = false;
            if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL) || 
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL) ||
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMETA) ||
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RMETA)) {
                if (keyCode == org.lwjgl.input.Keyboard.KEY_V) {
                    isPaste = true;
                }
            }
            
            if (isPaste) {
                try {
                    String clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(java.awt.datatransfer.DataFlavor.stringFlavor).toString();
                    if (clipboard != null && clipboard.length() <= 50) {
                        actualApiKey = clipboard;
                        updateFieldDisplay();
                    }
                } catch (Exception e) {
                    // Ignore clipboard errors
                }
            } else if (keyCode == 14) { // Backspace
                if (actualApiKey.length() > 0) {
                    actualApiKey = actualApiKey.substring(0, actualApiKey.length() - 1);
                    updateFieldDisplay();
                }
            } else if (keyCode == 211) { // Delete key
                actualApiKey = "";
                updateFieldDisplay();
            } else if (typedChar >= 32 && typedChar < 127 && actualApiKey.length() < 50) { // Printable characters
                actualApiKey += typedChar;
                updateFieldDisplay();
            }
        } else {
            this.apiField.textboxKeyTyped(typedChar, keyCode);
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
        drawCenteredString(this.fontRendererObj, "TabStats — API Key", this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        drawString(this.fontRendererObj, "Enter your Hypixel API key below:", this.width / 2 - 110, this.height / 2 - 45, 0xAAAAAA);
        this.apiField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
