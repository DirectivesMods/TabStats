package tabstats.playerapi;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import tabstats.TabStats;
import tabstats.listener.GameOverlayListener;
import tabstats.util.ChatColor;
import tabstats.util.Handler;
import tabstats.util.NickDetector;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class WorldLoader extends StatWorld {
    private final Minecraft mc = Minecraft.getMinecraft();
    private World lastObservedWorld;
    private static final Pattern VALID_USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    public boolean loadOrRender(EntityPlayer player) {
        if (player == null) return false;
        
        UUID uuid = player.getUniqueID();
        if (uuid == null) {
            return false;
        }

        String baseName = player.getName();
        if (baseName == null || !VALID_USERNAME.matcher(baseName).matches()) {
            return false;
        }

        IChatComponent displayComponent = player.getDisplayName();
        if (displayComponent != null) {
            String stripped = ChatColor.stripColor(displayComponent.getFormattedText());
            if (stripped != null && stripped.trim().startsWith("[NPC]")) {
                return false;
            }
        }

        // Treat version 4 (real), version 2 (replay/offline/potential lobby inserts) and version 1 (potentially nicked) as valid players
        // Version 3 UUIDs remain filtered out as they belong to holograms and NPCs
        int version = uuid.version();
        return version == 4 || version == 2 || version == 1;
    }

    /**
     * Check nick status for version 1 UUIDs without making API calls
     */
    private void checkNickStatus(EntityPlayer entityPlayer) {
        Handler.asExecutor(() -> {
            UUID uuid = entityPlayer.getUniqueID();
            String playerUUID = uuid.toString();
            String playerName = entityPlayer.getName();
            
            // Create base HPlayer without stats
            HPlayer hPlayer = new HPlayer(playerUUID, playerName);

            // Check skin hash for nick detection
            String skinHash = extractSkinHashFromEntity(entityPlayer);
            boolean isDefinitelyNicked = NickDetector.isPlayerNicked(playerUUID, skinHash);

            // Handle uncertain nick detection (Version 1 UUID but unknown skin hash)
            if (!isDefinitelyNicked && NickDetector.isNickedUuid(playerUUID) && skinHash == null) {
                // Uncertain case - Version 1 UUID but skin not loaded yet, retry up to 200 ticks
                int ticks = nickRetryTicks.merge(uuid, 1, (a, b) -> a + b);
                if (ticks < 200) {
                    // Remove from stat assembly to allow retry on next tick
                    this.removeFromStatAssembly(uuid);
                    return;
                } else {
                    // Max retries reached - treat as uncertain, show name only (not nicked)
                    nickRetryTicks.remove(uuid);
                }
            }
            
            // Set nick status and add to world
            hPlayer.setNicked(isDefinitelyNicked);
            this.addPlayer(uuid, hPlayer);
            this.removeFromStatAssembly(uuid);
            nickRetryTicks.remove(uuid); // Clear retry counter on success
        });
    }

    /* populates and checks the stat world player cache every tick */
    @SubscribeEvent
    public void onTick(TickEvent event) {
        World currentWorld = mc.theWorld;

        if (currentWorld != lastObservedWorld) {
            lastObservedWorld = currentWorld;
            resetTabScroll();
        }

        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
            UUID uuid = entityPlayer.getUniqueID();

            if (!existedMoreThan5Seconds.contains(uuid)) {
                timeCheck.putIfAbsent(uuid, 0);

                int old = this.timeCheck.get(uuid);
                if (old > 100) {
                    if (!this.existedMoreThan5Seconds.contains(uuid)) {
                        this.existedMoreThan5Seconds.add(uuid);
                    }
                } else {
                    this.timeCheck.put(uuid, old + 1);
                }
            }

            if (!loadOrRender(entityPlayer)) {
                continue;
            }

            if (this.getWorldPlayers().containsKey(uuid) || this.statAssembly.contains(uuid)) {
                continue;
            }

            this.statAssembly.add(uuid);
            if (uuid.version() == 4 || uuid.version() == 2) {
                this.fetchStats(entityPlayer);
            } else if (uuid.version() == 1) {
                this.checkNickStatus(entityPlayer);
            }
            this.checkCacheSize();
        }
    }

    public void checkCacheSize() {
        int max = 500;
        if (getWorldPlayers().size() > max) {
            Set<UUID> safePlayers = new HashSet<>();
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                UUID uuid = player.getUniqueID();
                if (this.existedMoreThan5Seconds.contains(uuid)) {
                    safePlayers.add(uuid);
                }
            }

            this.existedMoreThan5Seconds.clear();
            this.existedMoreThan5Seconds.addAll(safePlayers);

            for (UUID playerUUID : new ArrayList<>(this.getWorldPlayers().keySet())) {
                if (!safePlayers.contains(playerUUID)) {
                    this.removePlayer(playerUUID);
                }
            }
        }
    }

    public void onDelete() {
        this.clearPlayers();
        this.existedMoreThan5Seconds.clear();
        lastObservedWorld = null;
        resetTabScroll();
    }

    private void resetTabScroll() {
        TabStats instance = TabStats.getTabStats();
        if (instance == null) {
            return;
        }

        GameOverlayListener overlayListener = instance.getGameOverlayListener();
        if (overlayListener == null || overlayListener.getStatsTab() == null) {
            return;
        }

        overlayListener.getStatsTab().resetScroll();
    }
}
