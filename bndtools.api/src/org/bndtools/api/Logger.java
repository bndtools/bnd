package org.bndtools.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class Logger implements ILogger {

	private static final Logger					NULL_BUNDLE_LOGGER	= new Logger(null);
	private static final Map<Bundle, Logger>	CACHE				= new HashMap<>();

	public synchronized static ILogger getLogger(Class<?> clazz) {
		Bundle bundle = FrameworkUtil.getBundle(clazz);
		if (bundle == null)
			return NULL_BUNDLE_LOGGER;

		Logger result = CACHE.get(bundle);
		if (result != null)
			return result;

		result = new Logger(bundle);
		CACHE.put(bundle, result);
		return result;
	}

	private final Bundle bundle;

	private Logger(Bundle bundle) {
		this.bundle = bundle;
	}

	private String getStackTrace(Throwable t) {
		if (t == null) {
			return "No exception trace is available";
		}

		final Writer sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
		.ofPattern("yyyyMMdd HHmmss.SSS", Locale.ROOT)
		.withZone(ZoneId.systemDefault());

	private String constructSysErrString(IStatus status) {
		String formattedDate = DATE_TIME_FORMATTER.format(Instant.now());
		return String.format("%s - %s - %s - %s%n%s", formattedDate, status.getSeverity(), status.getPlugin(),
			status.getMessage(), getStackTrace(status.getException()));
	}

	private Status constructStatus(int status, String message, Throwable exception) {
		return new Status(status, bundle.getSymbolicName(), 0, message, exception);
	}

	private void log(int status, String message, Throwable exception) {
		logStatus(constructStatus(status, message, exception));
	}

	@Override
	public void logError(String message, Throwable exception) {
		log(IStatus.ERROR, message, exception);
	}

	@Override
	public void logWarning(String message, Throwable exception) {
		log(IStatus.WARNING, message, exception);
	}

	@Override
	public void logInfo(String message, Throwable exception) {
		log(IStatus.INFO, message, exception);
	}

	@Override
	public void logStatus(IStatus status) {
		if (bundle != null)
			InternalPlatform.getDefault()
				.getLog(bundle)
				.log(status);
		else
			System.err.println(constructSysErrString(status));
	}

}
