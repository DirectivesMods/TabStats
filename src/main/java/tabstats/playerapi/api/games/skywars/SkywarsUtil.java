package tabstats.playerapi.api.games.skywars;

import tabstats.playerapi.api.games.HGameBase;
import tabstats.playerapi.api.stats.StatInt;
import tabstats.util.ChatColor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class SkywarsUtil extends HGameBase {
    public SkywarsUtil(String playerName, String playerUUID) {
        super(playerName, playerUUID);
    }

    public double getKdr(Skywars sw) {
        return this.formatDouble(((StatInt) sw.kills).getValue(), ((StatInt) sw.deaths).getValue());
    }

    public double getWlr(Skywars sw) {
        return this.formatDouble(((StatInt) sw.wins).getValue(), ((StatInt) sw.losses).getValue());
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

    public ChatColor getWSColor(int ws) {
        if (ws < 5) return ChatColor.GRAY;
        if (ws < 10) return ChatColor.WHITE;
        if (ws < 20) return ChatColor.GOLD;
        if (ws < 35) return ChatColor.DARK_GREEN;
        if (ws < 50) return ChatColor.RED;
        if (ws < 75) return ChatColor.DARK_RED;
        if (ws < 100) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    /* SkyWars star helpers */
    public int getSkywarsLevelFromFormatted(String levelFormatted) {
        try {
            if (levelFormatted == null) return 0;
            // Strip color codes and brackets, extract digits
            String digits = levelFormatted.replaceAll("§[0-9A-FK-ORa-fk-or]", "").replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            return Integer.parseInt(digits);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public int getSkywarsLevelFromExperience(int exp) {
        // Fallback approximation: community standard is 10,000 XP per level after early levels.
        // We'll start simple and refine later.
        if (exp <= 0) return 0;
        return Math.max(1, exp / 10000);
    }

    public ChatColor getStarColor(int level) {
        if (level < 5) return ChatColor.GRAY;
        if (level < 10) return ChatColor.WHITE;
        if (level < 15) return ChatColor.GOLD;
        if (level < 20) return ChatColor.AQUA;
        if (level < 25) return ChatColor.DARK_GREEN;
        if (level < 30) return ChatColor.DARK_AQUA;
        if (level < 40) return ChatColor.DARK_RED;
        if (level < 50) return ChatColor.LIGHT_PURPLE;
        if (level < 75) return ChatColor.BLUE;
        return ChatColor.DARK_PURPLE;
    }

    public String getStarWithColor(int level) {
        // SkyWars default glyph per user preference
        return getStarColor(level) + Integer.toString(level) + "\u272F"; // ✯
    }

    /* Emblem mapping */
    private static final Map<String, String> EMBLEM_MAP;
    static {
        Map<String, String> m = new HashMap<String, String>();
        // Exact IDs → glyphs (from user-provided list)
        m.put("emblem_default", "✯");
        m.put("emblem_alpha", "α");
        m.put("emblem_omega", "Ω");
        m.put("emblem_rich", "$");
        m.put("emblem_podium", "π");
        m.put("emblem_crossed_swords", "⚔");
        m.put("emblem_null_icon", "∅");
        m.put("emblem_delta_icon", "δ");
        m.put("emblem_sigma_icon", "Σ");
        m.put("emblem_florin", "ƒ");
        m.put("emblem_fallen_crest", "☬");
        m.put("emblem_angel_1", "★");
        m.put("emblem_angel_2", "☆");
        m.put("emblem_angel_3", "☼");
        m.put("emblem_angel_4", "✶");
        m.put("emblem_angel_5", "✳");
        m.put("emblem_angel_6", "✴");
        m.put("emblem_angel_7", "✷");
        m.put("emblem_angel_8", "❋");
        m.put("emblem_angel_9", "✼");
        m.put("emblem_angel_10", "❂");
        m.put("emblem_angel_11", "❁");
        m.put("emblem_small_star", "⋆");
        m.put("emblem_star", "✯");
        m.put("emblem_greek_cross", "✙");
        m.put("emblem_heart", "❤");
        m.put("emblem_skull", "☠");
        m.put("emblem_four_points", "✦");
        m.put("emblem_peace_out", "✌");
        m.put("emblem_floral_heart", "❦");
        m.put("emblem_eight_pointed_pinwheel", "✵");
        m.put("emblem_heart_exclamation", "❣");
        m.put("emblem_yin_yang", "☯");
        m.put("emblem_yin_and_yang", "☯");
        m.put("emblem_formerly_known", "@_@");
        m.put("emblem_sixteen_pointed_asterisk", "✺");
        m.put("emblem_airplane", "✈");
        m.put("emblem_coffin", "⚰");
        m.put("emblem_maltese_cross", "✠");
        m.put("emblem_queen", "♕");
        m.put("emblem_bolt", "⚡");
        m.put("emblem_reflex_angle_eyebrows", "δvδ");
        m.put("emblem_asterism", "⁂");
        m.put("emblem_shadowed_star", "✰");
        m.put("emblem_stacked_asterisks", "⁑");
        m.put("emblem_radioactive", "☢");
        m.put("emblem_four_clubs", "✥");
        m.put("emblem_rocket", "♝");
        m.put("emblem_neptune", "♆");
        m.put("emblem_circled_star", "⍟");
        m.put("emblem_bishop", "♗");
        m.put("emblem_king", "♔");
        m.put("emblem_knight", "♞");
        m.put("emblem_pencil", "✏");
        m.put("emblem_heavy_sparkle", "❈");
        m.put("emblem_carrrots_for_eyes", "^_^");
        m.put("emblem_same_great_taste", "ಠ_ಠ");
        m.put("emblem_misaligned", "o...0");
        m.put("emblem_converge_on_tongue", ">u<");
        m.put("emblem_no_evil", "v-v");
        m.put("emblem_three_fourths_jam", "༼つ◕_◕༽つ");
        m.put("emblem_slime", "■·■");
        m.put("emblem_cloud", "☁");
        EMBLEM_MAP = Collections.unmodifiableMap(m);
    }

    public String mapEmblemGlyph(String emblemId) {
        try {
            if (emblemId == null || emblemId.isEmpty()) return null;
            String key = emblemId.trim();
            String exact = EMBLEM_MAP.get(key);
            if (exact != null) return exact;
            // Some APIs might return uppercase; try lowercase pass
            exact = EMBLEM_MAP.get(key.toLowerCase());
            if (exact != null) return exact;
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
