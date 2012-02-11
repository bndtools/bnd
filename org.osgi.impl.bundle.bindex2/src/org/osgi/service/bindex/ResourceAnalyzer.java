package org.osgi.service.bindex;

import java.util.List;

public interface ResourceAnalyzer {
	
	static final String FILTER = "filter";

	void analyzeResource(Resource resource,
			List<? super Capability> capabilities,
			List<? super Requirement> requirements) throws Exception;

}
