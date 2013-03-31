package org.bndtools.core.resolve;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.felix.resolver.Logger;

class ResolverLogger extends Logger {

    public static final int DEFAULT_LEVEL = 4;

    private final StringWriter writer = new StringWriter();
    private final PrintWriter printer = new PrintWriter(writer);

    public ResolverLogger() {
        this(DEFAULT_LEVEL);
    }

    public ResolverLogger(int level) {
        super(level);
    }

    @Override
    protected void doLog(int level, String msg, Throwable throwable) {
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

    String getLog() {
        return writer.toString();
    }

}
