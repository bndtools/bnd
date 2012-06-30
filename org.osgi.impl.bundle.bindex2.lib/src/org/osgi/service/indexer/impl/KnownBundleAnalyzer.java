package org.osgi.service.indexer.impl;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.osgi.framework.Version;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.types.SymbolicName;
import org.osgi.service.indexer.impl.types.VersionRange;
import org.osgi.service.indexer.impl.util.OSGiHeader;

public class KnownBundleAnalyzer implements ResourceAnalyzer {
	
	private final Properties props;

	public KnownBundleAnalyzer(Properties props) {
		this.props = props;
	}

	public void analyzeResource(Resource resource, List<Capability> caps, List<Requirement> reqs) throws Exception {
		SymbolicName resourceName = Util.getSymbolicName(resource);
		
		for (Enumeration<?> names = props.propertyNames(); names.hasMoreElements(); ) {
			String name = (String) names.nextElement();
			
			String[] bundleRef = name.split(";");
			String bsn = bundleRef[0];
			
			if (resourceName.getName().equals(bsn)) {
				VersionRange versionRange = null;
				if (bundleRef.length > 1)
					versionRange = new VersionRange(bundleRef[1]);
				
				Version version = Util.getVersion(resource);
				if (versionRange == null || versionRange.match(version)) {
					processClause(Util.readProcessedProperty(props, name), caps, reqs);
					return;
				}
			}
		}
	}
	
	private void processClause(String clauseStr, List<Capability> caps, List<Requirement> reqs) {
		Map<String, Map<String, String>> header = OSGiHeader.parseHeader(clauseStr);
		
		for (Entry<String, Map<String,String>> entry : header.entrySet()) {
			String type = OSGiHeader.removeDuplicateMarker(entry.getKey());
			Map<String, String> attribs = entry.getValue();
			
			String namespace = attribs.remove("namespace:");
			if (namespace != null) {
				Builder builder = new Builder().setNamespace(namespace);
				Util.copyAttribsToBuilder(builder, attribs);
				if ("capability".equalsIgnoreCase(type) || "cap".equalsIgnoreCase(type))
					caps.add(builder.buildCapability());
				else if ("requirement".equalsIgnoreCase(type) || "req".equalsIgnoreCase(type))
					reqs.add(builder.buildRequirement());
			}
		}
	}

}
