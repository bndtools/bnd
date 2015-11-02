package org.bndtools.templating;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TemplateEngine {
	
	Collection<String> getTemplateParameterNames(ResourceMap inputs) throws Exception;

	ResourceMap generateOutputs(ResourceMap inputs, Map<String, List<Object>> parameters) throws Exception;
	
}
