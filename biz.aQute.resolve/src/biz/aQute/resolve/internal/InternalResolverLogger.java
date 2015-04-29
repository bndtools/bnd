package biz.aQute.resolve.internal;

import org.apache.felix.resolver.Logger;

import biz.aQute.resolve.ResolverLogger;

public class InternalResolverLogger extends Logger {

    private final ResolverLogger logger;

    public InternalResolverLogger(ResolverLogger logger) {
        super(logger.getLogLevel());
        this.logger = logger;
    }

    @Override
    protected void doLog(int level, String msg, Throwable throwable) {
    	logger.log(level, msg, throwable);
    }
}
