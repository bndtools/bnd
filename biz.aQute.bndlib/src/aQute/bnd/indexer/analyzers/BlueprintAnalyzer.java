package aQute.bnd.indexer.analyzers;

import java.util.*;

import aQute.bnd.indexer.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.resource.*;

public class BlueprintAnalyzer implements aQute.bnd.indexer.ResourceAnalyzer {

	private static final String BUNDLE_BLUEPRINT_HEADER = "Bundle-Blueprint";


	public void analyzeResource(Jar resource, ResourceBuilder rb) throws Exception {
		boolean blueprintEnabled = false;

		String header = resource.getManifest().getMainAttributes().getValue(BUNDLE_BLUEPRINT_HEADER);
		if (header != null) {
			blueprintEnabled = true;
		} else {
			Map<String,Resource> children = resource.getDirectories().get("OSGI-INF/blueprint/");
			if (children != null) {
				for (String child : children.keySet()) {
					if (child.toLowerCase().endsWith(".xml")) {
						blueprintEnabled = true;
						break;
					}
				}
			}
		}

		if (blueprintEnabled) {
			CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_EXTENDER);
			String filter = String.format("(&(%s=%s)(version>=1.0.0)(!(version>=2.0.0)))", Namespaces.NS_EXTENDER, Namespaces.EXTENDER_BLUEPRINT);
			builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter).addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
			rb.addRequirement(builder);
			
		}
	}

}
