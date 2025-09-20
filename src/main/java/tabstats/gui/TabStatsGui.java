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

    @Override
    public void initGui() {
        this.buttonList.clear();
        int centerX = this.width / 2;
        int y = this.height / 2 - 20;

    // GuiTextField constructor for 1.8.9: GuiTextField(id, FontRenderer, x, y, width, height)
    this.apiField = new GuiTextField(3, this.fontRendererObj, centerX - 100, y - 24, 200, 20);
        String current = ModConfig.getInstance().getApiKey();
        this.apiField.setText(current == null ? "" : current);

        this.buttonList.add(new GuiButton(0, centerX - 100, y + 8, 60, 20, "Save"));
        this.buttonList.add(new GuiButton(1, centerX - 30, y + 8, 60, 20, "Clear"));
        this.buttonList.add(new GuiButton(2, centerX + 40, y + 8, 60, 20, "Close"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ModConfig cfg = ModConfig.getInstance();
        if (button.id == 0) {
            String key = this.apiField.getText().trim();
            cfg.setApiKey(key);
            cfg.save();
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "TabStats API key updated!"));
        } else if (button.id == 1) {
            cfg.setApiKey("");
            cfg.save();
            this.apiField.setText("");
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ChatColor.GREEN + "TabStats API key cleared."));
        } else if (button.id == 2) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        this.apiField.textboxKeyTyped(typedChar, keyCode);
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
        drawCenteredString(this.fontRendererObj, "TabStats — API Key", this.width / 2, this.height / 2 - 48, 0xFFFFFF);
        drawString(this.fontRendererObj, "Enter your Hypixel API key below:", this.width / 2 - 100, this.height / 2 - 36, 0xAAAAAA);
        this.apiField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
