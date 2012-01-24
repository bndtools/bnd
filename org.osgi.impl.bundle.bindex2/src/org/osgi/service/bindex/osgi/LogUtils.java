package org.osgi.service.bindex.osgi;

import static org.osgi.service.log.LogService.*;

public final class LogUtils {

	private LogUtils() {
	}

	public static String formatLogLevel(int level) {
		switch (level) {
		case LOG_DEBUG:
			return "DEBUG";
		case LOG_INFO:
			return "INFO";
		case LOG_WARNING:
			return "WARNING";
		case LOG_ERROR:
			return "ERROR";
		default:
			return "unknown";
		}
	}
}
