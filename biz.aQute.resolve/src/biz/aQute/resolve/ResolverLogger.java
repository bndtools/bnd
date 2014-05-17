package biz.aQute.resolve;

import java.io.*;

public class ResolverLogger {

    public static final int DEFAULT_LEVEL = 4;

    public static final int LOG_ERROR = 1;
    public static final int LOG_WARNING = 2;
    public static final int LOG_INFO = 3;
    public static final int LOG_DEBUG = 4;

    private final StringWriter writer = new StringWriter();
    private final PrintWriter printer = new PrintWriter(writer);

    private int level;

    public ResolverLogger() {
        this(DEFAULT_LEVEL);
    }

    public ResolverLogger(int level) {
        this.level = level;
    }

    public void log(int level, String msg, Throwable throwable) {
        String s = "";
        s = s + msg;
        if (throwable != null)
            s = s + " (" + throwable + ")";
        switch (level) {
        case LOG_DEBUG :
            printer.println("DEBUG: " + s);
            break;
        case LOG_ERROR :
            printer.println("ERROR: " + s);
            if (throwable != null) {
                throwable.printStackTrace(printer);
            }
            break;
        case LOG_INFO :
            printer.println("INFO: " + s);
            break;
        case LOG_WARNING :
            printer.println("WARNING: " + s);
            break;
        default :
            printer.println("UNKNOWN[" + level + "]: " + s);
        }

    }

    public String getLog() {
        return writer.toString();
    }

    public int getLogLevel() {
    	return level;
    }
}
