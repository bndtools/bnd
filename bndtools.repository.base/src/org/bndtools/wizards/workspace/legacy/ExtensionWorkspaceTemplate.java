package org.bndtools.wizards.workspace.legacy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bndtools.templating.FolderResource;
import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.templating.URLResource;
import org.bndtools.templating.util.ObjectClassDefinitionImpl;
import org.bndtools.utils.osgi.BundleUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.service.metatype.ObjectClassDefinition;

public class ExtensionWorkspaceTemplate implements Template, IExecutableExtension {

    private static final Bundle bundle = FrameworkUtil.getBundle(ExtensionWorkspaceTemplate.class);

    private Bundle contributor;
    private String name;
    private String category;
    private String description;
    private URI iconUri;
    private URI helpUri;
    private String templatePath;

    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        String bsn = config.getContributor().getName();
        contributor = BundleUtils.findBundle(bundle.getBundleContext(), bsn, null);

        name = config.getAttribute("name");
        category = config.getAttribute("category");
        description = config.getAttribute("description");
        templatePath = config.getAttribute("path");

        try {
            String iconPath = config.getAttribute("icon");
            if (iconPath != null && contributor != null) {
                URL iconUrl = contributor.getEntry(iconPath);
                iconUri = iconUrl != null ? iconUrl.toURI() : null;
            }

            String helpPath = config.getAttribute("help");
            if (helpPath != null && contributor != null) {
                URL helpUrl = contributor.getEntry(helpPath);
                helpUri = helpUrl != null ? helpUrl.toURI() : null;
            }
        } catch (InvalidRegistryObjectException | URISyntaxException e) {
            throw new CoreException(new Status(IStatus.ERROR, bundle.getSymbolicName(), 0, "Error loading template configuration element", e));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public int getRanking() {
        return 0;
    }

    @Override
    public Version getVersion() {
        return null;
    }

    @Override
    public ObjectClassDefinition getMetadata() throws Exception {
        return new ObjectClassDefinitionImpl(name, "", null);
    }

    @Override
    public ResourceMap generateOutputs(Map<String,List<Object>> parameters) throws Exception {
        ResourceMap resourceMap = new ResourceMap();

        if (templatePath != null) {
            if (!templatePath.endsWith("/"))
                templatePath += "/";

            resourceMap.put("cnf/", new FolderResource());

            List<String> dirs = new LinkedList<>();
            dirs.add(templatePath);

            while (!dirs.isEmpty()) {
                String dir = dirs.remove(0);
                for (Enumeration<String> pathEnum = contributor.getEntryPaths(dir); pathEnum.hasMoreElements();) {
                    Resource resource;
                    String fullPath = pathEnum.nextElement();
                    String relativePath = fullPath.substring(templatePath.length());
                    if (fullPath.endsWith("/")) {
                        dirs.add(0, fullPath); // depth first
                        resource = new FolderResource();
                    } else {
                        // TODO what is the encoding??
                        resource = new URLResource(contributor.getEntry(fullPath), "UTF-8");
                    }
                    resourceMap.put("cnf/" + relativePath, resource);
                }
            }
        }
        return resourceMap;
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }

    @Override
    public URI getIcon() {
        return iconUri;
    }

    @Override
    public URI getHelpContent() {
        return helpUri;
    }

}
