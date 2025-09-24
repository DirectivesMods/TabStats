package tabstats.gui;

import tabstats.config.ModConfig;
import tabstats.util.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatComponentText;
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

        this.apiField = new GuiTextField(3, this.fontRendererObj, centerX - 110, y - 24, 220, 20);
        this.apiField.setMaxStringLength(50);
        String current = ModConfig.getInstance().getApiKey();
        this.actualApiKey = current == null ? "" : current;
        this.showingApiKey = false;
        updateFieldDisplay();

        this.buttonList.add(new GuiButton(4, centerX + 120, y - 24, 40, 20, "Show"));
        this.buttonList.add(new GuiButton(0, centerX - 100, y + 8, 60, 20, "Save"));
        this.buttonList.add(new GuiButton(1, centerX - 30, y + 8, 60, 20, "Clear"));
        this.buttonList.add(new GuiButton(2, centerX + 40, y + 8, 60, 20, "Close"));
    }

    private void updateFieldDisplay() {
        if (showingApiKey) {
            this.apiField.setText(actualApiKey);
        } else {
            String masked = "";
            if (actualApiKey.length() <= 4) {
                masked = actualApiKey;
            } else {
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
            String currentKey = cfg.getApiKey();
            // Only update and refresh if the key actually changed
            if (!key.equals(currentKey == null ? "" : currentKey)) {
                cfg.setApiKey(key);
                cfg.save();
                try {
                    tabstats.TabStats.getTabStats().getStatWorld().refreshAllPlayers();
                } catch (Exception ignored) {
                    // Silent fail - don't spam console
                }
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "TabStats API key updated!"));
            } else {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.YELLOW + "API key unchanged."));
            }
        } else if (button.id == 1) {
            String currentKey = cfg.getApiKey();
            // Only clear and refresh if there's actually a key to clear
            if (currentKey != null && !currentKey.trim().isEmpty()) {
                cfg.setApiKey("");
                cfg.save();
                this.actualApiKey = "";
                updateFieldDisplay();
                try {
                    tabstats.TabStats.getTabStats().getStatWorld().refreshAllPlayers();
                } catch (Exception ignored) {
                    // Silent fail - don't spam console
                }
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "TabStats API key cleared."));
            } else {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.YELLOW + "API key is already empty."));
            }
        } else if (button.id == 2) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        } else if (button.id == 4) {
            this.showingApiKey = !this.showingApiKey;
            button.displayString = this.showingApiKey ? "Hide" : "Show";
            updateFieldDisplay();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        
        if (this.apiField.isFocused()) {
            boolean isModifierPressed = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL) || 
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL) ||
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMETA) ||
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RMETA);
            
            if (keyCode == 203) {
                if (isModifierPressed) {
                    this.apiField.setCursorPositionZero();
                } else {
                    int currentPos = this.apiField.getCursorPosition();
                    if (currentPos > 0) {
                        this.apiField.setCursorPosition(currentPos - 1);
                    }
                }
                return;
            } else if (keyCode == 205) {
                if (isModifierPressed) {
                    this.apiField.setCursorPosition(this.apiField.getText().length());
                } else {
                    int currentPos = this.apiField.getCursorPosition();
                    int maxPos = this.apiField.getText().length();
                    if (currentPos < maxPos) {
                        this.apiField.setCursorPosition(currentPos + 1);
                    }
                }
                return;
            } else if (keyCode == 199) {
                this.apiField.setCursorPositionZero();
                return;
            } else if (keyCode == 207) {
                this.apiField.setCursorPosition(this.apiField.getText().length());
                return;
            }
            
            boolean isPaste = false;
            boolean isCopy = false;
            boolean isSelectAll = false;
            boolean isCtrlBackspace = false;
                
            if (isModifierPressed) {
                if (keyCode == org.lwjgl.input.Keyboard.KEY_V) {
                    isPaste = true;
                } else if (keyCode == org.lwjgl.input.Keyboard.KEY_C) {
                    isCopy = true;
                } else if (keyCode == org.lwjgl.input.Keyboard.KEY_A) {
                    isSelectAll = true;
                } else if (keyCode == 14) { // Ctrl+Backspace
                    isCtrlBackspace = true;
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
                } catch (Exception ignored) {
                    // Silent fail on clipboard access
                }
            } else if (isCopy) {
                try {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(actualApiKey);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                } catch (Exception ignored) {
                    // Silent fail on clipboard access
                }
            } else if (isSelectAll) {
                this.apiField.setCursorPositionZero();
                this.apiField.setSelectionPos(this.apiField.getText().length());
            } else if (isCtrlBackspace) {
                int cursorPos = this.apiField.getCursorPosition();
                int displayTextLength = this.apiField.getText().length();
                int actualTextLength = actualApiKey.length();
                
                if (actualTextLength > 0 && cursorPos > 0) {
                    int actualCursorPos;
                    if (showingApiKey) {
                        actualCursorPos = Math.min(cursorPos, actualTextLength);
                    } else {
                        if (cursorPos >= displayTextLength) {
                            actualCursorPos = actualTextLength;
                        } else {
                            actualCursorPos = Math.min(cursorPos, actualTextLength);
                        }
                    }
                    
                    if (actualCursorPos > 0) {
                        actualApiKey = actualApiKey.substring(actualCursorPos);
                        updateFieldDisplay();
                        this.apiField.setCursorPositionZero();
                    }
                }
            } else if (keyCode == 14) {
                if (this.apiField.getSelectedText() != null && !this.apiField.getSelectedText().isEmpty()) {
                    actualApiKey = "";
                    updateFieldDisplay();
                } else if (actualApiKey.length() > 0) {
                    actualApiKey = actualApiKey.substring(0, actualApiKey.length() - 1);
                    updateFieldDisplay();
                }
            } else if (keyCode == 211) {
                if (this.apiField.getSelectedText() != null && !this.apiField.getSelectedText().isEmpty()) {
                    actualApiKey = "";
                    updateFieldDisplay();
                } else {
                    actualApiKey = "";
                    updateFieldDisplay();
                }
            } else if (typedChar >= 32 && typedChar < 127) {
                if (this.apiField.getSelectedText() != null && !this.apiField.getSelectedText().isEmpty()) {
                    actualApiKey = String.valueOf(typedChar);
                } else if (actualApiKey.length() < 50) {
                    actualApiKey += typedChar;
                }
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
