package aQute.lib.deployer.repository;

import org.osgi.framework.*;
import org.osgi.service.log.*;

import aQute.service.reporter.*;

public class ReporterLogService implements LogService {

	private final Reporter	reporter;

	public ReporterLogService(Reporter reporter) {
		this.reporter = reporter;
	}

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable t) {
		log(null, level, message, t);
	}

	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	public void log(ServiceReference sr, int level, String message, Throwable t) {
		if (t != null)
			message += " [" + t + "]";

		if (reporter != null) {
			if (level <= LOG_ERROR)
				reporter.error(message);
			else if (level == LOG_WARNING)
				reporter.warning(message);
			else if (level == LOG_INFO || level == LOG_DEBUG)
				reporter.trace(message);
		}
	}

}
