package tabstats.listener;

import tabstats.gui.TabStatsGui;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class GuiOpenListener {
    private static boolean shouldOpenGui = false;
    
    public static void requestGuiOpen() {
        shouldOpenGui = true;
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && shouldOpenGui) {
            shouldOpenGui = false;
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null && mc.thePlayer != null && mc.currentScreen == null) {
                mc.displayGuiScreen(new TabStatsGui());
            }
        }
    }
}