package biz.aQute.resolve;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import aQute.service.reporter.Reporter;

public class LogReporter extends org.apache.felix.resolver.Logger implements LogService {
	private Reporter reporter;

	class Error extends Silent {

		public void log(String message) {
			reporter.error("%s", message);
		}

		public void log(String message, Throwable exception) {
			reporter.error("%s : %s", message, exception);
		}

		public void log(ServiceReference< ? > sr, String message) {
			reporter.error("%s (%s)", message, sr);
		}

		public void log(ServiceReference< ? > sr, String message, Throwable exception) {
			reporter.error("%s : %s (%s)", message, exception, sr);
		}
	}

	class Warning extends Silent {

		public void log(String message) {
			reporter.warning("%s", message);
		}

		public void log(String message, Throwable exception) {
			reporter.warning("%s : %s", message, exception);
		}

		public void log(ServiceReference< ? > sr, String message) {
			reporter.warning("%s (%s)", message, sr);
		}

		public void log(ServiceReference< ? > sr, String message, Throwable exception) {
			reporter.warning("%s : %s (%s)", message, exception, sr);
		}
	}

	class Trace extends Silent {

		public void log(String message) {
			reporter.trace("%s", message);
		}

		public void log(String message, Throwable exception) {
			reporter.trace("%s : %s", message, exception);
		}

		public void log(ServiceReference< ? > sr, String message) {
			reporter.trace("%s (%s)", message, sr);
		}

		public void log(ServiceReference< ? > sr, String message, Throwable exception) {
			reporter.trace("%s : %s (%s)", message, exception, sr);
		}
	}

	class Silent {

		public void log(String message) {}

		public void log(String message, Throwable exception) {}

		public void log(ServiceReference< ? > sr, String message) {}

		public void log(ServiceReference< ? > sr, String message, Throwable exception) {}
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
