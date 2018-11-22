package aQute.bnd.junit;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

@Deprecated // see biz.aQute.bnd.remote.junit
public class ConsoleLogger implements LogReaderService {
	public List<LogEntry>							entries			= new ArrayList<>();
	public List<Facade>								facades			= new CopyOnWriteArrayList<>();
	public List<LogListener>						logListeners	= new CopyOnWriteArrayList<>();
	public static AtomicLong						timer			= new AtomicLong(0);
	public PrintStream								logToConsole	= System.out;
	private ServiceRegistration<?>					registerService;
	private ServiceRegistration<LogReaderService>	lrRegistration;
	private AtomicBoolean							closed			= new AtomicBoolean();

	public static class LogEntryImpl implements LogEntry {
		public final Bundle					bundle;
		public final ServiceReference<?>	reference;
		public final Throwable				exception;
		public final String					message;
		public final int					level;
		public final long					time	= timer.incrementAndGet();

		public LogEntryImpl(Bundle b, ServiceReference<?> reference, Throwable e, int level, String message) {
			bundle = b;
			this.reference = reference;
			exception = e;
			this.message = message;
			this.level = level;
		}

		@Override
		public Bundle getBundle() {
			return bundle;
		}

		@Override
		public ServiceReference getServiceReference() {
			return reference;
		}

		@Override
		public int getLevel() {
			return level;
		}

		@Override
		public String getMessage() {
			return message;
		}

		@Override
		public Throwable getException() {
			return exception;
		}

		@Override
		public long getTime() {
			return time;
		}

	}

	public class Facade implements LogService {

		private Bundle bundle;

		public Facade(Bundle bundle, ServiceRegistration<LogService> registration) {
			this.bundle = bundle;
		}

		@Override
		public void log(int level, String message) {
			log(null, level, message, null);
		}

		@Override
		public void log(int level, String message, Throwable exception) {
			log(null, level, message, exception);
		}

		@Override
		public void log(ServiceReference sr, int level, String message) {
			log(sr, level, message, null);

		}

		@Override
		public void log(ServiceReference sr, int level, String message, Throwable exception) {
			ConsoleLogger.this.log(bundle, sr, level, message, exception);
		}

		public void close() {
			// TODO Auto-generated method stub

		}

	}

	public ConsoleLogger(BundleContext context) {
		ServiceFactory<LogService> serviceFactory = new ServiceFactory<LogService>() {

			@Override
			public LogService getService(Bundle bundle, ServiceRegistration<LogService> registration) {
				Facade facade = new Facade(bundle, registration);
				facades.add(facade);
				return facade;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<LogService> registration, LogService service) {
				Facade facade = (Facade) service;
				facades.remove(facade);
				facade.close();
			}
		};

		registerService = context.registerService(new String[] {
			LogService.class.getName()
		}, serviceFactory, null);

		lrRegistration = context.registerService(LogReaderService.class, this, null);
	}

	void log(Bundle bundle, ServiceReference<?> sr, int level, String message, Throwable exception) {
		LogEntry le = new LogEntryImpl(bundle, sr, exception, level, message);
		synchronized (this) {
			entries.add(le);
			if (logToConsole != null) {
				logToConsole.format("%8s: %s %s %s\n", le.getTime(), le.getMessage(), le.getServiceReference(),
					le.getException());
			}
		}
	}

	@Override
	public void addLogListener(LogListener listener) {
		logListeners.add(listener);
	}

	@Override
	public void removeLogListener(LogListener listener) {
		logListeners.remove(listener);
	}

	@Override
	public Enumeration getLog() {
		return Collections.enumeration(entries);
	}

	public void close() {
		if (closed.getAndSet(true) == false) {
			registerService.unregister();
			lrRegistration.unregister();
		}
	}
}
