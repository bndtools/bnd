package org.osgi.service.indexer.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.RepoIndex;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

class AnalyzerTracker extends ServiceTracker {

	private final RepoIndex indexer;
	private final LogService log;

	public AnalyzerTracker(BundleContext context, RepoIndex indexer, LogService log) {
		super(context, ResourceAnalyzer.class.getName(), null);
		this.indexer = indexer;
		this.log = log;
	}

	private static class TrackingStruct {
		ResourceAnalyzer analyzer;
		Filter filter;
		boolean valid;
	}

	@Override
	public Object addingService(@SuppressWarnings("rawtypes") ServiceReference reference) {
		TrackingStruct struct = new TrackingStruct();
		try {
			String filterStr = (String) reference.getProperty(ResourceAnalyzer.FILTER);
			Filter filter = (filterStr != null) ? FrameworkUtil.createFilter(filterStr) : null;

			@SuppressWarnings("unchecked")
			ResourceAnalyzer analyzer = (ResourceAnalyzer) context.getService(reference);
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
	public void modifiedService(@SuppressWarnings("rawtypes") ServiceReference reference, Object service) {
		TrackingStruct struct = (TrackingStruct) service;
		
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
	public void removedService(@SuppressWarnings("rawtypes") ServiceReference reference, Object service) {
		TrackingStruct struct = (TrackingStruct) service;
		if (struct.valid)
			indexer.removeAnalyzer(struct.analyzer, struct.filter);
	}
}
