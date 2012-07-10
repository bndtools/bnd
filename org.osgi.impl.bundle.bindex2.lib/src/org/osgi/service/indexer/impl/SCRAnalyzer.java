package org.osgi.service.indexer.impl;

import java.util.List;
import java.util.StringTokenizer;

import org.osgi.framework.Version;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.types.VersionKey;
import org.osgi.service.indexer.impl.types.VersionRange;
import org.osgi.service.log.LogService;

public class SCRAnalyzer implements ResourceAnalyzer {
	private LogService log;

	public SCRAnalyzer(LogService log) {
		this.log = log;
	}

	public void analyzeResource(Resource resource, List<Capability> caps, List<Requirement> reqs) throws Exception {
		String header = resource.getManifest().getMainAttributes().getValue(ComponentConstants.SERVICE_COMPONENT);
		if (header == null)
			return;
		
		Requirement requirement = createRequirement(new VersionRange("[1.0,2.0)"));
		reqs.add(requirement);
	}

	private static Requirement createRequirement(VersionRange range) {
		Builder builder = new Builder().setNamespace(Namespaces.NS_EXTENDER);
		
		StringBuilder filter = new StringBuilder();
		filter.append('(').append(Namespaces.NS_EXTENDER).append('=').append(Namespaces.EXTENDER_SCR).append(')');
		
		filter.insert(0,  "(&");
		Util.addVersionFilter(filter, range, VersionKey.PackageVersion);
		filter.append(')');
		
		builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());
		Requirement requirement = builder.buildRequirement();
		return requirement;
	}
	
}
