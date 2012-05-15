package aQute.lib.deployer.repository;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class NullLogService implements LogService {
	
	public void log(int level, String message) {
	}

	public void log(int level, String message, Throwable t) {
	}

	public void log(ServiceReference sr, int level, String message) {
	}

	public void log(ServiceReference sr, int level, String message, Throwable t) {
	}

}
