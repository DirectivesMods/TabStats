package tabstats.playerapi;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import tabstats.util.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorldLoader extends StatWorld {
    Minecraft mc = Minecraft.getMinecraft();

    /* uuid version 4 means its a real player or watchdog bot, version 1 means it's potentially a nicked player */
    public boolean loadOrRender(EntityPlayer player) {
        if (player == null) return false;
        
        UUID uuid = player.getUniqueID();
        
        // Only process version 4 (real players) and version 1 (potentially nicked players)
        // Version 2/3 UUIDs are NPCs - filter them out completely
        return uuid.version() == 4 || uuid.version() == 1;
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
            
            // Set nick status and add to world
            hPlayer.setNicked(isDefinitelyNicked);
            this.addPlayer(uuid, hPlayer);
            this.removeFromStatAssembly(uuid);
        });
    }

    /* populates and checks the stat world player cache every tick */
    @SubscribeEvent
    public void onTick(TickEvent event) {
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
                        if (uuid.version() == 4) {
                            // Version 4 UUIDs (real players) - fetch stats from API
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
    }
}
