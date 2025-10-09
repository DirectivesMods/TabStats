package tabstats.playerapi.exception;

public class ApiRequestException extends Exception {
    public ApiRequestException() {
        super("Api Request UnSuccessful");
    }

    public ApiRequestException(String message) {
        super(message);
    }
}
