package org.bndtools.templating;

import java.util.List;
import java.util.Map;

public interface TemplateEngine {

	ResourceMap generateOutputs(ResourceMap inputs, Map<String, List<Object>> parameters) throws Exception;
	
}
