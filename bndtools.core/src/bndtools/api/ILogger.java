package bndtools.api;

public interface ILogger {

    void logError(String message, Throwable exception);

    void logWarning(String message, Throwable exception);

    void logInfo(String message, Throwable exception);

}
