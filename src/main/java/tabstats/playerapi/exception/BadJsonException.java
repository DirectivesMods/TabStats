package tabstats.playerapi.exception;

public class BadJsonException extends Exception {
    public BadJsonException() {
        super("Hypixel API returned Bad Json. Maybe the API is down?");
    }
}
