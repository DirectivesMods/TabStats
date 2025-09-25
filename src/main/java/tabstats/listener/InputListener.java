package tabstats.listener;

import tabstats.TabStats;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Mouse;

public class InputListener {
    private final Minecraft mc = Minecraft.getMinecraft();
    
    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        // Only handle scroll when tab list is open
        if (mc.gameSettings.keyBindPlayerList.isKeyDown() && mc.currentScreen == null) {
            int wheelDelta = Mouse.getEventDWheel();
            
            if (wheelDelta != 0) {
                // Get the current player count for scroll bounds
                int playerCount = mc.getNetHandler().getPlayerInfoMap().size();
                
                // Pass wheel event to the stats tab for handling
                GameOverlayListener overlayListener = TabStats.getTabStats().getGameOverlayListener();
                if (overlayListener != null && overlayListener.getStatsTab() != null) {
                    overlayListener.getStatsTab().handleMouseWheel(wheelDelta, playerCount);
                }
            }
        }
    }
}