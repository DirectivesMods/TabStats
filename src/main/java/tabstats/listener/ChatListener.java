package tabstats.listener;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import tabstats.TabStats;
import tabstats.util.Handler;

public class ChatListener {

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        IChatComponent message = event.message;
        if (message == null) return;
        
        String messageText = message.getUnformattedText();
        if (messageText == null) return;
        
        // Listen for Skywars game start message to refresh tab list (hacky yet consistent implementation to fix players not being tracked after game start)
        if (messageText.contains("Gather resources and equipment on your")) {
            Handler.asExecutor(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                if (Minecraft.getMinecraft().theWorld != null && TabStats.getTabStats() != null) {
                    TabStats.getTabStats().getStatWorld().rerenderTabList();
                }
            });
        }
    }
}