package org.bndtools.templating.jgit;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bndtools.templating.FileResource;
import org.bndtools.templating.FolderResource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.templating.util.ObjectClassDefinitionImpl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.osgi.framework.Version;
import org.osgi.service.metatype.ObjectClassDefinition;

import aQute.lib.io.IO;

public class GitCloneTemplate implements Template {

    private final GitCloneTemplateParams params;

    private Repository checkedOut = null;

    public GitCloneTemplate(GitCloneTemplateParams params) {
        this.params = params;
    }

    @Override
    public String getName() {
        return params.name != null ? params.name : params.cloneUrl;
    }

    @Override
    public String getShortDescription() {
        String desc;
        String branch = params.branch != null ? params.branch : GitCloneTemplateParams.DEFAULT_BRANCH;
        if (params.name == null) {
            desc = branch;
        } else {
            desc = params.cloneUrl + " " + branch;
        }
        return desc;
    }

    @Override
    public String getCategory() {
        return params.category;
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
        return getMetadata(new NullProgressMonitor());
    }

    @Override
    public ObjectClassDefinition getMetadata(IProgressMonitor monitor) throws Exception {
        return new ObjectClassDefinitionImpl(getName(), getShortDescription(), null);
    }

    @Override
    public ResourceMap generateOutputs(Map<String,List<Object>> parameters) throws Exception {
        return generateOutputs(parameters, new NullProgressMonitor());
    }

    @Override
    public ResourceMap generateOutputs(Map<String,List<Object>> parameters, IProgressMonitor monitor) throws Exception {
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
            String branch = params.branch != null ? params.branch : GitCloneTemplateParams.DEFAULT_BRANCH;

            try {
                CloneCommand cloneCmd = Git.cloneRepository().setURI(params.cloneUrl).setDirectory(workingDir).setNoCheckout(true);
                cloneCmd.setProgressMonitor(new EclipseGitProgressTransformer(monitor));
                cloneCmd.setBranchesToClone(Collections.singleton(branch));
                Git git = cloneCmd.call();

                git.checkout().setCreateBranch(true).setName("_tmp").setStartPoint(branch).call();
                checkedOut = git.getRepository();
            } catch (JGitInternalException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception)
                    throw (Exception) cause;
                throw e;
            }
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
        return params.iconUri;
    }

    @Override
    public URI getHelpContent() {
        return params.helpUri;
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
