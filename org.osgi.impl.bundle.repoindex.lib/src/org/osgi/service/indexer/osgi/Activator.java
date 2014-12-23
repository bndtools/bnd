package org.osgi.service.indexer.osgi;

import org.osgi.framework.*;
import org.osgi.service.indexer.*;
import org.osgi.service.indexer.impl.*;

public class Activator implements BundleActivator {

	private LogTracker logTracker;
	private AnalyzerTracker analyzerTracker;

	private ServiceRegistration<ResourceIndexer>	registration;

	public void start(BundleContext context) throws Exception {
		logTracker = new LogTracker(context);
		logTracker.open();

		RepoIndex indexer = new RepoIndex(logTracker);

		analyzerTracker = new AnalyzerTracker(context, indexer, logTracker);
		analyzerTracker.open();

		registration = context.registerService(ResourceIndexer.class, indexer, null);
	}

	public void stop(BundleContext context) throws Exception {
		registration.unregister();
		analyzerTracker.close();
		logTracker.close();
	}

}
