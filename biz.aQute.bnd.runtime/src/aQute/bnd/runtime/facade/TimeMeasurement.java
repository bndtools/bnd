package aQute.bnd.runtime.facade;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import aQute.bnd.runtime.facade.FrameworkFacade.ServiceTiming;
import aQute.bnd.runtime.facade.FrameworkFacade.XFrameworkEventDTO;

class TimeMeasurement implements Closeable, ServiceListener, BundleListener, FrameworkListener {
	final Map<Long, ServiceTiming>	timeseries		= new HashMap<>();
	final Map<Long, Long>			bundleTiming	= new HashMap<>();
	final List<XFrameworkEventDTO>	frameworkEvents	= new ArrayList<>();
	private BundleContext			context;
	private long					startTime;

	TimeMeasurement(BundleContext context) {
		this.context = context;
		startTime = System.nanoTime();
		context.addServiceListener(this);
		context.addBundleListener(this);
		context.addFrameworkListener(this);
	}

	@Override
	public void close() throws IOException {
		context.removeServiceListener(this);
		context.removeBundleListener(this);
		context.removeFrameworkListener(this);
	}

	@Override
	public synchronized void serviceChanged(ServiceEvent event) {
		long id = (long) event.getServiceReference()
			.getProperty(Constants.SERVICE_ID);

		ServiceTiming st = timeseries.computeIfAbsent(id, (key) -> {
			ServiceTiming serviceTiming = new ServiceTiming();
			ServiceReference<?> ref = event.getServiceReference();
			return serviceTiming;
		});

		switch (event.getType()) {
			case ServiceEvent.REGISTERED :
				st.registered = true;
				break;
			case ServiceEvent.MODIFIED :
				break;
			case ServiceEvent.UNREGISTERING :
				st.unregistered = true;
				break;
		}

		long time = System.nanoTime() - startTime;
		st.timings.add(time);
	}

	public long getStart(long bundleId) {
		return bundleTiming.getOrDefault(bundleId, 0L);
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.STARTING) {
			long time = System.nanoTime() - startTime;
			bundleTiming.put(event.getBundle()
				.getBundleId(), time);
		}
	}

	@Override
	public void frameworkEvent(FrameworkEvent event) {
		XFrameworkEventDTO xfed = new XFrameworkEventDTO();
		xfed.time = xfed.bundleId = event.getBundle()
			.getBundleId();

		Throwable throwable = event.getThrowable();
		if (throwable != null) {
			xfed.message = throwable.getMessage();
		}
		xfed.type = event.getType();
		frameworkEvents.add(xfed);
	}

	ServiceTiming getTiming(long id) {
		return timeseries.get(id);
	}

}
