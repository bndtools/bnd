package org.bndtools.templating.load;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bndtools.templating.Template;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.resource.CapReqBuilder;

public class ReposTemplateLoader {

	private static final String NS_TEMPLATE = "org.bndtools.template";

	private final List<Repository> repos;
	private final BundleLocator locator;


	public ReposTemplateLoader(List<Repository> repos, BundleLocator locator) {
		this.repos = repos;
		this.locator = locator;
	}
	
	public List<Template> findTemplates(String templateType) {
		String filterStr = String.format("(%s=%s)", NS_TEMPLATE, templateType);
		Requirement requirement = new CapReqBuilder(NS_TEMPLATE)
				.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filterStr)
				.buildSyntheticRequirement();
		List<Template> templates = new ArrayList<>();
		for (Repository repo : repos) {
			Map<Requirement, Collection<Capability>> providerMap = repo.findProviders(Collections.singleton(requirement));
			if (providerMap != null) {
				Collection<Capability> candidates = providerMap.get(requirement);
				if (candidates != null) {
					for (Capability cap : candidates) {
						templates.add(new CapabilityBasedTemplate(cap, locator));
					}
				}
			}
		}
		return templates;
	}
}
