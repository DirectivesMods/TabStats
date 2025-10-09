package tabstats.playerapi.api.games.duels;

/* custom enum for duels api retrieval*/
public enum DuelsModes {
    ALL_MODES("Overall", "wins"),
    OVERALL("Overall", "wins"),
    UHC("UHC", "uhc_duel_wins"),
    NO_DEBUFF("No-Debuff", "potion_duel_wins"),
    POTION("No-Debuff", "potion_duel_wins"),
    SUMO("Sumo", "sumo_duel_wins"),
    CLASSIC("Classic", "classic_duel_wins"),
    COMBO("Combo", "combo_duel_wins"),
    BOW("Bow", "bow_duel_wins"),
    SKYWARS("Skywars", "sw_duel_wins"),
    SW("Skywars", "sw_duel_wins"),
    BLITZ("Blitz", "blitz_duel_wins"),
    MEGAWALLS("Mega Walls", "mw_duel_wins"),
    MEGA_WALLS("Mega Walls", "mw_duel_wins"),
    MW("Mega Walls", "mw_duel_wins"),
    BOWSPLEEF("Bow Spleef", "bowspleef_duel_wins"),
    TNT_GAMES("Bow Spleef", "bowspleef_duel_wins"),
    BRIDGE("Bridge", "bridge_duel_wins"),
    BOXING("Boxing", "boxing_duel_wins"),
    OP("OP", "op_duel_wins"),
    PARKOUR("Parkour", "wins");

    private final String name;
    private final String winsJson;

    DuelsModes(String name, String winsJson) {
        this.name = name;
        this.winsJson = winsJson;
    }

    public String getName() {
        return this.name;
    }

    public String getWinsJson() {
        return this.winsJson;
    }
}
