package aQute.bnd.deployer.repository;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class NullLogService implements LogService {

	@Override
	public void log(int level, String message) {}

	@Override
	public void log(int level, String message, Throwable t) {}

	@Override
	public void log(ServiceReference sr, int level, String message) {}

	@Override
	public void log(ServiceReference sr, int level, String message, Throwable t) {}

}
