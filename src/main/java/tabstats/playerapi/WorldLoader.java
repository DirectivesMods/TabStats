package tabstats.playerapi;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import tabstats.TabStats;
import tabstats.config.ModConfig;
import tabstats.listener.GameOverlayListener;
import tabstats.util.ChatColor;
import tabstats.util.Handler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class WorldLoader extends StatWorld {
    private final Minecraft mc = Minecraft.getMinecraft();
    private World lastObservedWorld;
    private static final Pattern VALID_USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private boolean lastModEnabled = ModConfig.getInstance().isModEnabled();

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

    // Allow only UUID versions we care about: 4 (real), 2 (lobby/replay), 1 (nicked). Version 3 = holograms/NPCs.
        int version = uuid.version();
        return version == 4 || version == 2 || version == 1;
    }

    /** Handle version 1 UUIDs: always nicked. */
    private void checkNickStatus(EntityPlayer entityPlayer) {
        Handler.asExecutor(() -> {
            if (!ModConfig.getInstance().isModEnabled()) {
                return;
            }
            UUID uuid = entityPlayer.getUniqueID();
            HPlayer hPlayer = new HPlayer(uuid.toString(), entityPlayer.getName());
            hPlayer.setNicked(true);
            this.addPlayer(uuid, hPlayer);
            this.removeFromStatAssembly(uuid);
        });
    }

    /* populates and checks the stat world player cache every client tick */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!ModConfig.getInstance().isModEnabled()) {
            // Just reset scroll when disabling, preserve cache
            if (lastModEnabled) {
                resetTabScroll();
            }
            lastModEnabled = false;
            return;
        }

        lastModEnabled = true;
        World currentWorld = mc.theWorld;

        if (currentWorld != lastObservedWorld) {
            lastObservedWorld = currentWorld;
            // Only reset scroll position on world change, preserve cache
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

            if (this.getWorldPlayers().containsKey(uuid)) {
                continue;
            }

            if (!this.statAssembly.add(uuid)) {
                continue;
            }

            if (uuid.version() == 4 || uuid.version() == 2) {
                this.fetchStats(entityPlayer);
            } else if (uuid.version() == 1) {
                this.checkNickStatus(entityPlayer);
            }
            this.checkCacheSize();
        }
    }

    public void checkCacheSize() {
        if (getWorldPlayers().size() > 500) {
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
