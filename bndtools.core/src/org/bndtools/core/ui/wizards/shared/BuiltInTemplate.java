package org.bndtools.core.ui.wizards.shared;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.templating.repobased.StringTemplateEngine;
import org.bndtools.templating.util.ObjectClassDefinitionImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Used as a bare-bones template for scenarios where no repository or workspace is available to load real templates.
 */
public class BuiltInTemplate implements Template {

    private final Bundle bundle = FrameworkUtil.getBundle(BuiltInTemplate.class);

    private final String name;
    private final ResourceMap inputResources = new ResourceMap();
    private URI helpUri = null;

    public BuiltInTemplate(String name) {
        this.name = name;
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
        URI iconUri = getResourceURI("icons/template_empty.gif");
        return new ObjectClassDefinitionImpl(name, getShortDescription(), iconUri);
    }

    @Override
    public ResourceMap generateOutputs(Map<String,List<Object>> parameters) throws Exception {
        return new StringTemplateEngine().generateOutputs(inputResources, parameters);
    }

    public void addInputResource(String path, Resource resource) {
        inputResources.put(path, resource);
    }

    public void setHelpPath(String path) {
        this.helpUri = getResourceURI(path);
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

}
