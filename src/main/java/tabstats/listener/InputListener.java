package tabstats.listener;

import tabstats.TabStats;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class InputListener {
    private final Minecraft mc = Minecraft.getMinecraft();
    
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.dwheel == 0) {
            return;
        }

        boolean playerListHeld = mc.gameSettings.keyBindPlayerList.isKeyDown();
        if (playerListHeld && mc.currentScreen == null) {
            event.setCanceled(true);

            // pass wheel event to the stats tab for handling
            int playerCount = mc.getNetHandler().getPlayerInfoMap().size();
            GameOverlayListener overlayListener = TabStats.getTabStats().getGameOverlayListener();
            if (overlayListener != null && overlayListener.getStatsTab() != null) {
                overlayListener.getStatsTab().handleMouseWheel(event.dwheel, playerCount);
            }
        }
    }
}