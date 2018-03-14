package org.bndtools.templating;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Version;
import org.osgi.service.metatype.ObjectClassDefinition;

@ProviderType
public interface Template extends Closeable {

    /**
     * The name of this template.
     */
    String getName();

    /**
     * A short description of the template that may be shown in a summary view of all templates. This should NOT be
     * expensive to fetch.
     */
    String getShortDescription();

    /**
     * The category of the template. May be null, in which case the template will be considered uncategorised.
     */
    String getCategory();

    /**
     * The ranking of this template in relation to other templates of the same category. If you don't care, just return
     * zero.
     */
    int getRanking();

    /**
     * The version of this template.
     */
    Version getVersion();

    /**
     * Get the definition of required and optional parameters.
     */
    ObjectClassDefinition getMetadata() throws Exception;

    /**
     * Get the definition of required and optional parameters.
     */
    ObjectClassDefinition getMetadata(IProgressMonitor monitor) throws Exception;

    /**
     * Generate the output resources.
     */
    ResourceMap generateOutputs(Map<String, List<Object>> parameters) throws Exception;

    /**
     * Generate the output resources.
     */
    ResourceMap generateOutputs(Map<String, List<Object>> parameters, IProgressMonitor monitor) throws Exception;

    /**
     * An icon representing the template, or null if not available.
     */
    URI getIcon();

    /**
     * A URL to a help document for the template. May be null.
     */
    URI getHelpContent();

}
