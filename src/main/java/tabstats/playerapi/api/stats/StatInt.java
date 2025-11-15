package tabstats.playerapi.api.stats;

import com.google.gson.JsonObject;

public class StatInt extends Stat {
    private int value;
    private boolean loadedValue;

    /**
     * @param statName Name of the Stat
     * @param jsonName Json Name of the Stat in Hypixel's API
     * @param gameObject JsonObject of the desired Game Stat
     */
    public StatInt(String statName, String jsonName, JsonObject gameObject) {
        super(statName, jsonName, gameObject);
    }

    public StatInt(String statName) {
        super(statName);
    }

    @Override
    public void setStat() {
        try {
            this.value = Integer.parseInt(gameObject.get(jsonName).getAsString());
            this.loadedValue = true;
        } catch (Exception ex) {
            this.value = 0;
            this.loadedValue = false;
        }
    }

    public void setValue(int value) { this.value = value; }

    public int getValue() { return this.value; }

    public boolean isLoadedValue() {
        return loadedValue;
    }

    @Override
    public StatType getType() {
        return StatType.INT;
    }
}
