package org.bndtools.core.ui.wizards.shared;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateEngine;
import org.bndtools.templating.util.ObjectClassDefinitionImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Used as a bare-bones template for scenarios where no repository or workspace is available to load real templates.
 */
public class BuiltInTemplate implements Template {

    private final Bundle bundle = FrameworkUtil.getBundle(BuiltInTemplate.class);

    private final ResourceMap inputResources = new ResourceMap();
    private final String name;
    private final String engineName;

    private URI helpUri = null;

    public BuiltInTemplate(String name, String templateEngine) {
        this.name = name;
        this.engineName = templateEngine;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortDescription() {
        return "built-in";
    }

    @Override
    public String getCategory() {
        return "mmm/Bndtools";
    }

    @Override
    public int getRanking() {
        return -1000;
    }

    @Override
    public Version getVersion() {
        return Version.emptyVersion;
    }

    @Override
    public ObjectClassDefinition getMetadata() throws Exception {
        return new ObjectClassDefinitionImpl(name, getShortDescription(), null);
    }

    @Override
    public ResourceMap generateOutputs(Map<String,List<Object>> parameters) throws Exception {
        BundleContext context = bundle.getBundleContext();
        Collection<ServiceReference<TemplateEngine>> svcRefs = context.getServiceReferences(TemplateEngine.class, String.format("(name=%s)", engineName));
        if (svcRefs == null || svcRefs.isEmpty())
            throw new Exception(String.format("Unable to generate built-in template '%s': no Template Engine available matching '%s'", name, engineName));
        ServiceReference<TemplateEngine> svcRef = svcRefs.iterator().next();
        TemplateEngine engine = context.getService(svcRef);
        if (engine == null)
            throw new Exception(String.format("Unable to generate built-in template '%s': no Template Engine available matching '%s'", name, engineName));
        try {
            return engine.generateOutputs(inputResources, parameters);
        } finally {
            context.ungetService(svcRef);
        }
    }

    public void addInputResource(String path, Resource resource) {
        inputResources.put(path, resource);
    }

    public void setHelpPath(String path) {
        this.helpUri = getResourceURI(path);
    }

    @Override
    public URI getIcon() {
        return getResourceURI("icons/template_empty.gif");
    }

    @Override
    public URI getHelpContent() {
        return helpUri;
    }

    private URI getResourceURI(String path) {
        URI uri = null;
        URL url = bundle.getResource(path);
        if (url != null) {
            try {
                uri = url.toURI();
            } catch (URISyntaxException e) {
                // ignore, we'll just see a missing icon
            }
        }
        return uri;
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }

}
