package bndtools.api;

import org.eclipse.core.runtime.IStatus;

public interface ILogger {
    
    void logStatus(IStatus status);

    void logError(String message, Throwable exception);

    void logWarning(String message, Throwable exception);

    void logInfo(String message, Throwable exception);

}
