package tabstats.playerapi.api.games.bedwars;

import tabstats.playerapi.api.games.HGameBase;
import tabstats.playerapi.api.stats.StatInt;
import tabstats.util.ChatColor;

import java.util.HashMap;
import java.util.Map;

public abstract class BedwarsUtil extends HGameBase {
    public BedwarsUtil(String playerName, String playerUUID) {
        super(playerName, playerUUID);
    }

    public double getFkdr(Bedwars bw) {
        try {
            if (bw == null || bw.finalKills == null || bw.finalDeaths == null) return 0D;
            return this.formatDouble(((StatInt)bw.finalKills).getValue(), ((StatInt)bw.finalDeaths).getValue());
        } catch (Exception ignored) {
            return 0D;
        }
    }

    public ChatColor getFkdrColor(double fkdr) {
        if (fkdr < 1.5) return ChatColor.GRAY;
        if (fkdr < 3.5) return ChatColor.WHITE;
        if (fkdr < 5) return ChatColor.GOLD;
        if (fkdr < 10) return ChatColor.DARK_GREEN;
        if (fkdr < 20) return ChatColor.RED;
        if (fkdr < 50) return ChatColor.DARK_RED;
        if (fkdr < 100) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    public double getWlr(Bedwars bw) {
        try {
            if (bw == null || bw.wins == null || bw.losses == null) return 0D;
            return this.formatDouble(((StatInt)bw.wins).getValue(), ((StatInt)bw.losses).getValue());
        } catch (Exception ignored) {
            return 0D;
        }
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

    public double getBblr(Bedwars bw) {
        try {
            if (bw == null || bw.bedsBroken == null || bw.bedsLost == null) return 0D;
            return this.formatDouble(((StatInt)bw.bedsBroken).getValue(), ((StatInt)bw.bedsLost).getValue());
        } catch (Exception ignored) {
            return 0D;
        }
    }

    public ChatColor getBblrColor(double bblr) {
        if (bblr < 1.5) return ChatColor.GRAY;
        if (bblr < 2.5) return ChatColor.WHITE;
        if (bblr < 3.5) return ChatColor.GOLD;
        if (bblr < 5) return ChatColor.DARK_GREEN;
        if (bblr < 7.5) return ChatColor.RED;
        if (bblr < 10) return ChatColor.DARK_RED;
        if (bblr < 15) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    public ChatColor getWSColor(double ws) {
        if (ws < 5) return ChatColor.GRAY;
        if (ws < 10) return ChatColor.WHITE;
        if (ws < 20) return ChatColor.GOLD;
        if (ws < 35) return ChatColor.DARK_GREEN;
        if (ws < 50) return ChatColor.RED;
        if (ws < 75) return ChatColor.DARK_RED;
        if (ws < 100) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    public String getStarWithColor(int star) {
        PrestigeStyle style = getPrestigeStyle(star);
        if (style != null) {
            return formatWithStyle(Integer.toString(star), style) + style.glyphColor + getBedwarsGlyph(star);
        }

        return ChatColor.GRAY + "-";
    }

    private String getBedwarsGlyph(int star) {
        if (star < 1000) return "\u272B"; // ✫
        if (star < 2000) return "\u272A"; // ✪
        if (star < 3000) return "\u269D"; // ⚝
        if (star < 4000) return "\u2725"; // ✥
        if (star < 5000) return "\u2725"; // ✥
        return "\u2725"; // ✥
    }

    public ChatColor getFinalsColor(int finals) {
        if (finals < 300) return ChatColor.GRAY;
        if (finals < 500) return ChatColor.WHITE;
        if (finals < 1000) return ChatColor.GOLD;
        if (finals < 5000) return ChatColor.DARK_GREEN;
        if (finals < 1000) return ChatColor.RED;
        if (finals < 15000) return ChatColor.DARK_RED;
        if (finals < 20000) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    public ChatColor getWinsColor(int wins) {
        if (wins < 100) return ChatColor.GRAY;
        if (wins < 300) return ChatColor.WHITE;
        if (wins < 500) return ChatColor.GOLD;
        if (wins < 1000) return ChatColor.DARK_GREEN;
        if (wins < 3000) return ChatColor.RED;
        if (wins < 5000) return ChatColor.DARK_RED;
        if (wins < 10000) return ChatColor.LIGHT_PURPLE;
        return ChatColor.DARK_PURPLE;
    }

    
    private static class PrestigeStyle {
        final ChatColor[] digitColors;
        final ChatColor glyphColor;

        private PrestigeStyle(ChatColor[] digitColors, ChatColor glyphColor) {
            this.digitColors = digitColors;
            this.glyphColor = glyphColor;
        }
    }

    private static final Map<Integer, PrestigeStyle> PRESTIGE_STYLES = new HashMap<>();

    private String formatWithStyle(String number, PrestigeStyle style) {
        StringBuilder sb = new StringBuilder();
        char[] chars = number.toCharArray();
        int n = chars.length;
        int styleLen = style.digitColors.length;
        for (int i = 0; i < n; i++) {
            int fromRight = n - 1 - i;
            ChatColor color;
            if (fromRight < styleLen) {
                color = style.digitColors[styleLen - 1 - fromRight];
            } else {
                color = style.digitColors[0];
            }
            sb.append(color.toString()).append(chars[i]);
        }
        return sb.toString();
    }

    private PrestigeStyle getPrestigeStyle(int star) {
        if (star <= 0) return null;
        
        // For 5000+ stars, always use the 5000 star style
        if (star >= 5000) {
            return PRESTIGE_STYLES.get(5000);
        }
        
        int base = (star / 100) * 100; // floor using integer math
        return PRESTIGE_STYLES.get(base);
    }

    private static PrestigeStyle mono(ChatColor digits, ChatColor glyph) {
        return new PrestigeStyle(new ChatColor[]{digits, digits, digits, digits}, glyph);
    }

    private static void putMono(int base, ChatColor c) {
        PRESTIGE_STYLES.put(base, mono(c, c));
    }

    // refers to https://hypixel.fandom.com/wiki/Bed_Wars#Prestiges
    static {
        putMono(0, ChatColor.GRAY);
        putMono(100, ChatColor.WHITE);
        putMono(200, ChatColor.GOLD);
        putMono(300, ChatColor.AQUA);
        putMono(400, ChatColor.DARK_GREEN);
        putMono(500, ChatColor.DARK_AQUA);
        putMono(600, ChatColor.DARK_RED);
        putMono(700, ChatColor.LIGHT_PURPLE);
        putMono(800, ChatColor.BLUE);
        putMono(900, ChatColor.DARK_PURPLE);

        PRESTIGE_STYLES.put(1000, new PrestigeStyle(new ChatColor[]{ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.AQUA}, ChatColor.LIGHT_PURPLE));
        PRESTIGE_STYLES.put(1100, mono(ChatColor.WHITE, ChatColor.GRAY));
        PRESTIGE_STYLES.put(1200, mono(ChatColor.YELLOW, ChatColor.GOLD));
        PRESTIGE_STYLES.put(1300, mono(ChatColor.AQUA, ChatColor.DARK_AQUA));
        PRESTIGE_STYLES.put(1400, mono(ChatColor.GREEN, ChatColor.DARK_GREEN));
        PRESTIGE_STYLES.put(1500, mono(ChatColor.DARK_AQUA, ChatColor.BLUE));
        PRESTIGE_STYLES.put(1600, mono(ChatColor.RED, ChatColor.DARK_RED));
        PRESTIGE_STYLES.put(1700, mono(ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
        PRESTIGE_STYLES.put(1800, mono(ChatColor.BLUE, ChatColor.DARK_BLUE));
        PRESTIGE_STYLES.put(1900, mono(ChatColor.DARK_PURPLE, ChatColor.DARK_GRAY));
        PRESTIGE_STYLES.put(2000, new PrestigeStyle(new ChatColor[]{ChatColor.GRAY, ChatColor.WHITE, ChatColor.WHITE, ChatColor.GRAY}, ChatColor.GRAY));
        PRESTIGE_STYLES.put(2100, new PrestigeStyle(new ChatColor[]{ChatColor.WHITE, ChatColor.YELLOW, ChatColor.YELLOW, ChatColor.GOLD}, ChatColor.GOLD));
        PRESTIGE_STYLES.put(2200, new PrestigeStyle(new ChatColor[]{ChatColor.GOLD, ChatColor.WHITE, ChatColor.WHITE, ChatColor.AQUA}, ChatColor.DARK_AQUA));
        PRESTIGE_STYLES.put(2300, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE, ChatColor.LIGHT_PURPLE, ChatColor.GOLD}, ChatColor.YELLOW));
        PRESTIGE_STYLES.put(2400, new PrestigeStyle(new ChatColor[]{ChatColor.AQUA, ChatColor.WHITE, ChatColor.WHITE, ChatColor.GRAY}, ChatColor.GRAY));
        PRESTIGE_STYLES.put(2500, new PrestigeStyle(new ChatColor[]{ChatColor.WHITE, ChatColor.GREEN, ChatColor.GREEN, ChatColor.DARK_GREEN}, ChatColor.DARK_GREEN));
        PRESTIGE_STYLES.put(2600, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_RED, ChatColor.RED, ChatColor.RED, ChatColor.LIGHT_PURPLE}, ChatColor.LIGHT_PURPLE));
        PRESTIGE_STYLES.put(2700, new PrestigeStyle(new ChatColor[]{ChatColor.YELLOW, ChatColor.WHITE, ChatColor.WHITE, ChatColor.DARK_GRAY}, ChatColor.DARK_GRAY));
        PRESTIGE_STYLES.put(2800, new PrestigeStyle(new ChatColor[]{ChatColor.GREEN, ChatColor.GREEN, ChatColor.GREEN, ChatColor.GOLD}, ChatColor.GOLD));
        PRESTIGE_STYLES.put(2900, new PrestigeStyle(new ChatColor[]{ChatColor.AQUA, ChatColor.DARK_AQUA, ChatColor.DARK_AQUA, ChatColor.BLUE}, ChatColor.BLUE));
        PRESTIGE_STYLES.put(3000, new PrestigeStyle(new ChatColor[]{ChatColor.YELLOW, ChatColor.GOLD, ChatColor.GOLD, ChatColor.RED}, ChatColor.RED));
        PRESTIGE_STYLES.put(3100, new PrestigeStyle(new ChatColor[]{ChatColor.BLUE, ChatColor.DARK_AQUA, ChatColor.DARK_AQUA, ChatColor.GOLD}, ChatColor.GOLD));
        PRESTIGE_STYLES.put(3200, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_RED, ChatColor.GRAY, ChatColor.GRAY, ChatColor.DARK_RED}, ChatColor.RED));
        PRESTIGE_STYLES.put(3300, new PrestigeStyle(new ChatColor[]{ChatColor.BLUE, ChatColor.BLUE, ChatColor.LIGHT_PURPLE, ChatColor.RED}, ChatColor.RED));
        PRESTIGE_STYLES.put(3400, new PrestigeStyle(new ChatColor[]{ChatColor.GREEN, ChatColor.LIGHT_PURPLE, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE}, ChatColor.DARK_PURPLE));
        PRESTIGE_STYLES.put(3500, new PrestigeStyle(new ChatColor[]{ChatColor.RED, ChatColor.DARK_RED, ChatColor.DARK_RED, ChatColor.DARK_GREEN}, ChatColor.GREEN));
        PRESTIGE_STYLES.put(3600, new PrestigeStyle(new ChatColor[]{ChatColor.GREEN, ChatColor.GREEN, ChatColor.AQUA, ChatColor.BLUE}, ChatColor.BLUE));
        PRESTIGE_STYLES.put(3700, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_RED, ChatColor.RED, ChatColor.RED, ChatColor.AQUA}, ChatColor.DARK_AQUA));
        PRESTIGE_STYLES.put(3800, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_BLUE, ChatColor.BLUE, ChatColor.DARK_PURPLE, ChatColor.DARK_PURPLE}, ChatColor.LIGHT_PURPLE));
        PRESTIGE_STYLES.put(3900, new PrestigeStyle(new ChatColor[]{ChatColor.RED, ChatColor.GREEN, ChatColor.GREEN, ChatColor.DARK_AQUA}, ChatColor.BLUE));
        PRESTIGE_STYLES.put(4000, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_PURPLE, ChatColor.RED, ChatColor.RED, ChatColor.GOLD}, ChatColor.GOLD));
        PRESTIGE_STYLES.put(4100, new PrestigeStyle(new ChatColor[]{ChatColor.YELLOW, ChatColor.GOLD, ChatColor.RED, ChatColor.LIGHT_PURPLE}, ChatColor.LIGHT_PURPLE));
        PRESTIGE_STYLES.put(4200, new PrestigeStyle(new ChatColor[]{ChatColor.BLUE, ChatColor.DARK_AQUA, ChatColor.AQUA, ChatColor.WHITE}, ChatColor.GRAY));
        PRESTIGE_STYLES.put(4300, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_PURPLE, ChatColor.DARK_GRAY, ChatColor.DARK_GRAY, ChatColor.DARK_PURPLE}, ChatColor.DARK_PURPLE));
        PRESTIGE_STYLES.put(4400, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_GREEN, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.GOLD}, ChatColor.DARK_PURPLE));
        PRESTIGE_STYLES.put(4500, new PrestigeStyle(new ChatColor[]{ChatColor.WHITE, ChatColor.AQUA, ChatColor.AQUA, ChatColor.DARK_AQUA}, ChatColor.DARK_AQUA));
        PRESTIGE_STYLES.put(4600, new PrestigeStyle(new ChatColor[]{ChatColor.AQUA, ChatColor.YELLOW, ChatColor.YELLOW, ChatColor.GOLD}, ChatColor.LIGHT_PURPLE));
        PRESTIGE_STYLES.put(4700, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_RED, ChatColor.RED, ChatColor.RED, ChatColor.BLUE}, ChatColor.DARK_BLUE));
        PRESTIGE_STYLES.put(4800, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_PURPLE, ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW}, ChatColor.AQUA));
        PRESTIGE_STYLES.put(4900, new PrestigeStyle(new ChatColor[]{ChatColor.GREEN, ChatColor.WHITE, ChatColor.WHITE, ChatColor.GREEN}, ChatColor.GREEN));
        PRESTIGE_STYLES.put(5000, new PrestigeStyle(new ChatColor[]{ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.BLUE, ChatColor.BLUE}, ChatColor.DARK_BLUE));
        // All 5000+ stars use the 5000 star style (handled in getPrestigeStyle method)
    }
}
