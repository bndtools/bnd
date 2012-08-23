package org.osgi.service.indexer.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.RepoIndex;

public class Activator implements BundleActivator {

	private LogTracker logTracker;
	private AnalyzerTracker analyzerTracker;
	
	private ServiceRegistration registration;

	public void start(BundleContext context) throws Exception {
		logTracker = new LogTracker(context);
		logTracker.open();

		RepoIndex indexer = new RepoIndex(logTracker);
		
		analyzerTracker = new AnalyzerTracker(context, indexer, logTracker);
		analyzerTracker.open();

		registration = context.registerService(ResourceIndexer.class.getName(), indexer, null);
	}

	public void stop(BundleContext context) throws Exception {
		registration.unregister();
		analyzerTracker.close();
		logTracker.close();
	}

}
