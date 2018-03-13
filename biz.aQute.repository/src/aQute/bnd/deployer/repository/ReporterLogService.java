package aQute.bnd.deployer.repository;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.service.reporter.Reporter;

public class ReporterLogService implements LogService {
	private final static Logger	logger	= LoggerFactory.getLogger(ReporterLogService.class);

	private final Reporter		reporter;

	public ReporterLogService(Reporter reporter) {
		this.reporter = reporter;
	}

	@Override
	public void log(int level, String message) {
		log(null, level, message, null);
	}

	@Override
	public void log(int level, String message, Throwable t) {
		log(null, level, message, t);
	}

	@Override
	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	@Override
	public void log(ServiceReference sr, int level, String message, Throwable t) {
		switch (level) {
			case LOG_ERROR :
				if (t == null) {
					reporter.error("%s", message);
				} else {
					reporter.exception(t, "%s", message);
				}
				break;
			case LOG_WARNING :
				if (t == null) {
					reporter.warning("%s", message);
				} else {
					reporter.warning("%s [%s]", message, t);
				}
				break;
			case LOG_INFO :
				logger.info("{}", message, t);
				break;
			default :
				logger.debug("{}", message, t);
				break;
		}
	}

}
