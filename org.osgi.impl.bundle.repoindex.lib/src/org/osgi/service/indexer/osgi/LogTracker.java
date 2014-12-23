package org.osgi.service.indexer.osgi;

import java.io.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.log.*;
import org.osgi.util.tracker.*;

class LogTracker extends ServiceTracker<LogService,LogService> implements LogService {

	public LogTracker(BundleContext context) {
		super(context, LogService.class, null);
	}

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		LogService log = getService();

		if (log != null)
			log.log(sr, level, message, exception);
		else {
			PrintStream stream = (level <= LogService.LOG_WARNING) ? System.err : System.out;
			if (message == null)
				message = "";
			Date now = new Date();
			stream.println(String.format("[%-7s] %tF %tT: %s", LogUtils.formatLogLevel(level), now, now, message));
			if (exception != null)
				exception.printStackTrace(stream);
		}
	}

}
