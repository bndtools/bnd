package biz.aQute.resolve;

import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolutionError;
import org.osgi.resource.Resource;

class InternalResolverLogger extends Logger {

	private final ResolverLogger logger;

	public InternalResolverLogger(ResolverLogger logger) {
		super(logger.getLogLevel());
		this.logger = logger;
	}

	@Override
	protected void doLog(int level, String msg, Throwable throwable) {
		logger.log(level, msg, throwable);
	}

	@Override
	public void logUsesConstraintViolation(Resource resource, ResolutionError error) {
		logger.log(logger.getLogLevel(), String.format("Resource: %s, ResolutionError: %s", resource, error),
			error.toException());
	}
}
