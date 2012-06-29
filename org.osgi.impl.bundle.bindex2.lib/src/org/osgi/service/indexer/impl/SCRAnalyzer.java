package org.osgi.service.indexer.impl;

import java.util.List;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.types.VersionKey;
import org.osgi.service.indexer.impl.types.VersionRange;

public class SCRAnalyzer implements ResourceAnalyzer {

	public void analyzeResource(Resource resource, List<Capability> caps, List<Requirement> reqs) throws Exception {
		String header = resource.getManifest().getMainAttributes().getValue(ComponentConstants.SERVICE_COMPONENT);
		if (header == null)
			return;
		
		Builder builder = new Builder().setNamespace(Namespaces.NS_EXTENDER);
		
		StringBuilder filter = new StringBuilder();
		filter.append('(').append(Namespaces.NS_EXTENDER).append('=').append(Namespaces.EXTENDER_SCR).append(')');
		
		filter.insert(0,  "(&");
		Util.addVersionFilter(filter, new VersionRange("[1.0,2.0)"), VersionKey.PackageVersion);
		filter.append(')');
		
		builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());
		reqs.add(builder.buildRequirement());
	}

}
