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
                if (sw.has("skywars_experience")) {
                    int exp = sw.get("skywars_experience").getAsInt();
                    return getSkywarsLevelFromExperience(exp);
                }
            }
        } catch (Exception ignored) { /* silent-fail */ }
        return 0;
    }

    private String buildStarDisplay() {
        // Prefer in-game formatted string for exact colors and glyphs
        try {
            if (this.playerObject.has("stats") && this.playerObject.getAsJsonObject("stats").has("SkyWars")) {
                JsonObject sw = this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars");
                if (sw.has("levelFormatted")) {
                    String formatted = sw.get("levelFormatted").getAsString();
                    // Remove square brackets; keep Hypixel's color codes for digits
                    formatted = formatted.replace("[", "").replace("]", "");

                    // Build a version with any trailing glyph removed: cut at last digit
                    String visible = formatted.replaceAll("§.", "");
                    int lastDigitPos = -1;
                    for (int i = 0; i < visible.length(); i++) {
                        if (Character.isDigit(visible.charAt(i))) lastDigitPos = i;
                    }
                    if (lastDigitPos >= 0) {
                        StringBuilder rebuilt = new StringBuilder();
                        int vIndex = -1;
                        for (int i = 0; i < formatted.length(); i++) {
                            char ch = formatted.charAt(i);
                            if (ch == '§' && i + 1 < formatted.length()) {
                                rebuilt.append(ch).append(formatted.charAt(i + 1));
                                i++; // skip color code payload
                                continue;
                            }
                            vIndex++;
                            rebuilt.append(ch);
                            if (vIndex == lastDigitPos) {
                                break; // stop after last digit; drop any trailing glyphs/spaces
                            }
                        }
                        formatted = rebuilt.toString();
                    }

                    // Decide single glyph to append: prefer active_emblem if present, else default ✯
                    String glyph = "✯";
                    try {
                        if (sw.has("active_emblem")) {
                            String emblemId = sw.get("active_emblem").getAsString();
                            String emblem = mapEmblemGlyph(emblemId);
                            if (emblem != null && !emblem.isEmpty()) {
                                glyph = emblem;
                            }
                        }
                    } catch (Exception ignored) { /* silent-fail */ }

                    return formatted + glyph;
                }
            }
        } catch (Exception ignored) { /* silent-fail */ }

        // Fallback: compute level color, then append a single glyph (active_emblem if present, otherwise ✯)
        int lvl = resolveSkywarsLevel();
        String glyph = "✯";
        try {
            if (this.playerObject.has("stats") && this.playerObject.getAsJsonObject("stats").has("SkyWars")) {
                JsonObject sw = this.playerObject.getAsJsonObject("stats").getAsJsonObject("SkyWars");
                if (sw.has("active_emblem")) {
                    String emblemId = sw.get("active_emblem").getAsString();
                    String emblem = mapEmblemGlyph(emblemId);
                    if (emblem != null && !emblem.isEmpty()) {
                        glyph = emblem;
                    }
                }
            }
        } catch (Exception ignored) { /* silent-fail */ }
        return getStarColor(lvl) + Integer.toString(lvl) + glyph;
    }
}
