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
            try {
                // Experience is optional; used only for fallback level calc
                if (this.playerObject.has("stats") && this.playerObject.getAsJsonObject("stats").has("SkyWars")) {
                    this.experience = new StatInt("Experience", "skywars_experience",
                            this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars"));
                }
            } catch (Exception ignored) { /* silent fail */ }
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

        // STAR (first)
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

    private int resolveSkywarsLevel() {
        try {
            if (this.playerObject.has("stats") && this.playerObject.getAsJsonObject("stats").has("SkyWars")) {
                JsonObject sw = this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars");
                if (sw.has("levelFormatted")) {
                    String formatted = sw.get("levelFormatted").getAsString();
                    int lvl = getSkywarsLevelFromFormatted(formatted);
                    if (lvl > 0) return lvl;
                }
                // If levelFormatted is missing, default to 0
            }
        } catch (Exception ignored) { /* silent-fail */ }
        return 0;
    }

    private String buildStarDisplay() {
        // Prefer in-game formatted string for exact colors and glyphs
        try {
            if (this.playerObject.has("stats") && this.playerObject.getAsJsonObject("stats").has("SkyWars")) {
                JsonObject sw = this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars");
                // Always use Hypixel's formatted string with brackets when available
                try {
                    if (sw.has("levelFormattedWithBrackets")) {
                        String withBrackets = sw.get("levelFormattedWithBrackets").getAsString();
                        String out = withBrackets;
                        // Remove only the literal outer brackets; preserve all § color codes
                        try {
                            int open = out.indexOf('[');
                            int close = out.lastIndexOf(']');
                            if (open >= 0 && close > open) {
                                out = out.substring(0, open) + out.substring(open + 1, close) + out.substring(close + 1);
                            } else {
                                // Fallback: if format is unexpected, remove any brackets but keep colors
                                out = out.replace("[", "").replace("]", "");
                            }
                        } catch (Exception ignored) { /* silent-fail */ }
                        // Trim trailing reset to keep it tidy
                        out = out.replaceAll("§r\\s*$", "");

                        // Detect if there is an inline emblem glyph after the digits
                        String plain = out.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
                        int lastDigit = -1;
                        for (int i = 0; i < plain.length(); i++) {
                            if (Character.isDigit(plain.charAt(i))) lastDigit = i;
                        }
                        String tail = (lastDigit >= 0 && lastDigit + 1 < plain.length()) ? plain.substring(lastDigit + 1).trim() : "";
                        if (tail.isEmpty()) {
                            // No inline emblem present; append mapped active_emblem if available
                            try {
                                if (sw.has("active_emblem")) {
                                    String emblemId = sw.get("active_emblem").getAsString();
                                    String mapped = mapEmblemGlyph(emblemId);
                                    if (mapped != null && !mapped.isEmpty()) {
                                        out = out + mapped;
                                    }
                                }
                            } catch (Exception ignored) { /* silent-fail */ }
                        }
                        return out;
                    }
                } catch (Exception ignored) { /* silent-fail */ }
                if (sw.has("levelFormatted")) {
                    String formatted = sw.get("levelFormatted").getAsString();
                    // Remove square brackets; keep Hypixel's color codes for digits
                    formatted = formatted.replace("[", "").replace("]", "");

                    // No withBrackets available; return the formatted digits as-is
                    return formatted;
                }
            }
        } catch (Exception ignored) { /* silent-fail */ }

        // Absolute fallback: compute level color if nothing else is available
        int lvl = resolveSkywarsLevel();
        return getStarColor(lvl) + Integer.toString(lvl);
    }
}
