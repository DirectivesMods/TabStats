package tabstats.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import tabstats.TabStats;
import tabstats.config.ModConfig;
import tabstats.util.ChatColor;

import java.io.IOException;

abstract class AbstractApiKeyGui extends GuiScreen {
    private final GuiScreen parent;
    private MaskedGuiTextField apiField;
    private boolean showingApiKey;
    private int fieldX;
    private int fieldY;
    private int fieldWidth;
    private int titleY;

    protected AbstractApiKeyGui(GuiScreen parent) {
        this.parent = parent;
    }

    protected abstract String getScreenTitle();

    protected abstract String getChatKeyName();

    protected abstract String readStoredKey(ModConfig cfg);

    protected abstract void storeKey(ModConfig cfg, String value);

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();

        this.titleY = this.height / 2 - 60;
        this.fieldWidth = Math.max(220, this.fontRendererObj.getStringWidth("WWWWWWWW-WWWW-WWWW-WWWW-WWWWWWWWWWWW") + 10);
        this.fieldX = this.width / 2 - this.fieldWidth / 2;
        this.fieldY = this.titleY + 32;

        this.apiField = new MaskedGuiTextField(3, this.fontRendererObj, this.fieldX, this.fieldY, this.fieldWidth, 20);
        this.apiField.setMaxStringLength(50);
        this.apiField.setText(getInitialValue());
        this.apiField.setFocused(true);
        this.showingApiKey = false;
        this.apiField.setRevealing(this.showingApiKey);

        this.buttonList.add(new GuiButton(4, this.fieldX + this.fieldWidth + 10, this.fieldY, 60, 20, "Show"));

        int buttonSpacing = 4;
        int actionButtonWidth = (this.fieldWidth - buttonSpacing * 2) / 3;
        int buttonRowY = this.fieldY + 32;

        this.buttonList.add(new GuiButton(0, this.fieldX, buttonRowY, actionButtonWidth, 20, "Save"));
        this.buttonList.add(new GuiButton(1, this.fieldX + actionButtonWidth + buttonSpacing, buttonRowY, actionButtonWidth, 20, "Clear"));
        this.buttonList.add(new GuiButton(2, this.fieldX + (actionButtonWidth + buttonSpacing) * 2, buttonRowY, actionButtonWidth, 20, "Back"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                handleSave();
                break;
            case 1:
                handleClear();
                break;
            case 2:
                returnToParent();
                break;
            case 4:
                toggleVisibility(button);
                break;
            default:
                break;
        }
    }

    private void handleSave() {
        ModConfig cfg = ModConfig.getInstance();
        String newKey = normalize(this.apiField.getText());
        String currentKey = normalize(readStoredKey(cfg));

        if (!newKey.equals(currentKey)) {
            storeKey(cfg, newKey);
            cfg.save();
            this.apiField.setText(newKey);
            this.apiField.setRevealing(this.showingApiKey);
            triggerPlayerRefresh();
            addChatMessage(getChatKeyName() + " updated.");
        } else {
            addChatMessage(getChatKeyName() + " unchanged.");
        }
    }

    private void handleClear() {
        ModConfig cfg = ModConfig.getInstance();
        String currentKey = normalize(readStoredKey(cfg));
        if (!currentKey.isEmpty()) {
            storeKey(cfg, "");
            cfg.save();
            this.apiField.setText("");
            this.apiField.setRevealing(this.showingApiKey);
            triggerPlayerRefresh();
            addChatMessage(getChatKeyName() + " cleared.");
        } else {
            addChatMessage(getChatKeyName() + " is already empty.");
        }
    }

    private void toggleVisibility(GuiButton button) {
        this.showingApiKey = !this.showingApiKey;
        button.displayString = this.showingApiKey ? "Hide" : "Show";
        this.apiField.setRevealing(this.showingApiKey);
    }

    private void triggerPlayerRefresh() {
        TabStats instance = TabStats.getTabStats();
        if (instance == null || !instance.isModEnabled() || instance.getStatWorld() == null) {
            return;
        }

        try {
            instance.getStatWorld().recheckAllPlayers();
        } catch (Exception ignored) {
            // Silent fail - don't spam console
        }
    }

    private void addChatMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "[TabStats] " + ChatColor.WHITE + message));
    }

    private void returnToParent() {
        Minecraft.getMinecraft().displayGuiScreen(this.parent);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            returnToParent();
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
        drawCenteredString(this.fontRendererObj, getScreenTitle(), this.width / 2, this.titleY, 0xFFFFFF);
        drawString(this.fontRendererObj, getScreenTitle() + ":", this.fieldX, this.fieldY - 12, 0xAAAAAA);
        this.apiField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String getInitialValue() {
        ModConfig cfg = ModConfig.getInstance();
        String stored = readStoredKey(cfg);
        return stored == null ? "" : stored;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
