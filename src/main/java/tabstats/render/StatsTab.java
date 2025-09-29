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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class StatsTab extends GuiPlayerTabOverlay {
    private static final Ordering<NetworkPlayerInfo> field_175252_a = Ordering.from(new StatsTab.PlayerComparator());
    private static final int MAX_TAB_PLAYERS = 80;
    private static final Pattern VALID_USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
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
    private int lastPlayerListSize = 0;

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
        if (maxVisiblePlayers <= 0) {
            return;
        }

        int effectiveListSize = this.lastPlayerListSize > 0 ? this.lastPlayerListSize : Math.min(playerListSize, MAX_TAB_PLAYERS);

        if (maxVisiblePlayers >= effectiveListSize) {
            // No need to scroll if all players fit on screen
            return;
        }

        // Scroll by 1 player per wheel notch
        int scrollDirection = wheelDelta > 0 ? -1 : 1;
        targetScrollOffset += scrollDirection;

        // Clamp to valid bounds derived from the last rendered list size
        float maxScroll = Math.max(0, effectiveListSize - maxVisiblePlayers);
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
        return maxVisiblePlayers > 0 && maxVisiblePlayers < MAX_TAB_PLAYERS;
    }

    public void renderNewPlayerlist(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn, List<Stat> gameStatTitleList, String gamemode) {
        NetHandlerPlayClient netHandler = this.mc.thePlayer.sendQueue;
        StatWorld statWorld = TabStats.getTabStats().getStatWorld();
        List<NetworkPlayerInfo> playerList = collectEligiblePlayers(netHandler, statWorld);

        ScaledResolution scaledRes = new ScaledResolution(this.mc);
        int startingX = scaledRes.getScaledWidth() / 2 - width / 2;
        int startingY = 20;

        String objectiveName = "";
        if (scoreObjectiveIn != null) {
            String objectiveDisplayName = WordUtils.capitalize(scoreObjectiveIn.getDisplayName().replace("_", ""));
            objectiveName = objectiveDisplayName;
        }

        playerList = playerList.subList(0, Math.min(playerList.size(), MAX_TAB_PLAYERS));
        int playerListSize = playerList.size();
        this.lastPlayerListSize = playerListSize;

        this.maxVisiblePlayers = calculateMaxVisiblePlayers(scaledRes, startingY);

        float maxScroll = Math.max(0, playerListSize - maxVisiblePlayers);
        targetScrollOffset = MathHelper.clamp_float(targetScrollOffset, 0.0f, maxScroll);
        if (scrollOffset < 0.0f) {
            scrollOffset = 0.0f;
        }

        updateScrollAnimation();

        int startIndex = Math.max(0, Math.min((int)Math.floor(scrollOffset), playerListSize - maxVisiblePlayers));
        int endIndex = Math.min(playerListSize, startIndex + maxVisiblePlayers);
        List<NetworkPlayerInfo> visiblePlayers = playerList.subList(startIndex, endIndex);
        int visiblePlayerCount = visiblePlayers.size();

        int textBaselineOffset = this.entryHeight / 2 - 4;
        int backgroundLeft = startingX - this.backgroundBorderSize - (objectiveName.isEmpty() ? 0 : 5 + this.mc.fontRendererObj.getStringWidth(objectiveName));
        int backgroundRight = scaledRes.getScaledWidth() / 2 + width / 2 + this.backgroundBorderSize;
        int backgroundBottom = startingY + (visiblePlayerCount + 1) * (this.entryHeight + 1) - 1 + this.backgroundBorderSize;
        drawRect(backgroundLeft, startingY - this.backgroundBorderSize, backgroundRight, backgroundBottom, Integer.MIN_VALUE);

        drawRect(startingX, startingY, scaledRes.getScaledWidth() / 2 + width / 2, startingY + this.entryHeight, 553648127);

        int statXSpacer = startingX + headSize + 2;
        this.mc.fontRendererObj.drawStringWithShadow(ChatColor.BOLD + "NAME", statXSpacer, startingY + textBaselineOffset, ChatColor.WHITE.getRGB());
        this.mc.fontRendererObj.drawStringWithShadow(objectiveName, startingX - (this.mc.fontRendererObj.getStringWidth(objectiveName) + 5), startingY + textBaselineOffset, ChatColor.WHITE.getRGB());

        statXSpacer += this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + "[YOUTUBE] WWWWWWWWWWWWWWWW") + 10;

        for (Stat stat : gameStatTitleList) {
            String statName = stat.getStatName();
            String statLabel = statName == null ? "" : statName.toUpperCase();
            this.mc.fontRendererObj.drawStringWithShadow(ChatColor.BOLD + statLabel, statXSpacer, startingY + textBaselineOffset, ChatColor.WHITE.getRGB());
            statXSpacer += this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + statLabel) + 10;
        }

        int ySpacer = startingY + this.entryHeight + 1;
        int headerBottomY = ySpacer;

        float fractionalOffset = MathHelper.clamp_float(scrollOffset - startIndex, 0.0f, 0.999f);
        int smoothScrollPixels = (int)(fractionalOffset * (this.entryHeight + 1));
        ySpacer -= smoothScrollPixels;

        int scaleFactor = scaledRes.getScaleFactor();
        int scissorWidth = scaledRes.getScaledWidth() * scaleFactor;
        int scissorHeight = (scaledRes.getScaledHeight() - headerBottomY) * scaleFactor;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, 0, scissorWidth, scissorHeight);

        for (NetworkPlayerInfo playerInfo : visiblePlayers) {
            int xSpacer = startingX;
            drawRect(xSpacer, ySpacer, scaledRes.getScaledWidth() / 2 + width / 2, ySpacer + this.entryHeight, 553648127);

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            String name = this.getPlayerName(playerInfo);
            GameProfile gameProfile = playerInfo.getGameProfile();

            boolean renderSkin = this.mc.isIntegratedServerRunning() || this.mc.getNetHandler().getNetworkManager().getIsencrypted();
            if (renderSkin) {
                EntityPlayer entityPlayer = this.mc.theWorld.getPlayerEntityByUUID(gameProfile.getId());
                boolean upsideDown = entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.CAPE) && ("Dinnerbone".equals(gameProfile.getName()) || "Grumm".equals(gameProfile.getName()));
                this.mc.getTextureManager().bindTexture(playerInfo.getLocationSkin());
                int u = 8 + (upsideDown ? 8 : 0);
                int v = 8 * (upsideDown ? -1 : 1);
                Gui.drawScaledCustomSizeModalRect(xSpacer, ySpacer, 8.0F, u, 8, v, headSize, headSize, 64.0F, 64.0F);

                if (entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.HAT)) {
                    Gui.drawScaledCustomSizeModalRect(xSpacer, ySpacer, 40.0F, u, 8, v, headSize, headSize, 64.0F, 64.0F);
                }

                xSpacer += headSize + 2;
            }

            if (playerInfo.getGameType() != WorldSettings.GameType.SPECTATOR) {
                String displayName = playerInfo.getDisplayName() != null ? playerInfo.getDisplayName().getFormattedText() : null;
                HPlayer hPlayer = statWorld == null ? null : statWorld.getPlayerByIdentity(gameProfile.getId(), displayName, gameProfile.getName());
                if (hPlayer != null) {
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

                    List<Stat> statList = hPlayer.getFormattedGameStats(gamemode);
                    if (statList == null || statList.isEmpty()) {
                        statList = hPlayer.getFormattedGameStats("BEDWARS");
                    }

                    int valueXSpacer = startingX + this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + "[YOUTUBE] WWWWWWWWWWWWWWWW") + 10 + headSize + 2;

                    for (Stat stat : statList) {
                        String statValue = "";
                        switch (stat.getType()) {
                            case INT:
                                statValue = Integer.toString(((StatInt) stat).getValue());
                                break;
                            case DOUBLE:
                                statValue = Double.toString(((StatDouble) stat).getValue());
                                break;
                            case STRING:
                                statValue = ((StatString) stat).getValue();
                                break;
                        }

                        this.mc.fontRendererObj.drawStringWithShadow(statValue, valueXSpacer, ySpacer + textBaselineOffset, ChatColor.WHITE.getRGB());
                        valueXSpacer += this.mc.fontRendererObj.getStringWidth(ChatColor.BOLD + (stat.getStatName() == null ? "" : stat.getStatName().toUpperCase())) + 10;
                    }
                }

                this.mc.fontRendererObj.drawStringWithShadow(name, xSpacer, ySpacer + textBaselineOffset, -1);
            }

            if (scoreObjectiveIn != null && playerInfo.getGameType() != WorldSettings.GameType.SPECTATOR) {
                this.drawScoreboardValues(scoreObjectiveIn, ySpacer, gameProfile.getName(), xSpacer, startingX - 5, playerInfo);
            }

            ySpacer += this.entryHeight + 1;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (playerListSize > maxVisiblePlayers) {
            int indicatorX = scaledRes.getScaledWidth() / 2 + width / 2 - 10;

            if (startIndex > 0) {
                String upArrow = ChatColor.WHITE + "▲";
                this.mc.fontRendererObj.drawStringWithShadow(upArrow, indicatorX, startingY + this.entryHeight + 2, ChatColor.WHITE.getRGB());
            }

            if (endIndex < playerListSize) {
                String downArrow = ChatColor.WHITE + "▼";
                int downY = startingY + this.entryHeight + 1 + (visiblePlayerCount * (this.entryHeight + 1)) - 10;
                this.mc.fontRendererObj.drawStringWithShadow(downArrow, indicatorX, downY, ChatColor.WHITE.getRGB());
            }
        }
    }

    private List<NetworkPlayerInfo> collectEligiblePlayers(NetHandlerPlayClient netHandler, StatWorld statWorld) {
        List<NetworkPlayerInfo> sortedPlayers = field_175252_a.sortedCopy(netHandler.getPlayerInfoMap());
        List<NetworkPlayerInfo> filtered = new ArrayList<>(sortedPlayers.size());
        for (NetworkPlayerInfo info : sortedPlayers) {
            if (isEligiblePlayer(info, statWorld)) {
                filtered.add(info);
            }
        }
        return filtered;
    }

    private boolean isEligiblePlayer(NetworkPlayerInfo playerInfo, StatWorld statWorld) {
        GameProfile profile = playerInfo.getGameProfile();
        if (profile == null) {
            return false;
        }

        UUID playerUuid = profile.getId();
        if (playerUuid == null) {
            return false;
        }

        int uuidVersion = playerUuid.version();
        if (uuidVersion != 4 && uuidVersion != 1 && uuidVersion != 2) {
            return false;
        }

        if (uuidVersion == 2) {
            if (statWorld == null || statWorld.getPlayerByUUID(playerUuid) == null) {
                return false;
            }
        }

        String strippedName = ChatColor.stripColor(this.getPlayerName(playerInfo));
        if (strippedName != null && strippedName.trim().startsWith("[NPC]")) {
            return false;
        }

        String profileName = profile.getName();
        return profileName != null && VALID_USERNAME.matcher(profileName).matches();
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
