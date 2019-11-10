package aQute.libg.reporter.slf4j;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.strings.Strings;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

public class Slf4jReporter extends ReporterAdapter {
	private final Logger logger;

	public Slf4jReporter(Class<?> loggingClass) {
		this(LoggerFactory.getLogger(loggingClass));
	}

	public Slf4jReporter() {
		this(LoggerFactory.getLogger("default"));
	}

	public Slf4jReporter(Logger logger) {
		this.logger = requireNonNull(logger);
	}

	@Override
	public SetLocation error(String format, Object... args) {
		SetLocation location = super.error(format, args);
		if (logger.isErrorEnabled()) {
			logger.error("{}", Strings.format(format, args));
		}
		return location;
	}

	@Override
	public SetLocation warning(String format, Object... args) {
		SetLocation location = super.warning(format, args);
		if (logger.isWarnEnabled()) {
			logger.warn("{}", Strings.format(format, args));
		}
		return location;
	}

	/**
	 */
	@Override
	public void trace(String format, Object... args) {
		super.trace(format, args);
		if (isTrace()) {
			if (logger.isInfoEnabled()) {
				logger.info("{}", Strings.format(format, args));
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("{}", Strings.format(format, args));
			}
		}
	}

	/**
	 * @deprecated Use SLF4J Logger.info() instead.
	 */
	@Override
	@Deprecated
	public void progress(float progress, String format, Object... args) {
		super.progress(progress, format, args);
		if (logger.isInfoEnabled()) {
			logger.info("{}", Strings.format(format, args));
		}
	}

	@Override
	public SetLocation exception(Throwable t, String format, Object... args) {
		SetLocation location = super.exception(t, format, args);
		if (logger.isErrorEnabled()) {
			logger.error("{}", Strings.format(format, args), t);
		}
		return location;
	}

	public static Reporter getAlternative(Class<?> class1, Reporter reporter) {
		if (reporter == null)
			return new Slf4jReporter(class1);
		else
			return reporter;
	}

}
