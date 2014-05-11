package biz.aQute.configadmin;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class PidTracker<C> extends ServiceTracker {

	private final Map<String, C> map = new HashMap<String, C>();

	PidTracker(BundleContext context, Class<C> clazz) {
		super(context, clazz.getName(), null);
	}

	@Override
	public Object addingService(ServiceReference reference) {
		@SuppressWarnings("unchecked")
		C service = (C) super.addingService(reference);
		String pid = (String) reference.getProperty(Constants.SERVICE_PID);
		if (pid != null) {
			synchronized (map) {
				map.put(pid, service);
			}
		}
		return service;
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		String pid = (String) reference.getProperty(Constants.SERVICE_PID);
		if (pid != null) {
			synchronized (map) {
				map.remove(pid);
			}
		}
		super.removedService(reference, service);
	}

	public C findPid(String pid) {
		return map.get(pid);
	}

}
