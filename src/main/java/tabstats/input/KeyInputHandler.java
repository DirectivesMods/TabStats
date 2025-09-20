package tabstats.input;

import tabstats.gui.TabStatsGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeyInputHandler {
    public static final KeyBinding OPEN_GUI = new KeyBinding("key.open_tabstats", Keyboard.KEY_K, "key.categories.tabstats");

    public static void poll() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        
        if (OPEN_GUI.isPressed()) {
            mc.displayGuiScreen(new TabStatsGui());
        }
    }
}
