package tabstats.playerapi.exception;

public class PlayerNullException extends Exception {
    public PlayerNullException() {
        super("Player returned as null");
    }
}
