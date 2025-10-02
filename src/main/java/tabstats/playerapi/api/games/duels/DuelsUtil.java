package tabstats.playerapi.api.games.duels;

import tabstats.playerapi.api.games.HGameBase;
import tabstats.playerapi.api.stats.StatInt;
import tabstats.playerapi.api.stats.StatString;
import tabstats.util.ChatColor;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Locale;

public abstract class DuelsUtil extends HGameBase {
    public DuelsUtil(String playerName, String playerUUID) {
        super(playerName, playerUUID);
    }

    public double getWlr(Duels duels) {
        try {
            if (duels == null || duels.wins == null || duels.losses == null) return 0D;
            return this.formatDouble(((StatInt)duels.wins).getValue(), ((StatInt)duels.losses).getValue());
        } catch (Exception ignored) {
            return 0D;
        }
    }

    public ChatColor getWlrColor(double wlr) {
        if (wlr < 2) return ChatColor.GRAY;
        if (wlr < 4) return ChatColor.WHITE;
        if (wlr < 6) return ChatColor.GOLD;
        if (wlr < 7) return ChatColor.DARK_GREEN;
        if (wlr < 10) return ChatColor.RED;
        if (wlr < 15) return ChatColor.DARK_RED;
        if (wlr < 50) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    public ChatColor getWSColor(int ws) {
        if (ws < 50) return ChatColor.GRAY;
        if (ws < 200) return ChatColor.WHITE;
        if (ws < 350) return ChatColor.GOLD;
        if (ws < 500) return ChatColor.DARK_GREEN;
        if (ws < 650) return ChatColor.RED;
        if (ws < 800) return ChatColor.DARK_RED;
        if (ws < 1000) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    public ChatColor getKillsColor(int kills) {
        if (kills >= 200000) return ChatColor.GOLD;
        if (kills >= 100000) return ChatColor.LIGHT_PURPLE;
        if (kills >= 50000) return ChatColor.AQUA;
        if (kills >= 20000) return ChatColor.DARK_PURPLE;
        if (kills >= 10000) return ChatColor.YELLOW;
        if (kills >= 4000) return ChatColor.DARK_RED;
        if (kills >= 2000) return ChatColor.DARK_GREEN;
        if (kills >= 1000) return ChatColor.DARK_AQUA;
        if (kills >= 500) return ChatColor.GOLD;
        if (kills >= 200) return ChatColor.WHITE;
        return ChatColor.GRAY;
    }

    public ChatColor getWinsColor(int wins) {
        if (wins >= 200000) return ChatColor.GOLD;
        if (wins >= 100000) return ChatColor.LIGHT_PURPLE;
        if (wins >= 50000) return ChatColor.AQUA;
        if (wins >= 20000) return ChatColor.DARK_PURPLE;
        if (wins >= 10000) return ChatColor.YELLOW;
        if (wins >= 4000) return ChatColor.DARK_RED;
        if (wins >= 2000) return ChatColor.DARK_GREEN;
        if (wins >= 1000) return ChatColor.DARK_AQUA;
        if (wins >= 500) return ChatColor.GOLD;
        if (wins >= 200) return ChatColor.WHITE;
        return ChatColor.GRAY;
    }

    public ChatColor getLossesColor(int losses) {
        if (losses < 50) return ChatColor.DARK_PURPLE;
        if (losses < 75) return ChatColor.LIGHT_PURPLE;
        if (losses < 100) return ChatColor.DARK_RED;
        if (losses < 150) return ChatColor.RED;
        if (losses < 200) return ChatColor.DARK_GREEN;
        if (losses < 250) return ChatColor.GOLD;
        if (losses < 300) return ChatColor.WHITE;
        return ChatColor.GRAY;
    }

    public String getFormattedTitle(Duels duels) {
        String title = ((StatString)duels.title).getValue();
        String formattedTitle = title.replace("_", " ").replace("cosmetictitle", "");

        if (this.isPrestigeTitle(title)) {
            String modeName = title.substring(title.lastIndexOf("_") + 1);;

            /* Hypixel being extra difficult and changing the names of their gamemodes only for titles */
            if (title.contains("no_debuff") || title.contains("mega_walls") || title.contains("tnt_games") || title.contains("all_modes")) {
                /* probably an easier way of doing this, although I'm lazy ~Max */
                if (title.contains("no_debuff")) {
                    modeName = "no_debuff";
                } else if (title.contains("mega_walls")) {
                    modeName = "mega_walls";
                } else if (title.contains("tnt_games")) {
                    modeName = "tnt_games";
                } else if (title.contains("all_modes")) {
                    modeName = "all_modes";
                }
            }

            try {
                DuelsModes duelMode = DuelsModes.valueOf(modeName.toUpperCase(Locale.ROOT));
                String gamemodeName = duelMode.getName();

                // Safely pull wins json; if absent, fall back to formatted title
                int gamemodeWins = 0;
                try {
                    if (duels.duelJson != null && duels.duelJson.has(duelMode.getWinsJson()) && !duels.duelJson.get(duelMode.getWinsJson()).isJsonNull()) {
                        gamemodeWins = duels.duelJson.get(duelMode.getWinsJson()).getAsInt();
                    }
                } catch (Exception ignored) { gamemodeWins = 0; }

                int multiplier = title.toLowerCase(Locale.ROOT).contains("all modes") ? 2 : 1;

                if (gamemodeWins >= 100000 * multiplier) {
                    return ChatColor.GOLD + gamemodeName + " World's Best";
                } else if (gamemodeWins >= 50000 * multiplier) {
                    return ChatColor.LIGHT_PURPLE + gamemodeName + " World Master";
                } else if (gamemodeWins >= 25000 * multiplier) {
                    return ChatColor.AQUA + gamemodeName + " World Elite";
                } else if (gamemodeWins >= 10000 * multiplier) {
                    return ChatColor.DARK_PURPLE + ChatColor.BOLD.toString() + gamemodeName + " Godlike";
                } else if (gamemodeWins >= 5000 * multiplier) {
                    return ChatColor.YELLOW + ChatColor.BOLD.toString() + gamemodeName + " Grandmaster";
                } else if (gamemodeWins >= 2000 * multiplier) {
                    return ChatColor.DARK_RED + ChatColor.BOLD.toString() + gamemodeName + " Legend";
                } else if (gamemodeWins >= 1000 * multiplier) {
                    return ChatColor.DARK_GREEN + gamemodeName + " Master";
                } else if (gamemodeWins >= 500 * multiplier) {
                    return ChatColor.DARK_AQUA + gamemodeName + " Diamond";
                } else if (gamemodeWins >= 250 * multiplier) {
                    return ChatColor.GOLD + gamemodeName + " Gold";
                } else if (gamemodeWins >= 100 * multiplier) {
                    return ChatColor.WHITE + gamemodeName + " Iron";
                } else if (gamemodeWins >= 50 * multiplier) {
                    return ChatColor.GRAY + gamemodeName + " Rookie";
                }
            } catch (Throwable ignored) {
                // Any failure (missing enum constant due to new Hypixel mode, classload issue, bad json) falls back to raw formatted title.
            }
        }

        return WordUtils.capitalize(formattedTitle.trim());
    }

    private boolean isPrestigeTitle(String title) {
        title = title.toUpperCase();
        return title.contains("ROOKIE") || title.contains("IRON") || title.contains("GOLD") || title.contains("DIAMOND") || title.contains("MASTER") || title.contains("LEGEND") || title.contains("GRANDMASTER") || title.contains("GODLIKE") || title.contains("WORLD_ELITE") || title.contains("WORLD_MASTER") || title.contains("WORLDS_BEST");
    }
}