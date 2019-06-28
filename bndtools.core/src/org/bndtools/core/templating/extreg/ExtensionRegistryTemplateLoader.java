package org.bndtools.core.templating.extreg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.service.reporter.Reporter;
import bndtools.Plugin;

@Component(name = "org.bndtools.templating.extension", property = {
	"source=extension", Constants.SERVICE_DESCRIPTION + "=Load templates from the Eclipse Extension Registry"
})
public class ExtensionRegistryTemplateLoader implements TemplateLoader {

	private final Map<String, String>	typeToExtPoint	= new HashMap<>();

	private PromiseFactory				promiseFactory;
	private ExecutorService				localExecutor	= null;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	void setExecutorService(ExecutorService executor) {
		this.promiseFactory = new PromiseFactory(Objects.requireNonNull(executor));
	}

	@Activate
	void activate() {
		typeToExtPoint.put("project", "projectTemplates");
		typeToExtPoint.put("workspace", "workspaceTemplates");
		if (promiseFactory == null) {
			localExecutor = Executors.newCachedThreadPool();
			promiseFactory = new PromiseFactory(localExecutor);
		}
	}

	@Deactivate
	void dectivate() {
		if (localExecutor != null) {
			localExecutor.shutdown();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public Promise<List<Template>> findTemplates(String type, Reporter reporter) {
		List<Template> templates;
		String extPoint = typeToExtPoint.get(type);
		if (extPoint != null) {
			IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(Plugin.PLUGIN_ID, extPoint);
			if (elements == null)
				elements = new IConfigurationElement[0];
			templates = new ArrayList<>(elements.length);

			float total = elements.length;
			float worked = 0f;

			for (IConfigurationElement element : elements) {
				String elementName = element.getName();
				IContributor contributor = element.getContributor();
				try {
					Template extTemplate = (Template) element.createExecutableExtension("class");
					templates.add(extTemplate);
				} catch (CoreException e) {
					Plugin.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format(
							"Error loading template '%s' from bundle %s", elementName, contributor.getName()), e));
				} finally {
					worked += 1f;
					reporter.progress(worked / total, "Loading templates");
				}
			}
		} else {
			templates = Collections.emptyList();
		}
		return promiseFactory.resolved(templates);
	}
}
