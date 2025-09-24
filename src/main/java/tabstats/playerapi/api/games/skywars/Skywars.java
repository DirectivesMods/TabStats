package tabstats.playerapi.api.games.skywars;

import com.google.gson.JsonObject;
import tabstats.playerapi.api.games.HypixelGames;
import tabstats.playerapi.api.stats.Stat;
import tabstats.playerapi.api.stats.StatInt;
import tabstats.playerapi.api.stats.StatString;
import tabstats.playerapi.exception.GameNullException;
import tabstats.util.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class Skywars extends SkywarsUtil {
    public JsonObject skywarsJson;
    private final JsonObject wholeObject;
    private List<Stat> statList;
    private final List<Stat> formattedStatList;
    public Stat wins, losses, kills, deaths, star, experience, levelFormatted;

    public Skywars(String playerName, String playerUUID, JsonObject wholeObject) {
        super(playerName, playerUUID);
        this.wholeObject = wholeObject;
        this.playerObject = wholeObject.get("player").getAsJsonObject();
        this.statList = new ArrayList<>();
        this.formattedStatList = new ArrayList<>();

        if (setData(HypixelGames.SKYWARS)) {
            this.statList = setStats(
                    this.wins = new StatInt("Wins", "wins", this.skywarsJson),
                    this.losses = new StatInt("Losses", "losses", this.skywarsJson),
                    this.kills = new StatInt("Kills", "kills", this.skywarsJson),
                    this.deaths = new StatInt("Deaths", "deaths", this.skywarsJson)
            );
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
                this.skywarsJson = obj;
                return true;
            }
            return false;
        } catch (GameNullException ex) {
            return false;
        }
    }

    @Override
    public String getFormattedStats() {
        return String.format("%s%s", getKdrColor(getKdr(this)), getKdr(this));
    }

    @Override
    public HypixelGames getGame() {
        return HypixelGames.SKYWARS;
    }

    @Override
    public List<Stat> getStatList() {
        return this.statList;
    }

    @Override
    public List<Stat> getFormattedStatList() {
        List<Stat> list = new ArrayList<>(this.formattedStatList);

        // If player has no stats, return empty list so they show only their name
        if (!this.hasPlayed || this.skywarsJson == null) {
            return new ArrayList<>(); // Empty list = no stats displayed
        }

        // STAR
        StatString star = new StatString("STAR");
        star.setValue(buildStarDisplay());
        list.add(0, star);

        // KDR
        StatString kdr = new StatString("KDR");
        double kdrVal = this.getKdr(this);
        kdr.setValue(this.getKdrColor(kdrVal).toString() + kdrVal);
        list.add(kdr);

        // KILLS
        StatString kills = new StatString("KILLS");
        int killsVal = 0;
        try { if (this.kills != null) killsVal = ((StatInt) this.kills).getValue(); } catch (Exception ignored) {}
        kills.setValue(this.getKillsColor(killsVal).toString() + killsVal);
        list.add(kills);

        // WLR
        StatString wlr = new StatString("WLR");
        double wlrVal = this.getWlr(this);
        wlr.setValue(this.getWlrColor(wlrVal).toString() + wlrVal);
        list.add(wlr);

        // WINS
        StatString wins = new StatString("WINS");
        int winsVal = 0;
        try { if (this.wins != null) winsVal = ((StatInt) this.wins).getValue(); } catch (Exception ignored) {}
        wins.setValue(this.getWinsColor(winsVal).toString() + winsVal);
        list.add(wins);

        return list;
    }    @Override
    public void setFormattedStatList() {
        // no-op; we assemble in getFormattedStatList for consistency
    }

    private String buildStarDisplay() {
        // Only use Hypixel's preformatted string; strip brackets so only number + glyph remain
        try {
            if (this.playerObject.has("stats") && this.playerObject.getAsJsonObject("stats").has("SkyWars")) {
                JsonObject sw = this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars");
                if (sw.has("levelFormattedWithBrackets")) {
                    String s = sw.get("levelFormattedWithBrackets").getAsString();
                    // Remove literal square brackets, keep colors and any glyphs
                    return s.replace("[", "").replace("]", "");
                }
            }
        } catch (Exception ignored) { /* silent-fail */ }
        // If the API doesn't provide it, show a simple placeholder
        return ChatColor.GRAY + "-";
    }
}
