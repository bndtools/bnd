package org.bndtools.core.ui.wizards.shared;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

/**
 * Used as a bare-bones template for scenarios where no repository or workspace is available to load real templates.
 */
public class BuiltInTemplate implements Template {

    private final Bundle bundle = FrameworkUtil.getBundle(BuiltInTemplate.class);

    private final String name;
    private final ResourceMap resources = new ResourceMap();
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

    public void addResource(String path, Resource resource) {
        resources.put(path, resource);
    }

    @Override
    public ResourceMap getInputSources() throws IOException {
        return resources;
    }

    @Override
    public URI getIcon() {
        return getResourceURI("icons/template_empty.gif");
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
