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
            cfg.setApiKey(key);
            cfg.save();
            try {
                tabstats.TabStats.getTabStats().getStatWorld().refreshAllPlayers();
            } catch (Exception e) {
                // Silent fail
            }
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "TabStats API key updated!"));
        } else if (button.id == 1) {
            cfg.setApiKey("");
            cfg.save();
            this.actualApiKey = "";
            updateFieldDisplay();
            try {
                tabstats.TabStats.getTabStats().getStatWorld().refreshAllPlayers();
            } catch (Exception e) {
                // Silent fail
            }
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "TabStats API key cleared."));
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
        if (keyCode == 1) { // ESC
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        
        if (this.apiField.isFocused()) {
            // Check for modifier keys first
            boolean isModifierPressed = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL) || 
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL) ||
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMETA) ||
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RMETA);
            
            // Handle arrow keys - check for Ctrl+Arrow first, then regular arrows
            if (keyCode == 203) { // Left arrow key
                if (isModifierPressed) {
                    // Ctrl+Left: Jump to beginning
                    this.apiField.setCursorPositionZero();
                } else {
                    // Regular Left: Move one position left
                    int currentPos = this.apiField.getCursorPosition();
                    if (currentPos > 0) {
                        this.apiField.setCursorPosition(currentPos - 1);
                    }
                }
                return; // Don't process other keys
            } else if (keyCode == 205) { // Right arrow key  
                if (isModifierPressed) {
                    // Ctrl+Right: Jump to end
                    this.apiField.setCursorPosition(this.apiField.getText().length());
                } else {
                    // Regular Right: Move one position right
                    int currentPos = this.apiField.getCursorPosition();
                    int maxPos = this.apiField.getText().length();
                    if (currentPos < maxPos) {
                        this.apiField.setCursorPosition(currentPos + 1);
                    }
                }
                return; // Don't process other keys
            } else if (keyCode == 199) { // Home key
                this.apiField.setCursorPositionZero();
                return;
            } else if (keyCode == 207) { // End key
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
                } catch (Exception e) {
                    // Ignore clipboard errors
                }
            } else if (isCopy) {
                try {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(actualApiKey);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                } catch (Exception e) {
                    // Ignore clipboard errors
                }
            } else if (isSelectAll) {
                this.apiField.setCursorPositionZero();
                this.apiField.setSelectionPos(this.apiField.getText().length());
            } else if (isCtrlBackspace) {
                // Ctrl+Backspace: Delete everything to the left of cursor
                int cursorPos = this.apiField.getCursorPosition();
                int displayTextLength = this.apiField.getText().length();
                int actualTextLength = actualApiKey.length();
                
                if (actualTextLength > 0 && cursorPos > 0) {
                    // Calculate approximate cursor position in actual text
                    int actualCursorPos;
                    if (showingApiKey) {
                        actualCursorPos = Math.min(cursorPos, actualTextLength);
                    } else {
                        // For masked text, if cursor is at end, it's at actual end
                        if (cursorPos >= displayTextLength) {
                            actualCursorPos = actualTextLength;
                        } else {
                            // Cursor is somewhere in the masked portion
                            actualCursorPos = Math.min(cursorPos, actualTextLength);
                        }
                    }
                    
                    // Delete everything before the cursor position
                    if (actualCursorPos > 0) {
                        actualApiKey = actualApiKey.substring(actualCursorPos);
                        updateFieldDisplay();
                        // Set cursor to the beginning since we deleted everything before it
                        this.apiField.setCursorPositionZero();
                    }
                }
            } else if (keyCode == 14) {
                // Check if text is selected (selection exists)
                if (this.apiField.getSelectedText() != null && !this.apiField.getSelectedText().isEmpty()) {
                    // Clear all text when backspace is pressed with selection
                    actualApiKey = "";
                    updateFieldDisplay();
                } else if (actualApiKey.length() > 0) {
                    actualApiKey = actualApiKey.substring(0, actualApiKey.length() - 1);
                    updateFieldDisplay();
                }
            } else if (keyCode == 211) {
                // Check if text is selected for delete key too
                if (this.apiField.getSelectedText() != null && !this.apiField.getSelectedText().isEmpty()) {
                    // Clear all text when delete is pressed with selection
                    actualApiKey = "";
                    updateFieldDisplay();
                } else {
                    actualApiKey = "";
                    updateFieldDisplay();
                }
            } else if (typedChar >= 32 && typedChar < 127) {
                // Check if text is selected - if so, replace all text
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
