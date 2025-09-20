package tabstats.command;

import tabstats.listener.GuiOpenListener;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Arrays;
import java.util.List;

public class TabStatsCommand extends CommandBase {
    
    @Override
    public String getCommandName() {
        return "tabstats";
    }
    
    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("ts");
    }
    
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/tabstats - Open TabStats GUI";
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        GuiOpenListener.requestGuiOpen();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        return null;
    }
}