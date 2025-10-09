package tabstats.playerapi.exception;

public class ApiThrottleException extends ApiRequestException {
    private final boolean global;

    public ApiThrottleException(boolean global) {
        super(global ? "Hypixel API is currently throttling all requests" : "Hypixel API key is being throttled");
        this.global = global;
    }

    public boolean isGlobal() {
        return global;
    }
}
