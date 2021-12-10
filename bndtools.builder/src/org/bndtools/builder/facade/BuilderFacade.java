package org.bndtools.builder.facade;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.exceptions.Exceptions;

public class BuilderFacade extends IncrementalProjectBuilder {

	static org.slf4j.Logger		consoleLog	= org.slf4j.LoggerFactory.getLogger(BuilderFacade.class);
	static ILogger				uiLog		= Logger.getLogger(BuilderFacade.class);

	ServiceTracker<Object, ProjectBuilderDelegate>						tracker;
	String						id;
	IConfigurationElement		config;
	String						propertyName;
	Object						data;
	static final BundleContext	bc			= Optional.ofNullable(FrameworkUtil.getBundle(BuilderFacade.class))
		.map(Bundle::getBundleContext)
		.orElse(null);


	List<BiConsumer<ServiceReference<Object>, ProjectBuilderDelegate>>	onNewService	= new ArrayList<>();
	List<BiConsumer<ServiceReference<Object>, ProjectBuilderDelegate>>	onClosedService	= new ArrayList<>();

	public void onNewService(BiConsumer<ServiceReference<Object>, ProjectBuilderDelegate> callback) {
		onNewService.add(callback);
	}

	public void onClosedService(BiConsumer<ServiceReference<Object>, ProjectBuilderDelegate> callback) {
		onClosedService.add(callback);
	}

	public boolean isEmpty() {
		return tracker.isEmpty();
	}

	public int size() {
		return tracker.size();
	}

	public Optional<ProjectBuilderDelegate> getService() {
		return Optional.ofNullable(tracker.getService());
	}

	public ProjectBuilderDelegate getRequiredService() {
		consoleLog.debug("{} Attempting to get service {}", this, id);
		return getService().orElseThrow(() -> {
			final String className = ProjectBuilderDelegate.class.getCanonicalName();
			uiLog.logWarning(MessageFormat.format("Service {0} ({1}) not found.", id, className), null);
			consoleLog.warn("{} Service {} ({}) not found", this, id, className);
			return new RuntimeException("Service " + id + " (" + className + ") not found");
		});
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException {
		this.config = config;
		this.propertyName = propertyName;
		this.data = data;
		this.id = config.getAttribute("id");

		consoleLog.debug("{} Initializing facade, propName: \"{}\", data: \"{}\"", this, propertyName, data);

		if (data != null) {
			final String dataString = data.toString();
			this.id = dataString;
		}

		if (id == null) {
			id = "org.bndtools.builder.impl.BndtoolsBuilder";
		}
		try {
			initializeTracker(id);
		} catch (Exception e) {
			consoleLog.error("{} uncaught exception", this, e);
			throw Exceptions.duck(e);
		}
	}

	public void close() {
		consoleLog.debug("{} close()", this);
		tracker.close();
	}

	/**
	 * Invoked by the Eclipse UI. Initialization is deferred until
	 * {@link #setInitializationData} is called.
	 */
	public BuilderFacade() {}

	/**
	 * Constructor for programmatic instantiation.
	 *
	 * @param id
	 */
	public BuilderFacade(String id) {
		initializeTracker(id);
	}

	private void initializeTracker(String id) {
		consoleLog.debug("{} Initializing tracker", this);
		Filter filter = null;
		try {
			filter = bc.createFilter("(component.name=" + id + ")");
			consoleLog.debug("{} Tracking services with filter: {}", this, filter);
			tracker = new BuilderServiceTracker(this, bc, filter);
			tracker.open();
		} catch (InvalidSyntaxException e) {
			consoleLog.error("{} couldn't build filter for {}", this, filter, e);
			throw Exceptions.duck(e);
		}
	}

	@Override
	public String toString() {
		return "[" + id + ":" + System.identityHashCode(this) + "]";
	}

	@Override
	final protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
		throws CoreException {
		consoleLog.debug("{} calling build: {}, {}", this, kind, args);
		return getRequiredService().build(kind, args, monitor);
	}

	@Override
	final protected void clean(IProgressMonitor monitor) throws CoreException {
		consoleLog.debug("{} calling clean", this);
		getRequiredService().clean(monitor);
	}

	@Override
	final public ISchedulingRule getRule(int kind, Map<String, String> args) {
		consoleLog.debug("{} calling getRule: {}, {}", this, kind, args);
		return getRequiredService().getRule(kind, args);
	}

	public ISchedulingRule superGetRule(int kind, Map<String, String> args) {
		return super.getRule(kind, args);
	}

	public void superStartupOnInitialize() {
		super.startupOnInitialize();
	}
}
