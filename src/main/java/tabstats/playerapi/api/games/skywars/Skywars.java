package tabstats.playerapi.api.games.skywars;

import tabstats.playerapi.api.games.HypixelGames;
import tabstats.playerapi.api.stats.Stat;
import tabstats.playerapi.api.stats.StatInt;
import tabstats.playerapi.api.stats.StatString;
import tabstats.playerapi.exception.GameNullException;
import tabstats.util.ChatColor;
import com.google.gson.JsonObject;

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
            // Star sources: primary is player.stats.SkyWars.levelFormatted, fallback to player.stats.SkyWars.skywars_experience
            try {
                // Try structured access first; if missing, we handle in formatted getter
                int exp = 0;
                if (this.playerObject.has("stats") && this.playerObject.getAsJsonObject("stats").has("SkyWars")) {
                    JsonObject sw = this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars");
                    if (sw.has("skywars_experience")) {
                        exp = sw.get("skywars_experience").getAsInt();
                    }
                }
                this.experience = new StatInt("Experience", "skywars_experience", this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars"));
            } catch (Exception ignored) { /* silent fail */ }
        } else {
            this.formattedStatList.add(new StatString("KDR", ChatColor.RED + "NICKED"));
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
    // STAR (first)
    StatString star = new StatString("STAR");
    star.setValue(buildStarDisplay());
    list.add(0, star);

        // KDR
        StatString kdr = new StatString("KDR");
        kdr.setValue(this.getKdrColor(this.getKdr(this)).toString() + this.getKdr(this));
        list.add(kdr);

        // KILLS
        StatString kills = new StatString("KILLS");
        kills.setValue(this.getKillsColor(((StatInt) this.kills).getValue()).toString() + ((StatInt) this.kills).getValue());
        list.add(kills);

        // WLR
        StatString wlr = new StatString("WLR");
        wlr.setValue(this.getWlrColor(this.getWlr(this)).toString() + this.getWlr(this));
        list.add(wlr);

        // WINS
        StatString wins = new StatString("WINS");
        wins.setValue(this.getWinsColor(((StatInt) this.wins).getValue()).toString() + ((StatInt) this.wins).getValue());
        list.add(wins);

        return list;
    }

    @Override
    public void setFormattedStatList() {
        // no-op; we fill formattedStatList from getters to keep consistent with other games
    }

    private int resolveSkywarsLevel() {
        // Try levelFormatted first
        try {
            if (this.playerObject.has("stats") && this.playerObject.getAsJsonObject("stats").has("SkyWars")) {
                JsonObject sw = this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars");
                if (sw.has("levelFormatted")) {
                    String formatted = sw.get("levelFormatted").getAsString();
                    int lvl = getSkywarsLevelFromFormatted(formatted);
                    if (lvl > 0) return lvl;
                }
                if (sw.has("skywars_experience")) {
                    int exp = sw.get("skywars_experience").getAsInt();
                    return getSkywarsLevelFromExperience(exp);
                }
            }
        } catch (Exception ignored) { /* silent-fail */ }
        return 0;
    }

    private String buildStarDisplay() {
        // Prefer in-game formatted string for exact colors and unicode
        try {
            if (this.playerObject.has("stats") && this.playerObject.getAsJsonObject("stats").has("SkyWars")) {
                JsonObject sw = this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars");
                if (sw.has("levelFormatted")) {
                    String formatted = sw.get("levelFormatted").getAsString();
                    // Remove square brackets; keep color codes
                    formatted = formatted.replace("[", "").replace("]", "");
                    // Normalize various star glyphs to a glyph known to render in 1.8.9 (✫, \u272B)
                    formatted = formatted
                            .replace("\u2730", "\u272B") // ✰ -> ✫
                            .replace("\u272A", "\u272B") // ✪ -> ✫
                            .replace("\u272F", "\u272B") // ✯ -> ✫
                            .replace("\u272E", "\u272B") // ✮ -> ✫
                            .replace("\u2605", "\u272B") // ★ -> ✫
                            .replace("\u2606", "\u272B"); // ☆ -> ✫
                    // If no star survived (some API variants), append one
                    if (!formatted.contains("\u272B")) {
                        formatted = formatted + "\u272B";
                    }
                    return formatted;
                }
            }
        } catch (Exception ignored) { /* silent-fail */ }

        // Fallback to our colored star composition
        return this.getStarWithColor(resolveSkywarsLevel());
    }
}
