package org.osgi.service.indexer.osgi;

import org.osgi.framework.*;
import org.osgi.service.indexer.*;
import org.osgi.service.indexer.impl.*;
import org.osgi.service.indexer.osgi.AnalyzerTracker.TrackingStruct;
import org.osgi.service.log.*;
import org.osgi.util.tracker.*;

class AnalyzerTracker extends ServiceTracker<ResourceAnalyzer,TrackingStruct> {

	private final RepoIndex indexer;
	private final LogService log;

	public AnalyzerTracker(BundleContext context, RepoIndex indexer, LogService log) {
		super(context, ResourceAnalyzer.class, null);
		this.indexer = indexer;
		this.log = log;
	}

	static class TrackingStruct {
		ResourceAnalyzer analyzer;
		Filter filter;
		boolean valid;
	}

	@Override
	public TrackingStruct addingService(ServiceReference<ResourceAnalyzer> reference) {
		TrackingStruct struct = new TrackingStruct();
		try {
			String filterStr = (String) reference.getProperty(ResourceAnalyzer.FILTER);
			Filter filter = (filterStr != null) ? FrameworkUtil.createFilter(filterStr) : null;

			ResourceAnalyzer analyzer = context.getService(reference);
			if (analyzer == null)
				return null;

			struct = new TrackingStruct();
			struct.analyzer = analyzer;
			struct.filter = filter;
			struct.valid = true;

			indexer.addAnalyzer(analyzer, filter);
		} catch (InvalidSyntaxException e) {
			struct.valid = false;
			log.log(reference, LogService.LOG_ERROR, "Ignoring ResourceAnalyzer due to invalid filter expression", e);
		}
		return struct;
	}

	@Override
	public void modifiedService(ServiceReference<ResourceAnalyzer> reference, TrackingStruct struct) {
		if (struct.valid) {
			indexer.removeAnalyzer(struct.analyzer, struct.filter);
		}

		try {
			String filterStr = (String) reference.getProperty(ResourceAnalyzer.FILTER);
			Filter filter = (filterStr != null) ? FrameworkUtil.createFilter(filterStr) : null;

			struct = new TrackingStruct();
			struct.filter = filter;
			struct.valid = true;

			indexer.addAnalyzer(struct.analyzer, filter);
		} catch (InvalidSyntaxException e) {
			struct.valid = false;
			log.log(reference, LogService.LOG_ERROR, "Ignoring ResourceAnalyzer due to invalid filter expression", e);
		}

	}

	@Override
	public void removedService(ServiceReference<ResourceAnalyzer> reference, TrackingStruct struct) {
		if (struct.valid)
			indexer.removeAnalyzer(struct.analyzer, struct.filter);
	}
}
