package org.bndtools.templating;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

public interface TemplateEngine {

    Collection<String> getTemplateParameterNames(ResourceMap inputs) throws Exception;

    Collection<String> getTemplateParameterNames(ResourceMap inputs, IProgressMonitor monitor) throws Exception;

    ResourceMap generateOutputs(ResourceMap inputs, Map<String,List<Object>> parameters) throws Exception;

    ResourceMap generateOutputs(ResourceMap inputs, Map<String,List<Object>> parameters, IProgressMonitor monitor) throws Exception;

}
