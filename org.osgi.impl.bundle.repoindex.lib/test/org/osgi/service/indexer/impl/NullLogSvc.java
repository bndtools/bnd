package org.osgi.service.indexer.impl;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class NullLogSvc implements LogService {

	public void log(int level, String message) {
	}

	public void log(int level, String message, Throwable exception) {
	}

	public void log(ServiceReference sr, int level, String message) {
	}

	public void log(ServiceReference sr, int level, String message, Throwable exception) {
	}

}
