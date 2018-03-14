package biz.aQute.resolve;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.service.reporter.Reporter;

public class LogReporter extends org.apache.felix.resolver.Logger implements LogService {
	private final static Logger	logger	= LoggerFactory.getLogger(LogReporter.class);
	private Reporter			reporter;

	class Error extends Silent {

		@Override
		public void log(String message) {
			reporter.error("%s", message);
		}

		@Override
		public void log(String message, Throwable exception) {
			reporter.error("%s : %s", message, exception);
		}

		@Override
		public void log(ServiceReference<?> sr, String message) {
			reporter.error("%s (%s)", message, sr);
		}

		@Override
		public void log(ServiceReference<?> sr, String message, Throwable exception) {
			reporter.error("%s : %s (%s)", message, exception, sr);
		}
	}

	class Warning extends Silent {

		@Override
		public void log(String message) {
			reporter.warning("%s", message);
		}

		@Override
		public void log(String message, Throwable exception) {
			reporter.warning("%s : %s", message, exception);
		}

		@Override
		public void log(ServiceReference<?> sr, String message) {
			reporter.warning("%s (%s)", message, sr);
		}

		@Override
		public void log(ServiceReference<?> sr, String message, Throwable exception) {
			reporter.warning("%s : %s (%s)", message, exception, sr);
		}
	}

	class Trace extends Silent {

		@Override
		public void log(String message) {
			logger.debug("{}", message);
		}

		@Override
		public void log(String message, Throwable exception) {
			logger.debug("{}", message, exception);
		}

		@Override
		public void log(ServiceReference<?> sr, String message) {
			logger.debug("{} ({})", message, sr);
		}

		@Override
		public void log(ServiceReference<?> sr, String message, Throwable exception) {
			logger.debug("{} ({})", message, sr, exception);
		}
	}

	class Silent {

		public void log(String message) {}

		public void log(String message, Throwable exception) {}

		public void log(ServiceReference<?> sr, String message) {}

		public void log(ServiceReference<?> sr, String message, Throwable exception) {}
	}

	public LogReporter(Reporter reporter) {
		super(LogService.LOG_WARNING);
		this.reporter = reporter;
	}

	@Override
	public void log(ServiceReference sr, int level, String message) {
		getLevel(level).log(sr, message);
	}

	@Override
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		getLevel(level).log(sr, message, exception);
	}

	private Silent getLevel(int usedLevel) {
		if (usedLevel <= this.getLogLevel()) {
			switch (usedLevel) {
				case LogService.LOG_ERROR :
					return new Error();

				case LogService.LOG_WARNING :
					return new Warning();

				case LogService.LOG_INFO :
				case LogService.LOG_DEBUG :
				default :
					return new Trace();
			}
		}
		return new Silent();
	}
}
