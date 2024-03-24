package biz.aQute.bnd.facade.provider;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.aQute.bnd.facade.api.Binder;
import biz.aQute.bnd.facade.api.Delegate;
import biz.aQute.bnd.facade.api.FacadeManager;
import biz.aQute.bnd.facade.api.Instance;

@Component
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FacadeManagerProvider implements FacadeManager {
	final static Logger						logger		= LoggerFactory.getLogger("facade.provider");

	final Map<String, Controller>			controllers	= new ConcurrentHashMap<>();
	final ScheduledExecutorService			executor;
	final ServiceTracker<Object, Delegate>	tracker;

	volatile boolean						closed;

	class Controller {
		final Map<Binder, Instance>	binders	= new IdentityHashMap<>();
		final String				id;

		boolean						closed;
		Delegate					delegate;

		Controller(String id) {
			logger.debug("new id {}", id);
			this.id = id;
		}

		synchronized AutoCloseable add(Binder binder) {
			if (closed)
				return () -> {
				};

			assert !binders.containsKey(binder) : "binders may never register twice";

			Instance instance = bind(delegate, binder);
			Instance old = binders.put(binder, instance);

			assert old == null : "binders may never register twice";

			return () -> {
				remove(binder);
			};
		}

		synchronized void remove(Binder binder) {
			Instance instance = binders.remove(binder);
			unbind(binder,instance);
			if (closed)
				return;
			assert (instance != null) == (delegate != null) : "the state must follow the delegate";
		}


		synchronized void setDelegate(OSGiPrototypeDelegate delegate) {
			if (closed)
				return;

			if (this.delegate != null) {
				throw new IllegalArgumentException("there are multiple delegates with the same id " + id + ": a="
						+ this.delegate + ", b=" + delegate);
			}

			this.delegate = delegate;

			for (Entry<Binder, Instance> next : binders.entrySet()) {
				Binder binder = next.getKey();
				Instance instance = next.getValue();
				assert instance == null : "should be bracketed";
				instance = bind(delegate,binder);
				next.setValue(instance);
			}
		}

		synchronized void removeDelegate(Delegate delegate) {
			if (delegate == this.delegate) {
				this.delegate = null;

				for (Entry<Binder, Instance> next : binders.entrySet()) {
					Binder binder = next.getKey();
					Instance instance = next.getValue();
					unbind(binder, instance);
					next.setValue(null);
				}
			}
		}

		synchronized boolean close() {
			if (closed)
				return false;
			closed = true;
			delegate = null;
			return true;
		}

		// locked
		private Instance bind(Delegate delegate, Binder binder) {
			if (delegate == null)
				return null;

			assert binder.getId().equals(delegate.getId());

			Instance instance = delegate.create(binder);
			binder.setState(null);
			binder.accept(instance.get());
			return instance;
		}
		// locked
		private void unbind(Binder binder, Instance instance) {
			if (instance != null) {
				binder.accept(null);
				binder.setState(instance.getState());
				instance.close();
			}
		}


		@Override
		public String toString() {
			return "Controller [id=" + id + ", closed=" + closed + ", delegate=" + delegate + "]";
		}
	}

	@Activate
	public FacadeManagerProvider(BundleContext context) throws InvalidSyntaxException {
		Filter filter = context.createFilter("(" + FACADE_ID + "=*)");
		this.tracker = new ServiceTracker<>(context, filter, null) {
			@Override
			public Delegate addingService(ServiceReference<Object> reference) {
				String id = (String) reference.getProperty(FACADE_ID);
				String objectClass[] = (String[]) reference.getProperty(Constants.OBJECTCLASS);
				if (in(Delegate.class.getName(), objectClass)) {
					Delegate delegate = (Delegate) context.getService(reference);
					return delegate;
				} else {
					ServiceObjects so = context.getServiceObjects(reference);
					OSGiPrototypeDelegate delegate = new OSGiPrototypeDelegate(id, so);
					getController(delegate.id).setDelegate(delegate);
					return delegate;
				}
			}

			public void removedService(org.osgi.framework.ServiceReference<Object> reference, Delegate delegate) {
				getController(delegate.getId()).removeDelegate(delegate);
			}
		};
		this.executor = Executors.newScheduledThreadPool(1);
		this.executor.scheduleAtFixedRate(Binder::purge, 1, 1, TimeUnit.MINUTES);
		tracker.open();
		Binder.setFacadeManager(this);
	}

	@Deactivate
	void deactivate() {
		Binder.setFacadeManager(null);
		tracker.close();
		this.closed = true;
		this.executor.shutdownNow();
		this.controllers.values().removeIf(Controller::close);
	}

	@Override
	public AutoCloseable register(Binder<?> binder) {
		Controller controller = getController(binder.getId());
		controller.add(binder);
		if (closed) {
			controller.close();
			controller.remove(binder);
		}

		return () -> {
			if (closed)
				return;
			controller.remove(binder);
		};
	}

	private Controller getController(String id) {
		assert !closed : "DS responsible";
		return controllers.computeIfAbsent(id, Controller::new);
	}

	private boolean in(String string, String[] domain) {
		return Stream.of(domain).filter(entry -> entry.equals(string)).findAny().isPresent();
	}

}
