package tabstats.playerapi.api.games.duels;

import tabstats.playerapi.api.games.HypixelGames;
import tabstats.playerapi.api.stats.Stat;
import tabstats.playerapi.api.stats.StatInt;
import tabstats.playerapi.api.stats.StatString;
import tabstats.playerapi.exception.GameNullException;
import tabstats.util.ChatColor;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Duels extends DuelsUtil {
    public JsonObject duelJson;
    private final JsonObject wholeObject;
    private List<Stat> statList;
    private final List<Stat> formattedStatList;
    public Stat title, winstreak, bestWinstreak, wins, losses, kills;

    public Duels(String playerName, String playerUUID, JsonObject wholeObject) {
        super(playerName, playerUUID);
        this.wholeObject = wholeObject;
        this.playerObject = wholeObject.get("player").getAsJsonObject();
        this.statList = new ArrayList<>();
        this.formattedStatList = new ArrayList<>();

        if (setData(HypixelGames.DUELS)) {
            this.statList = setStats(
                    this.title = new StatString("Title", "active_cosmetictitle", this.duelJson),
                    this.winstreak = new StatInt("Winstreak", "current_winstreak", this.duelJson),
                    this.bestWinstreak = new StatInt("Best Winstreak", "best_overall_winstreak", this.duelJson),
                    this.wins = new StatInt("Wins", "wins", this.duelJson),
                    this.losses = new StatInt("Losses", "losses", this.duelJson),
                    this.kills = new StatInt("Kills", "kills", this.duelJson));
        } else {
            this.formattedStatList.add(new StatString("Wins", ChatColor.RED + "NICKED"));
        }
    }

    @Override
    public boolean setData(HypixelGames game) {
        this.isNicked = false;
        this.hasPlayed = false;

        try {
            JsonObject obj = getGameData(wholeObject, game);
            if (!this.isNicked) {
                this.hasPlayed = true;
                this.duelJson = obj;
                return true;
            }
            return false;
        } catch (GameNullException ex) {
            return false;
        }
    }

    @Override
    public String getFormattedStats() {
        return String.format("%s%s", getWlrColor(getWlr(this)), getWlr(this));
    }

    @Override
    public HypixelGames getGame() {
        return HypixelGames.DUELS;
    }

    @Override
    public List<Stat> getStatList() {
        return this.statList;
    }


    /* retrieves the formatted stat list */
    @Override
    public List<Stat> getFormattedStatList() {
        List<Stat> statList = new ArrayList<>(this.formattedStatList);

        // If player is nicked or has no data, return minimal safe list
        if (this.isNicked) {
            return statList; // already contains NICKED marker from constructor
        }
        if (!this.hasPlayed || this.duelJson == null) {
            statList.add(new StatString("WLR", ChatColor.GRAY + "0"));
            return statList;
        }

        // Safely build title row
        try {
            StatString title = new StatString("TITLE                      ");
            String tVal = this.getFormattedTitle(this);
            title.setValue(tVal == null ? "N/A" : tVal);
            statList.add(0, title);
        } catch (Exception ignored) { /* silent-fail */ }

        return statList;
    }

    /* sets the formatted stat list when the player is first grabbed */
    /* only set a single time */
    @Override
    public void setFormattedStatList() {
        StatString ws = new StatString("WS");
        ws.setValue(this.getWSColor(((StatInt)this.winstreak).getValue()).toString() + ((StatInt)this.winstreak).getValue());
        this.formattedStatList.add(ws);

        StatString bws = new StatString("BWS");
        bws.setValue(this.getWSColor(((StatInt)this.bestWinstreak).getValue()).toString() + ((StatInt)this.bestWinstreak).getValue());
        this.formattedStatList.add(bws);

        StatString ks = new StatString("KILLS");
        ks.setValue(this.getKillsColor(((StatInt)this.kills).getValue()).toString() + ((StatInt)this.kills).getValue());
        this.formattedStatList.add(ks);

        StatString WLR = new StatString("WLR");
        WLR.setValue(this.getWlrColor(this.getWlr(this)).toString() + this.getWlr(this));
        this.formattedStatList.add(WLR);

        StatString wins = new StatString("WINS");
        wins.setValue(/* this sets the color >>*/ this.getWinsColor(((StatInt)this.wins).getValue()).toString() + /* this is what's actually displayed >>>*/ ((StatInt)this.wins).getValue());
        this.formattedStatList.add(wins);

        StatString losses = new StatString("LOSSES");
        losses.setValue(this.getLossesColor(((StatInt)this.losses).getValue()).toString() + ((StatInt)this.losses).getValue());
        this.formattedStatList.add(losses);
    }
}