package aQute.bnd.runtime.snapshot;

import static aQute.bnd.runtime.util.Util.error;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.runtime.api.SnapshotProvider;
import aQute.bnd.runtime.facade.ConfigurationFacade;
import aQute.bnd.runtime.facade.FrameworkFacade;
import aQute.bnd.runtime.facade.LogFacade;
import aQute.bnd.runtime.facade.ServiceComponentRuntimeFacade;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.Encoder;
import aQute.lib.json.JSONCodec;

@SuppressWarnings("deprecation")
public class Snapshot implements BundleActivator {

	private static final Encoder		JSON_CODEC			= new JSONCodec().enc();
	final AtomicBoolean					once				= new AtomicBoolean(false);
	final Thread						snapshotThread		= new Thread(this::snapshot, "snapshot");
	final Map<String, SnapshotProvider>	providers			= new LinkedHashMap<>();

	ServiceTracker<Object, Object>		tracker;
	BundleContext						context;
	Bundle								framework;
	LogFacade							logTracker;
	final static DateTimeFormatter		DATE_TIME_FORMAT	= DateTimeFormatter.ofPattern("YYYYMMddHHmmss");
	FrameworkFacade						frameworkFront;

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		this.framework = context.getBundle(0);

		add("framework", FrameworkFacade.class);
		add("scr", ServiceComponentRuntimeFacade.class);
		add("log", LogFacade.class);
		add("cnf", ConfigurationFacade.class);
		// add("coordinator", CoordinatorFacade.class);

		this.frameworkFront = new FrameworkFacade(this.context);
		Runtime.getRuntime()
			.addShutdownHook(snapshotThread);

		Filter filter = FrameworkUtil.createFilter("(snapshot=*)");
		tracker = new ServiceTracker<>(context, filter, null);
		tracker.open();

		this.context.addBundleListener(new SynchronousBundleListener() {

			@Override
			public synchronized void bundleChanged(BundleEvent event) {
				try {
					if (framework.getState() != Bundle.STOPPING || once.getAndSet(true) == true)
						return;
					Thread.sleep(1000);

					snapshot();
				} catch (InterruptedException e) {
					Exceptions.duck(e);
				}
			}
		});

		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(CommandProcessor.COMMAND_SCOPE, "bnd");
		properties.put(CommandProcessor.COMMAND_FUNCTION, "snapshot");
		this.context.registerService(Object.class, this, properties);
	}

	private <T extends SnapshotProvider> void add(String name, Class<T> class1) {
		try {
			Constructor<T> constructor = class1.getConstructor(BundleContext.class);
			T newInstance = constructor.newInstance(context);
			providers.put(name, newInstance);
		} catch (Throwable e) {
			error(name + ": not available", null);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		Runtime.getRuntime()
			.removeShutdownHook(snapshotThread);
		providers.values()
			.forEach(IO::close);
		tracker.close();
	}

	@Descriptor("Create a snapshot of the framework state")
	public File snapshot() {
		return snapshot(null);
	}

	@Descriptor("Create a snapshot of the framework state")
	public File snapshot(@Descriptor("Path to the snapshot file") String name) {
		try {

			Map<String, Object> top = new LinkedHashMap<>();

			doProviders(top);

			doExtensions(top);

			return flush(top, name);
		} catch (Exception e) {
			error("creating snapshot", e);
			return null;
		}
	}

	private void doProviders(Map<String, Object> top) {
		for (Map.Entry<String, SnapshotProvider> entry : providers.entrySet()) {
			try {
				Object snapshot = entry.getValue()
					.getSnapshot();
				top.put(entry.getKey(), snapshot);
			} catch (Throwable t) {
				error(entry.getKey(), t);
			}
		}
	}

	private File flush(Map<String, Object> top, String name) throws IOException, Exception {
		if (name == null) {
			name = context.getProperty("launchpad.name");
			if (name != null) {
				String className = context.getProperty("launchpad.className");
				if (className != null) {
					int x = className.lastIndexOf('.');
					className = className.substring(x + 1);
					name = className.toLowerCase() + "-" + name;
				}
				name += ".json";
			} else
				name = "snapshot-" + DATE_TIME_FORMAT.format(Instant.now()
					.atOffset(ZoneOffset.UTC)) + ".json";
		}
		File file = new File(name);

		if (!file.isAbsolute()) {
			String dirName = context.getProperty("snapshot.dir");
			if (dirName != null) {
				File dir = new File(dirName).getAbsoluteFile();
				dir.mkdirs();
				file = new File(dir, name);
			}
		}

		JSON_CODEC.indent("\t")
			.writeDefaults()
			.to(file)
			.put(top)
			.close();
		return file;
	}

	private void doExtensions(Map<String, Object> top) {
		ServiceReference<Object>[] refs = tracker.getServiceReferences();
		if (refs != null) {
			for (ServiceReference<Object> ref : refs) {
				String name = "" + ref.getProperty("snapshot");
				top.put(name, tracker.getService(ref));
			}
		}
	}

}
