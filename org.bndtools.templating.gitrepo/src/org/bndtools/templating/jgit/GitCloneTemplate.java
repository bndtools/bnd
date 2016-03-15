package org.bndtools.templating.jgit;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.bndtools.templating.FileResource;
import org.bndtools.templating.FolderResource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.templating.util.ObjectClassDefinitionImpl;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.osgi.framework.Version;
import org.osgi.service.metatype.ObjectClassDefinition;

import aQute.lib.io.IO;

public class GitCloneTemplate implements Template {

    private final String cloneUrl;
    private final String name;
    private final String description;
    private final String category;
    private final URI iconUri;

    private Repository checkedOut = null;

    public GitCloneTemplate(String cloneUrl, String name, String description, String category, URI iconUri) {
        this.cloneUrl = cloneUrl;
        this.name = name;
        this.description = description;
        this.category = category;
        this.iconUri = iconUri;
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
        return new ObjectClassDefinitionImpl(getName(), getShortDescription(), null);
    }

    @Override
    public ResourceMap generateOutputs(Map<String,List<Object>> parameters) throws Exception {
        File workingDir = null;
        File gitDir = null;

        // Get existing checkout if available
        synchronized (this) {
            if (checkedOut != null) {
                workingDir = checkedOut.getWorkTree();
                gitDir = new File(workingDir, ".git");
            }
        }

        if (workingDir == null) {
            // Need to do a new checkout
            workingDir = Files.createTempDirectory("checkout").toFile();
            gitDir = new File(workingDir, ".git");

            Git call = Git.cloneRepository().setURI(cloneUrl).setBranch("master").setDirectory(workingDir).call();
            checkedOut = call.getRepository();
        }

        final File exclude = gitDir;
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File path) {
                return !path.equals(exclude);
            }
        };
        return toResourceMap(workingDir, filter);
    }

    @Override
    public void close() throws IOException {
        File tempDir = null;
        synchronized (this) {
            if (checkedOut != null)
                tempDir = checkedOut.getWorkTree();
        }
        if (tempDir != null)
            IO.delete(tempDir);
    }

    @Override
    public URI getIcon() {
        return iconUri;
    }

    @Override
    public URI getHelpContent() {
        return null;
    }

    private static ResourceMap toResourceMap(File baseDir, FileFilter filter) {
        ResourceMap result = new ResourceMap();
        File[] files = baseDir.listFiles(filter);
        if (files != null)
            for (File file : files) {
                recurse("", file, filter, result);
            }
        return result;
    }

    private static void recurse(String prefix, File file, FileFilter filter, ResourceMap resourceMap) {
        if (file.isDirectory()) {
            String path = prefix + file.getName() + "/";
            resourceMap.put(path, new FolderResource());

            File[] children = file.listFiles(filter);
            for (File child : children) {
                recurse(path, child, filter, resourceMap);
            }
        } else {
            String path = prefix + file.getName();
            // TODO: WTF is the encoding?
            resourceMap.put(path, new FileResource(file, "UTF-8"));
        }
    }
}
