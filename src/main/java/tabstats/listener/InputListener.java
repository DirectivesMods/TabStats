package tabstats.listener;

import tabstats.TabStats;
import tabstats.config.ModConfig;
import tabstats.render.StatsTab;
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

        if (!ModConfig.getInstance().isModEnabled()) {
            return;
        }

        if (!mc.gameSettings.keyBindPlayerList.isKeyDown() || mc.currentScreen != null) {
            return;
        }

        event.setCanceled(true);

        if (mc.getNetHandler() == null) {
            return;
        }

        GameOverlayListener overlayListener = TabStats.getTabStats().getGameOverlayListener();
        if (overlayListener == null) {
            return;
        }

        StatsTab statsTab = overlayListener.getStatsTab();
        if (statsTab == null) {
            return;
        }

        int playerCount = mc.getNetHandler().getPlayerInfoMap().size();
        statsTab.handleMouseWheel(event.dwheel, playerCount);
    }
}
