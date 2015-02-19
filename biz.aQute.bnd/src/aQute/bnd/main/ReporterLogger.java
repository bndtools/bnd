package aQute.bnd.main;

import org.osgi.framework.*;
import org.osgi.service.log.*;

import aQute.service.reporter.*;

public class ReporterLogger implements LogService {

	private Reporter	reporter;
	private int			level;

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

			default :
				reporter.trace("%s", message);
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

			default :
				reporter.trace("%s: %s", exception, message);
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

			default :
				reporter.trace("%s: %s", sr, message);
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

			default :
				reporter.trace("%s:%s: %s", sr, exception, message);
				return;
		}
	}

}
