package tabstats.render;

import tabstats.TabStats;
import tabstats.playerapi.HPlayer;
import tabstats.playerapi.StatWorld;
import tabstats.playerapi.api.stats.Stat;
import tabstats.playerapi.api.stats.StatDouble;
import tabstats.playerapi.api.stats.StatInt;
import tabstats.playerapi.api.stats.StatString;
import tabstats.util.ChatColor;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;
import java.util.List;

public class StatsTab extends GuiPlayerTabOverlay {
    private static final Ordering<NetworkPlayerInfo> field_175252_a = Ordering.from(new StatsTab.PlayerComparator());
    private final Minecraft mc;
    private final GuiIngame guiIngame;
    private IChatComponent footer;
    private IChatComponent header;
    /** The amount of time since the playerlist was opened (went from not being rendered, to being rendered) */
    private long lastTimeOpened;
    /** Whether or not the playerlist is currently being rendered */
    public boolean tabBeingRendered;
    /* whether or not rank should come before color prefix */
    private boolean rankBeforePrefix = false;
    private final int entryHeight = 12;
    private final int backgroundBorderSize = 12;
    public static final int headSize = 12;
    
    // Scrolling variables for smooth tab list navigation
    private float scrollOffset = 0.0f;
    private float targetScrollOffset = 0.0f;
    private int maxVisiblePlayers = 0;
    private final float scrollSpeed = 0.2f; // Animation smoothness factor

    public StatsTab(Minecraft mcIn, GuiIngame guiIngameIn) {
        super(mcIn, guiIngameIn);
        this.mc = mcIn;
        this.guiIngame = guiIngameIn;
    }
    
    /**
     * Calculates the maximum number of players that can fit on screen before overflow
     * @param scaledRes The current scaled resolution
     * @param startingY The Y position where player entries start
     * @return Maximum players that fit on screen
     */
    private int calculateMaxVisiblePlayers(ScaledResolution scaledRes, int startingY) {
        // Available height = screen height - starting position - bottom padding
        int availableHeight = scaledRes.getScaledHeight() - startingY - this.backgroundBorderSize - this.entryHeight;
        
        // Each player entry takes entryHeight + 1 pixel spacing
        int entryTotalHeight = this.entryHeight + 1;
        
        // Calculate how many complete entries fit, minimum 1
        return Math.max(1, availableHeight / entryTotalHeight);
    }
    
    /**
     * Updates the scroll animation by interpolating towards the target offset
     */
    private void updateScrollAnimation() {
        // Smooth interpolation towards target
        float delta = targetScrollOffset - scrollOffset;
        scrollOffset += delta * scrollSpeed;
        
        // Snap to target when very close to avoid floating point precision issues
        if (Math.abs(delta) < 0.01f) {
            scrollOffset = targetScrollOffset;
        }
    }
    
    /**
     * Handles mouse wheel input for scrolling
     * @param wheelDelta The scroll wheel delta (positive = scroll up, negative = scroll down)
     * @param playerListSize Total number of players in the list
     */
    public void handleMouseWheel(int wheelDelta, int playerListSize) {
        if (maxVisiblePlayers >= playerListSize) {
            // No need to scroll if all players fit on screen
            return;
        }
        
        // Scroll by 1 player per wheel notch
        int scrollDirection = wheelDelta > 0 ? -1 : 1;
        targetScrollOffset += scrollDirection;
        
        // Clamp to valid bounds
        float maxScroll = Math.max(0, playerListSize - maxVisiblePlayers);
        targetScrollOffset = MathHelper.clamp_float(targetScrollOffset, 0, maxScroll);
    }
    
    /**
     * Resets scroll position to top
     */
    public void resetScroll() {
        scrollOffset = 0.0f;
        targetScrollOffset = 0.0f;
    }
    
    /**
     * Gets the current scroll offset for external use
     */
    public float getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * Gets whether the tab list is currently scrollable
     */
    public boolean isScrollable() {
        return maxVisiblePlayers > 0 && maxVisiblePlayers < 80; // 80 is current player limit
    }

    public void renderNewPlayerlist(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn, List<Stat> gameStatTitleList, String gamemode) {
        NetHandlerPlayClient nethandler = this.mc.thePlayer.sendQueue;
        StatWorld statWorld = TabStats.getTabStats().getStatWorld();
        List<NetworkPlayerInfo> playerList = field_175252_a.sortedCopy(nethandler.getPlayerInfoMap());
        
        // Filter out version 3 UUIDs from the tab list completely
        // Only show version 4 (definitely real players) and version 1 (potentially nicked players) and version 2 (potentially real players and some bots)
        playerList.removeIf(playerInfo -> {
            if (playerInfo.getGameProfile().getId() == null) return true;

            int uuidVersion = playerInfo.getGameProfile().getId().version();
            return uuidVersion != 4 && uuidVersion != 1 && uuidVersion != 2;
        });
        
        /* width of the player's name */
        int nameWidth = 0;
        /* width of the player's objective string */
        int objectiveWidth = 0;
        /* retrieve scaled resolution for accurate dimensions */
        ScaledResolution scaledRes = new ScaledResolution(this.mc);
        /* where the render should start on x plane */
        int startingX = scaledRes.getScaledWidth() / 2 - width / 2;
        int startingY = 20;

        /* this is kind of useless...as nameWidth and objectiveWidth aren't used */
        for (NetworkPlayerInfo playerInfo : playerList) {
            int strWidth = this.mc.fontRendererObj.getStringWidth(this.getPlayerName(playerInfo));
            nameWidth = Math.max(nameWidth, strWidth);

            if (scoreObjectiveIn != null && scoreObjectiveIn.getRenderType() != IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
                strWidth = this.mc.fontRendererObj.getStringWidth(" " + scoreboardIn.getValueFromObjective(playerInfo.getGameProfile().getName(), scoreObjectiveIn).getScoreScoreboard());
                objectiveWidth = Math.max(objectiveWidth, strWidth);
            }
        }

        /* initialize objectiveName outside of the below if block, so we can render it after the background */
        String objectiveName = "";
        /* meant for initializing the render of score objective */
        if (scoreObjectiveIn != null) {
            /* this is usually the raw name meant for internal usage */
            String objectiveRawName = WordUtils.capitalize(scoreObjectiveIn.getName().replace("_", " "));

            /* this is usually the formatted name meant for display */
            String objectiveDisplayname = WordUtils.capitalize(scoreObjectiveIn.getDisplayName().replace("_", ""));

            /* you can change this value to objectiveRawName, but I like using the displayname */
            objectiveName = objectiveDisplayname;
        }

        /* only grabs downwards of 80 players */
        playerList = playerList.subList(0, Math.min(playerList.size(), 80));
        int playerListSize = playerList.size();
        
        // Calculate maximum visible players based on screen height
        this.maxVisiblePlayers = calculateMaxVisiblePlayers(scaledRes, startingY);
        
        // Update scroll animation
        updateScrollAnimation();
        
        // Calculate visible player slice for rendering
        int startIndex = Math.max(0, Math.min((int) Math.floor(scrollOffset), playerListSize - maxVisiblePlayers));
        int endIndex = Math.min(playerListSize, startIndex + maxVisiblePlayers);
        List<NetworkPlayerInfo> visiblePlayers = playerList.subList(startIndex, endIndex);
        int visiblePlayerCount = visiblePlayers.size();

        /* the entire tab background - use visible player count for accurate sizing */
        drawRect(startingX - this.backgroundBorderSize - (objectiveName.isEmpty() ? 0 : 5 + this.mc.fontRendererObj.getStringWidth(objectiveName)), startingY - this.backgroundBorderSize, (scaledRes.getScaledWidth() / 2 + width / 2) + this.backgroundBorderSize,  (startingY + (visiblePlayerCount + 1) * (this.entryHeight + 1) - 1) + this.backgroundBorderSize, Integer.MIN_VALUE);

        /* draw an entry rect for the stat name title */
        drawRect(startingX, startingY, scaledRes.getScaledWidth() / 2 + width / 2, startingY + this.entryHeight, 553648127);

        /* Start with drawing the name and objective, as they will always be here and aren't inside of the Stat List */
        int statXSpacer = startingX + headSize + 2;
        this.mc.fontRendererObj.drawStringWithShadow(ChatColor.BOLD + "NAME", statXSpacer, startingY + (this.entryHeight / 2 - 4), ChatColor.WHITE.getRGB());
        this.mc.fontRendererObj.drawStringWithShadow(objectiveName, startingX - (this.mc.fontRendererObj.getStringWidth(objectiveName) + 5), startingY + (this.entryHeight / 2 - 4), ChatColor.WHITE.getRGB());

        /* adds longest name possible in pixels to statXSpacer since name's are way longer than stats */
        statXSpacer += this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + "[YOUTUBE] WWWWWWWWWWWWWWWW") + 10;

        /* loops through all the stats that should be displayed and renders their stat titles */
        for (Stat stat : gameStatTitleList) {
            String statName = stat.getStatName();
            String statLabel = statName == null ? "" : statName.toUpperCase();
            this.mc.fontRendererObj.drawStringWithShadow(ChatColor.BOLD + statLabel, statXSpacer, startingY + (this.entryHeight / 2 - 4), ChatColor.WHITE.getRGB());

            /* adds spacer for next stat (use uppercase label width to match header) */
            statXSpacer += this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + statLabel) + 10;
        }

        /* add entryHeight so it starts below the stat name title */
        int ySpacer = startingY + this.entryHeight + 1;
        int headerBottomY = ySpacer; // Save the Y position where content should start
        
        // Apply smooth scrolling offset to Y position
        float smoothScrollPixels = (scrollOffset - startIndex) * (this.entryHeight + 1);
        ySpacer -= (int) smoothScrollPixels;
        
        // Enable scissor test to clip content above the stat title row
        ScaledResolution scaledRes2 = new ScaledResolution(this.mc);
        int scaleFactor = scaledRes2.getScaleFactor();
        
        // Calculate scissor rectangle to clip anything that would render above headerBottomY
        int scissorX = 0;
        int scissorY = 0; // Start from bottom of screen
        int scissorWidth = scaledRes2.getScaledWidth() * scaleFactor;
        // Height should be from bottom of screen to the headerBottomY position
        int scissorHeight = (scaledRes2.getScaledHeight() - headerBottomY) * scaleFactor;
        
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        
        for (NetworkPlayerInfo playerInfo : visiblePlayers) {
            int xSpacer = startingX;
            /* entry background */
            drawRect(xSpacer, ySpacer, scaledRes.getScaledWidth() / 2 + width / 2, ySpacer + this.entryHeight, 553648127);

            /* ignore this, this is just preparing the gl canvas for rendering */
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            String name = this.getPlayerName(playerInfo);
            GameProfile gameProfile = playerInfo.getGameProfile();

            boolean flag = this.mc.isIntegratedServerRunning() || this.mc.getNetHandler().getNetworkManager().getIsencrypted();
            if (flag) {
                /* renders the player's face */
                EntityPlayer entityPlayer = this.mc.theWorld.getPlayerEntityByUUID(gameProfile.getId());
                boolean flag1 = entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.CAPE) && (gameProfile.getName().equals("Dinnerbone") || gameProfile.getName().equals("Grumm"));
                this.mc.getTextureManager().bindTexture(playerInfo.getLocationSkin());
                int u = 8 + (flag1 ? 8 : 0);
                int v = 8 * (flag1 ? -1 : 1);
                Gui.drawScaledCustomSizeModalRect(xSpacer, ySpacer, 8.0F, u, 8, v, headSize, headSize, 64.0F, 64.0F);

                if (entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.HAT)) {
                    Gui.drawScaledCustomSizeModalRect(xSpacer, ySpacer, 40.0F, u, 8, v, headSize, headSize, 64.0F, 64.0F);
                }

                /* adds x amount of pixels so that rendering name won't overlap with skin render */
                xSpacer += headSize + 2;
            }

            if (playerInfo.getGameType() == WorldSettings.GameType.SPECTATOR) {
                /* how you should render spectators */
            } else {
                /* how you should render everyone else */
                String displayName = playerInfo.getDisplayName() != null ? playerInfo.getDisplayName().getFormattedText() : null;
                HPlayer hPlayer = statWorld == null ? null : statWorld.getPlayerByIdentity(gameProfile.getId(), displayName, gameProfile.getName());
                if (hPlayer != null) {
                    /* render tabstats here */
                    if (hPlayer.isNicked()) {
                        name = this.getHPlayerName(playerInfo, hPlayer);
                    } else {
                        boolean obfuscated = name.contains(ChatColor.OBFUSCATE.toString());
                        if (obfuscated) {
                            ScorePlayerTeam liveTeam = playerInfo.getPlayerTeam();
                            String teamPrefix = liveTeam != null ? liveTeam.getColorPrefix() : "";
                            String color = teamPrefix.isEmpty() ? hPlayer.getPlayerRankColor() : teamPrefix;
                            name = color + hPlayer.getPlayerName();
                        } else {
                            name = this.getHPlayerName(playerInfo, hPlayer);
                        }
                    }

                    /* gets bedwars if the gamemode is not a game added to the hplayer's game list, otherwise, grab the game stats based on the scoreboard */
                    List<Stat> statList = hPlayer.getFormattedGameStats(gamemode);
                    if (statList == null || statList.isEmpty()) {
                        statList = hPlayer.getFormattedGameStats("BEDWARS");
                    }
                    /* start at the first stat */
                    int valueXSpacer = startingX + this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + "[YOUTUBE] WWWWWWWWWWWWWWWW") + 10 + headSize + 2;

                    for (Stat stat : statList) {
                        String statValue = "";

                        /* finds the exact stat type so it can properly retrieve the stat value */
                        switch (stat.getType()) {
                            case INT:
                                statValue = Integer.toString(((StatInt)stat).getValue());
                                break;
                            case DOUBLE:
                                statValue = Double.toString(((StatDouble)stat).getValue());
                                break;
                            case STRING:
                                statValue = ((StatString)stat).getValue();
                                break;
                        }

                        // draws the stats
                        this.mc.fontRendererObj.drawStringWithShadow(statValue, valueXSpacer, ySpacer + (this.entryHeight / 2 - 4), ChatColor.WHITE.getRGB());
                        /* match header spacing: use uppercase stat name width */
                        valueXSpacer += this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + (stat.getStatName() == null ? "" : stat.getStatName().toUpperCase())) + 10;
                    }
                }

                // draws the players name
                this.mc.fontRendererObj.drawStringWithShadow(name, xSpacer, ySpacer + (this.entryHeight / 2 - 4), -1);
            }

            if (scoreObjectiveIn != null & playerInfo.getGameType() != WorldSettings.GameType.SPECTATOR) {
                /* if player isn't a spectator and scoreobjective isn't null, render their score objective */

                /* not really sure how all objectives are drawn, but I understand HP and that's usually what Hypixel uses lol */
                this.drawScoreboardValues(scoreObjectiveIn, ySpacer, gameProfile.getName(), xSpacer, startingX - 5, playerInfo);
            }

            /* spaces each entry by the specified pixels */
            ySpacer += this.entryHeight + 1;
        }
        
        // Disable scissor test after rendering players
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        
        // Draw scroll indicators if there are more players above or below
        if (playerListSize > maxVisiblePlayers) {
            int indicatorX = scaledRes.getScaledWidth() / 2 + width / 2 - 10;
            
            // Up arrow if there are players above
            if (startIndex > 0) {
                String upArrow = ChatColor.WHITE + "▲";
                this.mc.fontRendererObj.drawStringWithShadow(upArrow, indicatorX, startingY + this.entryHeight + 2, ChatColor.WHITE.getRGB());
            }
            
            // Down arrow if there are players below  
            if (endIndex < playerListSize) {
                String downArrow = ChatColor.WHITE + "▼";
                int downY = startingY + this.entryHeight + 1 + (visiblePlayerCount * (this.entryHeight + 1)) - 10;
                this.mc.fontRendererObj.drawStringWithShadow(downArrow, indicatorX, downY, ChatColor.WHITE.getRGB());
            }
        }
    }

    private void drawScoreboardValues(ScoreObjective objectiveIn, int y, String playerName, int startX, int endX, NetworkPlayerInfo playerInfo) {
        int i = objectiveIn.getScoreboard().getValueFromObjective(playerName, objectiveIn).getScorePoints();

        if (objectiveIn.getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
            this.mc.getTextureManager().bindTexture(icons);

            if (this.lastTimeOpened == playerInfo.func_178855_p()) {
                if (i < playerInfo.func_178835_l()) {
                    playerInfo.func_178846_a(Minecraft.getSystemTime());
                    playerInfo.func_178844_b((long)(this.guiIngame.getUpdateCounter() + 20));
                } else if (i > playerInfo.func_178835_l()) {
                    playerInfo.func_178846_a(Minecraft.getSystemTime());
                    playerInfo.func_178844_b((long)(this.guiIngame.getUpdateCounter() + 10));
                }
            }

            if (Minecraft.getSystemTime() - playerInfo.func_178847_n() > 1000L || this.lastTimeOpened != playerInfo.func_178855_p()) {
                playerInfo.func_178836_b(i);
                playerInfo.func_178857_c(i);
                playerInfo.func_178846_a(Minecraft.getSystemTime());
            }

            playerInfo.func_178843_c(this.lastTimeOpened);
            playerInfo.func_178836_b(i);
            int j = MathHelper.ceiling_float_int((float)Math.max(i, playerInfo.func_178860_m()) / 2.0F);
            int k = Math.max(MathHelper.ceiling_float_int((float)(i / 2)), Math.max(MathHelper.ceiling_float_int((float)(playerInfo.func_178860_m() / 2)), 10));
            boolean flag = playerInfo.func_178858_o() > (long)this.guiIngame.getUpdateCounter() && (playerInfo.func_178858_o() - (long)this.guiIngame.getUpdateCounter()) / 3L % 2L == 1L;

            if (j > 0) {
                float f = Math.min((float)(endX - startX - 4) / (float)k, 9.0F);

                if (f > 3.0F) {
                    for (int l = j; l < k; ++l) {
                        this.drawTexturedModalRect((float)startX + (float)l * f, (float)y, flag ? 25 : 16, 0, 9, 9);
                    }

                    for (int j1 = 0; j1 < j; ++j1) {
                        this.drawTexturedModalRect((float)startX + (float)j1 * f, (float)y, flag ? 25 : 16, 0, 9, 9);

                        if (flag) {
                            if (j1 * 2 + 1 < playerInfo.func_178860_m()) {
                                this.drawTexturedModalRect((float)startX + (float)j1 * f, (float)y, 70, 0, 9, 9);
                            }

                            if (j1 * 2 + 1 == playerInfo.func_178860_m()) {
                                this.drawTexturedModalRect((float)startX + (float)j1 * f, (float)y, 79, 0, 9, 9);
                            }
                        }

                        if (j1 * 2 + 1 < i) {
                            this.drawTexturedModalRect((float)startX + (float)j1 * f, (float)y, j1 >= 10 ? 160 : 52, 0, 9, 9);
                        }

                        if (j1 * 2 + 1 == i) {
                            this.drawTexturedModalRect((float)startX + (float)j1 * f, (float)y, j1 >= 10 ? 169 : 61, 0, 9, 9);
                        }
                    }
                } else {
                    float f1 = MathHelper.clamp_float((float)i / 20.0F, 0.0F, 1.0F);
                    int i1 = (int)((1.0F - f1) * 255.0F) << 16 | (int)(f1 * 255.0F) << 8;
                    String s = "" + (float)i / 2.0F;

                    if (endX - this.mc.fontRendererObj.getStringWidth(s + "hp") >= startX) {
                        s = s + "hp";
                    }

                    this.mc.fontRendererObj.drawStringWithShadow(s, (float)((endX + startX) / 2 - this.mc.fontRendererObj.getStringWidth(s) / 2), (float)y, i1);
                }
            }
        } else {
            /* This is where Hypixel usually has Client draw Scoreboard Stats */

            String s1 = EnumChatFormatting.YELLOW + "" + i;
            this.mc.fontRendererObj.drawStringWithShadow(s1, (float)(endX - this.mc.fontRendererObj.getStringWidth(s1)), (float)y + (this.entryHeight / 2 - 4), 16777215);
//            drawRect(endX - this.mc.fontRendererObj.getStringWidth(objectiveIn.getDisplayName()), y, endX, y + this.entryHeight, 553648127);
        }
    }

    @SideOnly(Side.CLIENT)
    static class PlayerComparator implements Comparator<NetworkPlayerInfo> {
        private PlayerComparator() {
        }

        public int compare(NetworkPlayerInfo p_compare_1_, NetworkPlayerInfo p_compare_2_) {
            ScorePlayerTeam scoreplayerteam = p_compare_1_.getPlayerTeam();
            ScorePlayerTeam scoreplayerteam1 = p_compare_2_.getPlayerTeam();
            return ComparisonChain.start().compareTrueFirst(p_compare_1_.getGameType() != WorldSettings.GameType.SPECTATOR, p_compare_2_.getGameType() != WorldSettings.GameType.SPECTATOR).compare(scoreplayerteam != null ? scoreplayerteam.getRegisteredName() : "", scoreplayerteam1 != null ? scoreplayerteam1.getRegisteredName() : "").compare(p_compare_1_.getGameProfile().getName(), p_compare_2_.getGameProfile().getName()).result();
        }
    }

    /* Custom Player Name Formatter */
    public String getHPlayerName(NetworkPlayerInfo playerInfo, HPlayer hPlayer) {
        ScorePlayerTeam team = playerInfo.getPlayerTeam();
        String teamPrefix = team != null ? team.getColorPrefix() : "";
        String teamSuffix = team != null ? team.getColorSuffix() : "";
        String playerRank = hPlayer.getPlayerRank();

        if (hPlayer.isNicked()) {
            return teamPrefix + ChatColor.WHITE + "[" + ChatColor.RED + "NICKED" + ChatColor.WHITE + "] " + ChatColor.WHITE + playerInfo.getGameProfile().getName() + teamSuffix;
        }

        if (team != null) {
            /** remove [NON] as it's not shown in regular tab */
            String colorPrefix = teamPrefix;

            // Don't remove gray color for non-ranked players - preserve it
            if (ChatColor.stripColor(colorPrefix).contains(ChatColor.stripColor(playerRank)) && !playerRank.equals("§7")) {
                playerRank = "";
//                /* aqua colored MVP++ */
//                if (!colorPrefix.contains(playerRank) && colorPrefix.contains("++")) {
//                    colorPrefix = colorPrefix.replace(colorPrefix.substring(colorPrefix.indexOf("["), colorPrefix.indexOf("]") + 1), "").trim();
//                } else {
//                    colorPrefix = colorPrefix.replace(playerRank, "");
//                }
            }

            if (this.rankBeforePrefix) {
                return playerRank + colorPrefix + playerInfo.getGameProfile().getName() + teamSuffix;
            } else {
                return colorPrefix + playerRank + playerInfo.getGameProfile().getName() + teamSuffix;
            }
        }

        return this.getPlayerName(playerInfo);
    }
}
