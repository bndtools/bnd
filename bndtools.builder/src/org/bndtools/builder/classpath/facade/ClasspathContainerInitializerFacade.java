package org.bndtools.builder.classpath.facade;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.exceptions.Exceptions;

public class ClasspathContainerInitializerFacade extends ClasspathContainerInitializer implements IExecutableExtension {
	static org.slf4j.Logger														consoleLog		= org.slf4j.LoggerFactory
		.getLogger(ClasspathContainerInitializerFacade.class);
	static ILogger																uiLog			= Logger
		.getLogger(ClasspathContainerInitializerFacade.class);

	ServiceTracker<Object, ClasspathContainerInitializer>						tracker;
	String																		id;
	IConfigurationElement														config;
	String																		propertyName;
	Object																		data;
	static final BundleContext													bc				= Optional
		.ofNullable(FrameworkUtil.getBundle(ClasspathContainerInitializerFacade.class))
		.map(Bundle::getBundleContext)
		.orElse(null);

	List<BiConsumer<ServiceReference<Object>, ClasspathContainerInitializer>>	onNewService	= new ArrayList<>();
	List<BiConsumer<ServiceReference<Object>, ClasspathContainerInitializer>>	onClosedService	= new ArrayList<>();

	public void onNewService(BiConsumer<ServiceReference<Object>, ClasspathContainerInitializer> callback) {
		onNewService.add(callback);
	}

	public void onClosedService(BiConsumer<ServiceReference<Object>, ClasspathContainerInitializer> callback) {
		onClosedService.add(callback);
	}

	public boolean isEmpty() {
		return tracker.isEmpty();
	}

	public int size() {
		return tracker.size();
	}

	public Optional<ClasspathContainerInitializer> getService() {
		try {
			return Optional.ofNullable(tracker.waitForService(10000));
		} catch (InterruptedException e) {
			return Optional.empty();
		}
	}

	public ClasspathContainerInitializer getRequiredService() {
		consoleLog.debug("{} Attempting to get service {}", this, id);
		return getService().orElseThrow(() -> {
			final String className = ClasspathContainerInitializer.class.getCanonicalName();
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

		// if (id == null) {
			id = "org.bndtools.builder.classpath.BndContainerInitializer";
		// }
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
	public ClasspathContainerInitializerFacade() {}

	/**
	 * Constructor for programmatic instantiation.
	 *
	 * @param id
	 */
	public ClasspathContainerInitializerFacade(String id) {
		initializeTracker(id);
	}

	private void initializeTracker(String id) {
		consoleLog.debug("{} Initializing tracker", this);
		Filter filter = null;
		try {
			filter = bc.createFilter("(component.name=" + id + ")");
			consoleLog.debug("{} Tracking services with filter: {}", this, filter);
			tracker = new ClasspathContainerInitializerServiceTracker(this, bc, filter);
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
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		getRequiredService().initialize(containerPath, project);
	}

	@Override
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		return getRequiredService().canUpdateClasspathContainer(containerPath, project);
	}

	@Override
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project,
		IClasspathContainer containerSuggestion) throws CoreException {
		getRequiredService().requestClasspathContainerUpdate(containerPath, project, containerSuggestion);
	}

	@Override
	public String getDescription(IPath containerPath, IJavaProject project) {
		return getRequiredService().getDescription(containerPath, project);
	}

	@Override
	public IClasspathContainer getFailureContainer(IPath containerPath, IJavaProject project) {
		return getRequiredService().getFailureContainer(containerPath, project);
	}

	@Override
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		return getRequiredService().getComparisonID(containerPath, project);
	}

	@Override
	public IStatus getAccessRulesStatus(IPath containerPath, IJavaProject project) {
		return getRequiredService().getAccessRulesStatus(containerPath, project);
	}

	@Override
	public IStatus getAttributeStatus(IPath containerPath, IJavaProject project, String attributeKey) {
		return getRequiredService().getAttributeStatus(containerPath, project, attributeKey);
	}

	@Override
	public IStatus getSourceAttachmentStatus(IPath containerPath, IJavaProject project) {
		return getRequiredService().getSourceAttachmentStatus(containerPath, project);
	}
}
