package tabstats.playerapi.api.games.bedwars;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import tabstats.TabStats;
import tabstats.config.ModConfig;
import tabstats.playerapi.HPlayer;
import tabstats.playerapi.StatWorld;
import tabstats.playerapi.api.UrchinAPI;
import tabstats.playerapi.api.UrchinAPI.UrchinReport;
import tabstats.playerapi.api.UrchinAPI.UrchinReportType;
import tabstats.playerapi.api.games.HGameBase;
import tabstats.playerapi.api.stats.StatInt;
import tabstats.playerapi.exception.ApiRequestException;
import tabstats.playerapi.exception.BadJsonException;
import tabstats.util.ChatColor;
import tabstats.util.Handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class BedwarsUtil extends HGameBase {
    private static final UrchinAPI URCHIN_API = new UrchinAPI();
    private static final UrchinReportMatrixEntry[] URCHIN_REPORT_MATRIX = new UrchinReportMatrixEntry[]{
            entry(UrchinReportType.SNIPER, ChatColor.DARK_RED + "S", ChatColor.DARK_RED + "Sniper"),
            entry(UrchinReportType.POSSIBLE_SNIPER, ChatColor.RED + "PS", ChatColor.RED + "Possible Sniper"),
            entry(UrchinReportType.LEGIT_SNIPER, ChatColor.RED + "LS", ChatColor.RED + "Legit Sniper"),
            entry(UrchinReportType.CONFIRMED_CHEATER, ChatColor.DARK_PURPLE + "CC", ChatColor.DARK_PURPLE + "Confirmed Cheater"),
            entry(UrchinReportType.BLATANT_CHEATER, ChatColor.GOLD + "BC", ChatColor.GOLD + "Blatant Cheater"),
            entry(UrchinReportType.CLOSET_CHEATER, ChatColor.GOLD + "CC", ChatColor.GOLD + "Closet Cheater"),
            entry(UrchinReportType.CAUTION, ChatColor.YELLOW + "C", ChatColor.YELLOW + "Caution"),
            entry(UrchinReportType.ACCOUNT, ChatColor.DARK_AQUA + "A", ChatColor.DARK_AQUA + "Account"),
            entry(UrchinReportType.INFO, ChatColor.DARK_GRAY + "I", ChatColor.DARK_GRAY + "Info")
    };
    private static final Map<UrchinReportType, UrchinReportMatrixEntry> URCHIN_REPORT_LOOKUP = new EnumMap<>(UrchinReportType.class);
    private static final ConcurrentMap<String, CachedUrchinTag> URCHIN_TAG_CACHE = new ConcurrentHashMap<>();
    private static final UrchinLookupDispatcher URCHIN_LOOKUP_DISPATCHER = new UrchinLookupDispatcher();
    private static final long URCHIN_BATCH_WINDOW_MS = 4000L;
    private static final BatchWindowTracker URCHIN_BATCH_TRACKER = new BatchWindowTracker();
    private static final UrchinReportType[] URCHIN_PRIORITY = new UrchinReportType[]{
            UrchinReportType.CONFIRMED_CHEATER,
            UrchinReportType.BLATANT_CHEATER,
            UrchinReportType.CLOSET_CHEATER,
            UrchinReportType.SNIPER,
            UrchinReportType.POSSIBLE_SNIPER,
            UrchinReportType.LEGIT_SNIPER,
            UrchinReportType.CAUTION,
            UrchinReportType.ACCOUNT,
            UrchinReportType.INFO
    };
    protected static final String NO_RESPONSE_TAG = ChatColor.GRAY + "-";

    static {
        for (UrchinReportMatrixEntry entry : URCHIN_REPORT_MATRIX) {
            URCHIN_REPORT_LOOKUP.put(entry.getType(), entry);
        }
    }

    private static UrchinReportMatrixEntry entry(UrchinReportType type, String displayValue, String chatLabel) {
        return new UrchinReportMatrixEntry(type, displayValue, chatLabel);
    }

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
        if (finals < 10000) return ChatColor.RED;
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

    protected void enqueueUrchinLookup(String identity, Consumer<CachedUrchinTag> callback) {
        if (identity == null || callback == null) {
            return;
        }
        Long batchJoinTime = tryEnterUrchinBatchWindow();
        if (batchJoinTime != null) {
            URCHIN_LOOKUP_DISPATCHER.enqueue(identity, callback, batchJoinTime);
            return;
        }

        Handler.asExecutor(() -> {
            Map<String, CachedUrchinTag> result = resolveUrchinBatch(getActiveUrchinApiKey(), Collections.singletonList(identity));
            CachedUrchinTag tag = result.get(identity);
            callback.accept(tag == null ? createPendingTag() : tag);
        });
    }

    private static Map<String, CachedUrchinTag> resolveUrchinBatch(String apiKey, Collection<String> identities) {
        Map<String, CachedUrchinTag> results = new LinkedHashMap<>();
        if (identities == null || identities.isEmpty()) {
            return results;
        }

        String trimmedKey = apiKey == null ? "" : apiKey.trim();
        if (trimmedKey.isEmpty()) {
            for (String identity : identities) {
                results.put(identity, cacheTag(identity, createNoHitTag()));
            }
            return results;
        }

        try {
            Map<String, List<UrchinReport>> response = URCHIN_API.fetchPlayerReports(trimmedKey, identities);
            Map<String, List<UrchinReport>> normalizedResponse = new HashMap<>();
            if (response != null) {
                for (Map.Entry<String, List<UrchinReport>> entry : response.entrySet()) {
                    String normalizedKey = normalizeIdentity(entry.getKey());
                    if (normalizedKey != null) {
                        normalizedResponse.put(normalizedKey, entry.getValue());
                    }
                }
            }

            List<UrchinReport> fallback = normalizedResponse.isEmpty() ? null : normalizedResponse.values().iterator().next();
            for (String identity : identities) {
                String normalizedIdentity = normalizeIdentity(identity);
                List<UrchinReport> reports = normalizedIdentity == null ? null : normalizedResponse.get(normalizedIdentity);
                if (reports == null) {
                    reports = fallback;
                }
                results.put(identity, toCachedUrchinTag(identity, reports));
            }
        } catch (ApiRequestException | BadJsonException ex) {
            for (String identity : identities) {
                results.put(identity, createPendingTag());
            }
        }

        return results;
    }

    private static Long tryEnterUrchinBatchWindow() {
        if (URCHIN_BATCH_WINDOW_MS <= 0L) {
            return null;
        }
        TabStats instance = TabStats.getTabStats();
        if (instance == null) {
            return null;
        }
        StatWorld world = instance.getStatWorld();
        if (world == null) {
            return null;
        }
        long joinTime = world.getLastWorldJoinTime();
        if (joinTime <= 0L) {
            return null;
        }
        if (System.currentTimeMillis() - joinTime > URCHIN_BATCH_WINDOW_MS) {
            return null;
        }
        return URCHIN_BATCH_TRACKER.tryAcquire(joinTime) ? joinTime : null;
    }

    /**
     * Batches Urchin lookups so that world joins don't spam the API with one request per player.
     */
    private static final class UrchinLookupDispatcher {
        private static final long BATCH_DEBOUNCE_MS = 150L;

        private final Object lock = new Object();
        private final LinkedHashMap<String, List<Consumer<CachedUrchinTag>>> pendingLookups = new LinkedHashMap<>();
        private boolean draining;

        void enqueue(String identity, Consumer<CachedUrchinTag> callback, long joinTime) {
            synchronized (lock) {
                pendingLookups.computeIfAbsent(identity, key -> new ArrayList<>()).add(callback);
                if (!draining) {
                    draining = true;
                    Handler.asExecutor(() -> processOnce(joinTime));
                }
            }
        }

        private void processOnce(long joinTime) {
            delay(BATCH_DEBOUNCE_MS);
            List<LookupRequest> batch = pollBatch();
            if (!batch.isEmpty()) {
                dispatch(batch);
            }
            URCHIN_BATCH_TRACKER.markConsumed(joinTime);
            synchronized (lock) {
                pendingLookups.clear();
                draining = false;
            }
        }

        private List<LookupRequest> pollBatch() {
            synchronized (lock) {
                if (pendingLookups.isEmpty()) {
                    return Collections.emptyList();
                }

                List<LookupRequest> batch = new ArrayList<>(pendingLookups.size());
                Iterator<Map.Entry<String, List<Consumer<CachedUrchinTag>>>> iterator = pendingLookups.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, List<Consumer<CachedUrchinTag>>> entry = iterator.next();
                    batch.add(new LookupRequest(entry.getKey(), new ArrayList<>(entry.getValue())));
                    iterator.remove();
                }
                return batch;
            }
        }

        private void dispatch(List<LookupRequest> batch) {
            if (batch.isEmpty()) {
                return;
            }

            List<String> identities = new ArrayList<>(batch.size());
            for (LookupRequest request : batch) {
                identities.add(request.identity);
            }

            Map<String, CachedUrchinTag> resolved = resolveUrchinBatch(getActiveUrchinApiKey(), identities);
            for (LookupRequest request : batch) {
                CachedUrchinTag tag = resolved.get(request.identity);
                if (tag == null) {
                    tag = createPendingTag();
                }
                for (Consumer<CachedUrchinTag> callback : request.callbacks) {
                    try {
                        callback.accept(tag);
                    } catch (RuntimeException ignored) {
                        // Avoid breaking the dispatcher if a callback misbehaves.
                    }
                }
            }
        }

        private void delay(long millis) {
            if (millis <= 0) {
                return;
            }
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        private static final class LookupRequest {
            private final String identity;
            private final List<Consumer<CachedUrchinTag>> callbacks;

            private LookupRequest(String identity, List<Consumer<CachedUrchinTag>> callbacks) {
                this.identity = identity;
                this.callbacks = callbacks == null ? Collections.emptyList() : callbacks;
            }
        }
    }

    private static final class BatchWindowTracker {
        private long trackedJoinTime = -1L;
        private boolean consumed;

        synchronized boolean tryAcquire(long joinTime) {
            if (joinTime <= 0L) {
                return false;
            }
            if (joinTime != trackedJoinTime) {
                trackedJoinTime = joinTime;
                consumed = false;
            }
            return !consumed;
        }

        synchronized void markConsumed(long joinTime) {
            if (joinTime == trackedJoinTime) {
                consumed = true;
            }
        }
    }

    private static CachedUrchinTag toCachedUrchinTag(String identity, List<UrchinReport> reports) {
        if (identity == null) {
            return createPendingTag();
        }

        if (reports == null) {
            return createPendingTag();
        }

        UrchinReport report = extractPriorityReport(reports);
        CachedUrchinTag result = report == null ? createNoHitTag() : buildTagFromReport(report);
        return cacheTag(identity, result);
    }

    private static UrchinReport extractPriorityReport(List<UrchinReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return null;
        }

        for (UrchinReportType priority : URCHIN_PRIORITY) {
            for (UrchinReport report : reports) {
                if (report != null && report.getType() == priority) {
                    return report;
                }
            }
        }

        for (UrchinReport report : reports) {
            if (report != null && getMatrixEntry(report.getType()) != null) {
                return report;
            }
        }

        return null;
    }

    private static CachedUrchinTag buildTagFromReport(UrchinReport report) {
        String display = formatUrchinTag(report.getType());
        String chatLabel = formatUrchinChatTag(report.getType());
        return new CachedUrchinTag(
                display,
                chatLabel,
                report.getType(),
                sanitize(report.getReason()),
                formatDate(report.getAddedOn())
        );
    }

    protected static CachedUrchinTag createNoHitTag() {
        return new CachedUrchinTag(NO_RESPONSE_TAG, NO_RESPONSE_TAG, null, "", "");
    }

    protected static CachedUrchinTag createPendingTag() {
        return new CachedUrchinTag("", "", null, "", "");
    }

    private static final class UrchinReportMatrixEntry {
        private final UrchinReportType type;
        private final String displayValue;
        private final String chatLabel;

        private UrchinReportMatrixEntry(UrchinReportType type, String displayValue, String chatLabel) {
            this.type = type;
            this.displayValue = displayValue == null ? NO_RESPONSE_TAG : displayValue;
            this.chatLabel = chatLabel == null ? this.displayValue : chatLabel;
        }

        private UrchinReportType getType() {
            return type;
        }

        private String getDisplayValue() {
            return displayValue;
        }

        private String getChatLabel() {
            return chatLabel;
        }
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    private static String formatDate(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        int tIndex = trimmed.indexOf('T');
        return tIndex > 0 ? trimmed.substring(0, tIndex) : trimmed;
    }

    private static String formatUrchinTag(UrchinReportType type) {
        UrchinReportMatrixEntry entry = getMatrixEntry(type);
        return entry == null ? NO_RESPONSE_TAG : entry.getDisplayValue();
    }

    private static String formatUrchinChatTag(UrchinReportType type) {
        UrchinReportMatrixEntry entry = getMatrixEntry(type);
        return entry == null ? NO_RESPONSE_TAG : entry.getChatLabel();
    }

    private static UrchinReportMatrixEntry getMatrixEntry(UrchinReportType type) {
        if (type == null) {
            return null;
        }
        return URCHIN_REPORT_LOOKUP.get(type);
    }

    protected static CachedUrchinTag getCachedUrchinTag(String identity) {
        String normalized = normalizeIdentity(identity);
        return normalized == null ? null : URCHIN_TAG_CACHE.get(normalized);
    }

    private static CachedUrchinTag cacheTag(String identity, CachedUrchinTag value) {
        String normalized = normalizeIdentity(identity);
        if (normalized == null || value == null) {
            return value;
        }

        CachedUrchinTag stored = URCHIN_TAG_CACHE.compute(normalized, (key, existing) -> {
            if (existing != null && existing.samePayload(value)) {
                return existing;
            }
            return value;
        });

        return stored == null ? value : stored;
    }

    private static String normalizeIdentity(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    protected void announceTagIfNeeded(CachedUrchinTag data) {
        if (data == null || !data.shouldAnnounce() || !data.markAnnounced()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }

        Runnable task = () -> {
            if (mc.thePlayer == null) {
                return;
            }

            String formattedName = getFormattedPlayerLabel();
            String reason = data.getReason().isEmpty() ? "Unknown" : data.getReason();
            String addedOn = data.getAddedOn().isEmpty() ? "Unknown" : data.getAddedOn();
            String chatLabel = data.getChatLabel();

            String message =
                    formattedName + ChatColor.YELLOW + " is tagged on Urchin!" + " \n" +
                    ChatColor.GRAY + "- Tag: " + chatLabel + " \n" +
                    ChatColor.GRAY + "- Reason: " + ChatColor.WHITE + reason + " \n" +
                    ChatColor.GRAY + "- Added on: " + ChatColor.WHITE + addedOn + ChatColor.RESET;

            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        };

        if (mc.isCallingFromMinecraftThread()) {
            task.run();
        } else {
            mc.addScheduledTask(task);
        }
    }

    protected String getFormattedPlayerLabel() {
        String fallbackName = getPlayerName();
        TabStats tabStats = TabStats.getTabStats();
        if (tabStats != null) {
            StatWorld statWorld = tabStats.getStatWorld();
            if (statWorld != null) {
                HPlayer player = statWorld.getPlayerByName(fallbackName);
                if (player != null) {
                    String rank = player.getPlayerRank();
                    String name = player.getPlayerName();
                    return (rank == null ? "" : rank) + (name == null ? "" : name);
                }
            }
        }

        if (fallbackName == null || fallbackName.trim().isEmpty()) {
            return ChatColor.WHITE + "Unknown player";
        }

        return ChatColor.WHITE + fallbackName;
    }

    protected static String getActiveUrchinApiKey() {
        ModConfig cfg = ModConfig.getInstance();
        if (cfg == null) {
            return "";
        }
        String configured = cfg.getUrchinApiKey();
        return configured == null ? "" : configured.trim();
    }

    protected String getLookupIdentity() {
        String uuid = getPlayerUUID();
        if (uuid == null) {
            return null;
        }
        String trimmed = uuid.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected static final class CachedUrchinTag {
        private final String displayValue;
        private final String chatLabel;
        private final UrchinReportType type;
        private final String reason;
        private final String addedOn;
        private final AtomicBoolean announced = new AtomicBoolean(false);

        private CachedUrchinTag(String displayValue, String chatLabel, UrchinReportType type, String reason, String addedOn) {
            this.displayValue = displayValue == null ? NO_RESPONSE_TAG : displayValue;
            this.chatLabel = chatLabel == null ? this.displayValue : chatLabel;
            this.type = type;
            this.reason = reason == null ? "" : reason;
            this.addedOn = addedOn == null ? "" : addedOn;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public String getChatLabel() {
            return chatLabel;
        }

        public String getReason() {
            return reason;
        }

        public String getAddedOn() {
            return addedOn;
        }

        public boolean shouldAnnounce() {
            return type != null && !NO_RESPONSE_TAG.equals(displayValue);
        }

        public boolean markAnnounced() {
            return announced.compareAndSet(false, true);
        }

        public boolean samePayload(CachedUrchinTag other) {
            if (other == null) {
                return false;
            }
            return this.type == other.type
                    && this.displayValue.equals(other.displayValue)
                    && this.chatLabel.equals(other.chatLabel)
                    && this.reason.equals(other.reason)
                    && this.addedOn.equals(other.addedOn);
        }
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
