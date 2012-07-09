package bndtools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import bndtools.api.ILogger;

public class Logger {
    private static org.eclipse.core.runtime.Plugin plugin = null;
    private static final String PLUGIN_ID = Plugin.PLUGIN_ID;

    public static final ILogger logger = new ILogger() {
        private String getStackTrace(Throwable t) {
            if (t == null) {
                return "No exception trace is available";
            }

            final Writer sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.toString();
        }

        private String constructSysErrString(IStatus status) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HHmmss.SSS");
            String formattedDate = formatter.format(new Date());
            return String.format("%s - %s - %s - %s%n%s", formattedDate, status.getSeverity(), status.getPlugin(), status.getMessage(), getStackTrace(status.getException()));
        }

        private Status constructStatus(int status, String message, Throwable exception) {
            return new Status(status, PLUGIN_ID, 0, message, exception);
        }

        private void log(int status, String message, Throwable exception) {
            logStatus(constructStatus(status, message, exception));
        }

        public void logError(String message, Throwable exception) {
            log(IStatus.ERROR, message, exception);
        }

        public void logWarning(String message, Throwable exception) {
            log(IStatus.WARNING, message, exception);
        }

        public void logInfo(String message, Throwable exception) {
            log(IStatus.INFO, message, exception);
        }

        public void logStatus(IStatus status) {
            if (plugin == null) {
                System.err.println(constructSysErrString(status));
                return;
            }
            plugin.getLog().log(status);
        }

    };

    static void setPlugin(org.eclipse.core.runtime.Plugin p) {
        plugin = p;
    }

    public static final ILogger getLogger() {
        return logger;
    }
}
