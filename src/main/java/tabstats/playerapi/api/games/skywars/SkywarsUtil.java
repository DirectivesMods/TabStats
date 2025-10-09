package tabstats.playerapi.api.games.skywars;

import tabstats.playerapi.api.games.HGameBase;
import tabstats.playerapi.api.stats.StatInt;
import tabstats.util.ChatColor;

public abstract class SkywarsUtil extends HGameBase {
    public SkywarsUtil(String playerName, String playerUUID) {
        super(playerName, playerUUID);
    }

    public double getKdr(Skywars sw) {
        try {
            if (sw == null || sw.kills == null || sw.deaths == null) return 0D;
            return this.formatDouble(((StatInt) sw.kills).getValue(), ((StatInt) sw.deaths).getValue());
        } catch (Exception ignored) {
            return 0D;
        }
    }

    public double getWlr(Skywars sw) {
        try {
            if (sw == null || sw.wins == null || sw.losses == null) return 0D;
            return this.formatDouble(((StatInt) sw.wins).getValue(), ((StatInt) sw.losses).getValue());
        } catch (Exception ignored) {
            return 0D;
        }
    }

    public ChatColor getKdrColor(double kdr) {
        if (kdr < 1) return ChatColor.GRAY;
        if (kdr < 2) return ChatColor.WHITE;
        if (kdr < 3) return ChatColor.GOLD;
        if (kdr < 5) return ChatColor.DARK_GREEN;
        if (kdr < 10) return ChatColor.RED;
        if (kdr < 20) return ChatColor.DARK_RED;
        if (kdr < 50) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    public ChatColor getWlrColor(double wlr) {
        if (wlr < 1) return ChatColor.GRAY;
        if (wlr < 2) return ChatColor.WHITE;
        if (wlr < 3) return ChatColor.GOLD;
        if (wlr < 5) return ChatColor.DARK_GREEN;
        if (wlr < 10) return ChatColor.RED;
        if (wlr < 15) return ChatColor.DARK_RED;
        if (wlr < 50) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    public ChatColor getKillsColor(int kills) {
        if (kills < 500) return ChatColor.GRAY;
        if (kills < 1000) return ChatColor.WHITE;
        if (kills < 2000) return ChatColor.GOLD;
        if (kills < 5000) return ChatColor.DARK_GREEN;
        if (kills < 10000) return ChatColor.RED;
        if (kills < 20000) return ChatColor.DARK_RED;
        if (kills < 50000) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    public ChatColor getWinsColor(int wins) {
        if (wins < 200) return ChatColor.GRAY;
        if (wins < 500) return ChatColor.WHITE;
        if (wins < 1000) return ChatColor.GOLD;
        if (wins < 2000) return ChatColor.DARK_GREEN;
        if (wins < 5000) return ChatColor.RED;
        if (wins < 7500) return ChatColor.DARK_RED;
        if (wins < 10000) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

}
