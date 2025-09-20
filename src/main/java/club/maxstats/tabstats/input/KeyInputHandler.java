package club.maxstats.tabstats.input;

import club.maxstats.tabstats.gui.TabStatsGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeyInputHandler {
    public static final KeyBinding OPEN_GUI = new KeyBinding("key.open_tabstats", Keyboard.KEY_K, "key.categories.tabstats");

    public static void init() {
        // Register keybinding (Architectury/Loom/Forge registration usually handled elsewhere in newer versions)
        // For 1.8.9, directly poll in-game via tick handlers or other input hooks. We'll keep a simple poll method.
    }

    public static void poll() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (OPEN_GUI.isPressed()) {
            mc.displayGuiScreen(new TabStatsGui());
        }
    }
}
