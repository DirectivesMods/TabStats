package tabstats.listener;

import tabstats.TabStats;
import tabstats.playerapi.HPlayer;
import tabstats.playerapi.StatWorld;
import tabstats.playerapi.api.stats.Stat;
import tabstats.render.StatsTab;
import tabstats.util.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameOverlayListener {
    private final StatsTab statsTab;
    private final Minecraft mc = Minecraft.getMinecraft();

    public GameOverlayListener() {
        this.statsTab = new StatsTab(this.mc, this.mc.ingameGUI);
    }
    
    /**
     * Gets the StatsTab instance for external access
     */
    public StatsTab getStatsTab() {
        return this.statsTab;
    }

    @SubscribeEvent
    public void onOverlayRender(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            return;
        }

        if (this.mc.thePlayer == null) {
            return;
        }

        event.setCanceled(true);

        Scoreboard scoreboard = this.mc.thePlayer.getWorldScoreboard();
        String gamemode = resolveGamemode(scoreboard);

        StatWorld statWorld = TabStats.getTabStats().getStatWorld();
        HPlayer theHPlayer = statWorld == null ? null : statWorld.getPlayerByUUID(this.mc.thePlayer.getUniqueID());

        List<Stat> gameStatTitleList;
        if (theHPlayer == null) {
            gameStatTitleList = Collections.emptyList();
        } else {
            List<Stat> stats = theHPlayer.getFormattedGameStats(gamemode);
            if (stats == null) {
                gameStatTitleList = new ArrayList<>();
            } else {
                gameStatTitleList = stats;
            }
        }

        int width = computeTabWidth(gameStatTitleList);
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(0);
        this.statsTab.renderNewPlayerlist(width, scoreboard, objective, gameStatTitleList, gamemode);
    }

    private String resolveGamemode(Scoreboard scoreboard) {
        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebarObjective == null) {
            return "BEDWARS";
        }

        String displayName = sidebarObjective.getDisplayName();
        String stripped = ChatColor.stripColor(displayName);
        if (stripped == null) {
            return "BEDWARS";
        }

        return stripped.replace(" ", "").toUpperCase();
    }

    private int computeTabWidth(List<Stat> stats) {
        int width = (StatsTab.headSize + 2) * 2 + this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + "[YOUTUBE] WWWWWWWWWWWWWWWW") + 10 - 10;

        for (Stat stat : stats) {
            if (stat == null) {
                continue;
            }

            String statName = stat.getStatName();
            if (statName == null) {
                continue;
            }

            width += this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + statName) + 10;
        }

        return width;
    }
}
