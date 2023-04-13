package org.bndtools.core.ui.wizards.service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.promise.Promise;

import aQute.bnd.osgi.Processor;
import aQute.libg.tuple.Pair;
import bndtools.Plugin;

public class ServiceTemplateLoaderJob implements IRunnableWithProgress {

	private BundleContext	bundleContext	= FrameworkUtil.getBundle(ServiceTemplateLoaderJob.class)
		.getBundleContext();

	private final String[] templateTypes;

	public ServiceTemplateLoaderJob(String[] templateTypes) {
		this.templateTypes = templateTypes;
	}

	public ServiceTemplateLoaderJob() {
		this.templateTypes = new String[] {
			ServiceTemplateConstants.TEMPLATE_SERVICE_API_TYPE, ServiceTemplateConstants.TEMPLATE_SERVICE_IMPL_TYPE,
			ServiceTemplateConstants.TEMPLATE_SERVICE_CONSUMER_TYPE
		};
	}

	protected Set<Template> getTemplates(String templateType, IProgressMonitor progress)
		throws InvocationTargetException {
		SubMonitor monitor = SubMonitor.convert(progress);

		try {
			final Set<Template> templates = new LinkedHashSet<>();
			// Fire all the template loaders and get their promises
			List<ServiceReference<TemplateLoader>> templateLoaderSvcRefs = new ArrayList<>(
				bundleContext.getServiceReferences(TemplateLoader.class, null));
			monitor.beginTask("Loading " + templateType + " templates...", templateLoaderSvcRefs.size());
			Collections.sort(templateLoaderSvcRefs);
			List<Pair<String, Promise<? extends Collection<Template>>>> promises = new LinkedList<>();
			for (ServiceReference<TemplateLoader> templateLoaderSvcRef : templateLoaderSvcRefs) {
				String label = (String) templateLoaderSvcRef.getProperty(Constants.SERVICE_DESCRIPTION);
				if (label == null)
					label = (String) templateLoaderSvcRef.getProperty(ComponentConstants.COMPONENT_NAME);
				if (label == null)
					label = String.format(
						"Template Loader service ID " + templateLoaderSvcRef.getProperty(Constants.SERVICE_ID));

				TemplateLoader templateLoader = bundleContext.getService(templateLoaderSvcRef);

				Promise<? extends Collection<Template>> promise = templateLoader.findTemplates(templateType,
					new Processor());
				promise.onResolve(() -> bundleContext.ungetService(templateLoaderSvcRef));
				promises.add(new Pair<String, Promise<? extends Collection<Template>>>(label, promise));
			}

			// Force the promises in sequence
			for (Pair<String, Promise<? extends Collection<Template>>> namedPromise : promises) {
				String name = namedPromise.getFirst();
				SubMonitor childMonitor = monitor.split(1, SubMonitor.SUPPRESS_NONE);
				childMonitor.beginTask(name, 1);
				try {
					Throwable failure = namedPromise.getSecond()
						.getFailure();
					if (failure != null)
						Plugin.getDefault()
							.getLog()
							.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
								"Failed to load from template loader: " + name, failure));
					else {
						Collection<Template> loadedTemplates = namedPromise.getSecond()
							.getValue();
						templates.addAll(loadedTemplates);
					}
				} catch (InterruptedException e) {
					Plugin.getDefault()
						.getLog()
						.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0,
							"Interrupted while loading from template loader: " + name, e));
				}
			}
			return templates;
		} catch (InvalidSyntaxException ex) {
			throw new InvocationTargetException(ex);
		} finally {
			if (progress != null)
				progress.done();
		}
	}

	protected Map<String, Set<Template>> templates = new HashMap<String, Set<Template>>();

	public Map<String,Set<Template>> getTemplates() {
		return templates;
	}

	@Override
	public void run(IProgressMonitor progress) throws InvocationTargetException {
		SubMonitor monitor = SubMonitor.convert(progress);
		try {
			int templateTypeCount = this.templates.size();
			monitor.beginTask("", templateTypeCount);
			for(String templateType: this.templateTypes) {
				this.templates.put(templateType, getTemplates(templateType, monitor));
				monitor.split(1, SubMonitor.SUPPRESS_NONE);
			}
		} finally {
			if (progress != null)
				progress.done();
		}
	}
}
