package aQute.bld.main;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.framework.*;

import aQute.bnd.build.*;
import aQute.lib.osgi.*;
import aQute.libg.classloaders.*;
import de.kalpatec.pojosr.framework.*;
import de.kalpatec.pojosr.framework.launch.*;

public class Run extends Processor implements ServiceListener {
	File					base;
	String[]				args;
	File					build;
	PojoServiceRegistry		sr;
	BlockingQueue<Runnable>	mainThreads	= new LinkedBlockingQueue<Runnable>();

	public void run(String args[], File cwd, File build, URLClassLoaderWrapper wrapper)
			throws Exception {
		Workspace workspace = Workspace.getWorkspace(build.getParentFile());
		Project cnf = workspace.getProject(build.getName());

		Collection<Container> bundles = cnf.getRunbundles();
		List<File> files = new ArrayList<File>();
		for (Container container : bundles) {
			container.contributeFiles(files, this);
		}

		for (File f : files) {
			wrapper.addURL(f.toURI().toURL());
		}

		Properties p = workspace.getFlattenedProperties();
		List<BundleDescriptor> descriptors = new ClasspathScanner().scanForBundles();
		p.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, descriptors);
		sr = new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(p);

		Hashtable<String, Object> argprops = new Hashtable<String, Object>();
		argprops.put("launcher.arguments", args);
		argprops.put("launcher.ready", "true");
		sr.getBundleContext().registerService(Main.class.getName(), this, argprops);

		sr.addServiceListener(this, "(&(objectclass=java.lang.Runnable)(main.thread=true))");
		while (!Thread.currentThread().isInterrupted()) {
			Runnable take = mainThreads.take();
			take.run();
		}
	}

	@Override public void serviceChanged(ServiceEvent event) {
		if (event.getType() == ServiceEvent.REGISTERED) {
			ServiceReference ref = event.getServiceReference();
			Runnable r = (Runnable) sr.getBundleContext().getService(ref);
			if (r != null)
				mainThreads.offer((Runnable) r);
		}
	}
}
