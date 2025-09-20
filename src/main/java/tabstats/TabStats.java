package tabstats;

import tabstats.config.ModConfig;
import tabstats.listener.GameOverlayListener;
import tabstats.playerapi.WorldLoader;
import tabstats.util.References;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Arrays;

@Mod(modid = References.MODID, name = References.MODNAME, clientSideOnly = true, version = References.VERSION, acceptedMinecraftVersions = "1.8.9")
public class TabStats {
    private static TabStats tabStats;
    private WorldLoader statWorld;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        tabStats = this;
        ModConfig.getInstance().init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        this.statWorld = new WorldLoader();
        this.registerListeners(statWorld, new GameOverlayListener());
        try {
            net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(tabstats.input.KeyInputHandler.OPEN_GUI);
        } catch (Throwable ignored) {}
    }

    private void registerListeners(Object... listeners) {
        Arrays.stream(listeners).forEachOrdered(MinecraftForge.EVENT_BUS::register);
    }

    public static TabStats getTabStats() { 
        return tabStats; 
    }

    public WorldLoader getStatWorld() { 
        return statWorld; 
    }
}