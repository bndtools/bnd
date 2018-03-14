package aQute.bnd.main;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.service.reporter.Reporter;

public class ReporterLogger implements LogService {
	private final static Logger	logger	= LoggerFactory.getLogger(ReporterLogger.class);

	private Reporter			reporter;
	private int					level;

	public ReporterLogger(Reporter reporter, int level) {
		this.reporter = reporter;
		this.level = level;
	}

	@Override
	public void log(int level, String message) {
		if (level > this.level)
			return;

		switch (level) {
			case LogService.LOG_ERROR :
				reporter.error("%s", message);
				return;

			case LogService.LOG_WARNING :
				reporter.warning("%s", message);
				return;

			case LogService.LOG_INFO :
				logger.info("{}", message);
				return;

			default :
				logger.debug("{}", message);
				return;
		}
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		if (level > this.level)
			return;

		switch (level) {
			case LogService.LOG_ERROR :
				reporter.error("%s: %s", exception, message);
				return;

			case LogService.LOG_WARNING :
				reporter.warning("%s: %s", exception, message);
				return;

			case LogService.LOG_INFO :
				logger.info("{}", message, exception);
				return;

			default :
				logger.debug("{}", message, exception);
				return;
		}
	}

	@Override
	public void log(ServiceReference sr, int level, String message) {
		switch (level) {
			case LogService.LOG_ERROR :
				reporter.error("%s %s", sr, message);
				return;

			case LogService.LOG_WARNING :
				reporter.warning("%s: %s", sr, message);
				return;

			case LogService.LOG_INFO :
				logger.info("{}: {}", sr, message);
				return;

			default :
				logger.debug("{}: {}", sr, message);
				return;
		}
	}

	@Override
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		switch (level) {
			case LogService.LOG_ERROR :
				reporter.error("%s:%s: %s", sr, exception, message);
				return;

			case LogService.LOG_WARNING :
				reporter.warning("%s:%s: %s", sr, exception, message);
				return;

			case LogService.LOG_INFO :
				logger.info("{}: {}", sr, message, exception);
				return;

			default :
				logger.debug("{}: {}", sr, message, exception);
				return;
		}
	}

}
