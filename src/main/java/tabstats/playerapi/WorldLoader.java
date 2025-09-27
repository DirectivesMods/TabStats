package tabstats.playerapi;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import tabstats.TabStats;
import tabstats.listener.GameOverlayListener;
import tabstats.util.Handler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorldLoader extends StatWorld {
    Minecraft mc = Minecraft.getMinecraft();
    private World lastObservedWorld;

    public boolean loadOrRender(EntityPlayer player) {
        if (player == null) return false;
        
        UUID uuid = player.getUniqueID();

        // Treat version 4 (real), version 2 (replay/offline/potential real player spoofed) and version 1 (potentially nicked) as valid players
        // Version 3 UUIDs remain filtered out as they belong to NPCs like holograms
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
            boolean isDefinitelyNicked = tabstats.util.NickDetector.isPlayerNicked(playerUUID, skinHash);
            
            // Handle uncertain nick detection (Version 1 UUID but unknown skin hash)
            if (!isDefinitelyNicked && tabstats.util.NickDetector.isNickedUuid(playerUUID) && skinHash == null) {
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

        if (mc.theWorld != null && mc.thePlayer != null) {
            for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
                final UUID uuid = entityPlayer.getUniqueID(); // Cache UUID once per player per tick
                
                if (!existedMoreThan5Seconds.contains(uuid)) {
                    if (!this.timeCheck.containsKey(uuid))
                        this.timeCheck.put(uuid, 0);

                    int old = this.timeCheck.get(uuid);
                    if (old > 100) {
                        if (!this.existedMoreThan5Seconds.contains(uuid))
                            this.existedMoreThan5Seconds.add(uuid);
                    } else {
                        this.timeCheck.put(uuid, old + 1);
                    }
                }
                if (loadOrRender(entityPlayer)) {
                    if (!this.getWorldPlayers().containsKey(uuid) && !this.statAssembly.contains(uuid)) {
                        this.statAssembly.add(uuid);
                        if (uuid.version() == 4 || uuid.version() == 2) {
                            // Version 4 UUIDs (real players) and version 2 UUIDs (replay/offline) - fetch stats from API
                            this.fetchStats(entityPlayer);
                        } else if (uuid.version() == 1) {
                            // Version 1 UUIDs (potentially nicked players) - check nick detection only, no API calls
                            this.checkNickStatus(entityPlayer);
                        }
                        this.checkCacheSize();
                    }
                }
            }
        }
    }

    public void checkCacheSize() {
        int max = 500;
        if (getWorldPlayers().size() > max) {
            List<UUID> safePlayers = new ArrayList<>();
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                UUID uuid = player.getUniqueID();
                if (this.existedMoreThan5Seconds.contains(uuid)) {
                    safePlayers.add(uuid);
                }
            }

            this.existedMoreThan5Seconds.clear();
            this.existedMoreThan5Seconds.addAll(safePlayers);

            for (UUID playerUUID : this.getWorldPlayers().keySet()) {
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
