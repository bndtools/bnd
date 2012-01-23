package org.osgi.service.bindex;

import java.util.List;

public interface ResourceAnalyzer {

	void analyseResource(Resource resource,
			List<? super Capability> capabilities,
			List<? super Requirement> requirements) throws Exception;

}
