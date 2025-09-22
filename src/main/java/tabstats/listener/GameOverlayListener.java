package tabstats.listener;

import tabstats.TabStats;
import tabstats.playerapi.HPlayer;
import tabstats.playerapi.api.stats.Stat;
import tabstats.render.StatsTab;
import tabstats.util.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class GameOverlayListener {
    private StatsTab statsTab;
    private Minecraft mc = Minecraft.getMinecraft();

    public GameOverlayListener() {
        this.statsTab = new StatsTab(this.mc, this.mc.ingameGUI);
    }

    @SubscribeEvent
    public void onOverlayRender(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            event.setCanceled(true);
            Scoreboard scoreboard = this.mc.thePlayer.getWorldScoreboard();

            String gamemode = "BEDWARS";
            if (scoreboard.getObjectiveInDisplaySlot(1) != null) {
                gamemode = ChatColor.stripColor(scoreboard.getObjectiveInDisplaySlot(1).getDisplayName()).replace(" ", "").toUpperCase();
            }

            HPlayer theHPlayer = TabStats.getTabStats().getStatWorld().getPlayerByUUID(Minecraft.getMinecraft().thePlayer.getUniqueID());

            List<Stat> gameStatTitleList = new ArrayList<>();
            if (theHPlayer != null) {
                gameStatTitleList = theHPlayer.getFormattedGameStats(gamemode);
            }

            int width = (StatsTab.headSize + 2) * 2 + this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + "[YOUTUBE] WWWWWWWWWWWWWWWW") + 10 - 10;

            for (Stat stat : gameStatTitleList) {
                width += this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + stat.getStatName()) + 10;
            }

            this.statsTab.renderNewPlayerlist(width, scoreboard, scoreboard.getObjectiveInDisplaySlot(0), gameStatTitleList, gamemode);
        }
    }
}
