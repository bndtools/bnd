package aQute.libg.reporter.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

public class Slf4jReporter extends ReporterAdapter {
	final Logger logger;

	public Slf4jReporter(Class< ? > loggingClass) {
		logger = LoggerFactory.getLogger(loggingClass);
	}

	public Slf4jReporter() {
		logger = LoggerFactory.getLogger("default");
	}

	public SetLocation error(String format, Object... args) {
		SetLocation location = super.error(format, args);
		if (logger.isErrorEnabled()) {
			String msg = String.format(format, args);
			logger.error(msg);
		}
		return location;
	}

	public SetLocation warning(String format, Object... args) {
		SetLocation location = super.warning(format, args);
		if (logger.isWarnEnabled()) {
			String msg = String.format(format, args);
			logger.warn(msg);
		}
		return location;
	}

	public void trace(String format, Object... args) {
		super.trace(format, args);
		if (logger.isInfoEnabled()) {
			String msg = String.format(format, args);
			logger.info(msg);
		}
	}

	public void progress(float progress, String format, Object... args) {
		super.progress(progress, format, args);
		if (logger.isDebugEnabled()) {
			String msg = String.format(format, args);
			logger.debug(msg);
		}
	}

	public SetLocation exception(Throwable t, String format, Object... args) {
		SetLocation location = super.exception(t, format, args);
		if (logger.isErrorEnabled()) {
			String msg = String.format(format, args);
			logger.error(msg, t);
		}
		return location;
	}

	public static Reporter getAlternative(Class< ? > class1, Reporter reporter) {
		if (reporter == null)
			return new Slf4jReporter(class1);
		else
			return reporter;
	}

}
