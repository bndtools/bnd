package org.bndtools.builder.classpath;

import java.io.Serializable;
import java.util.List;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

public class BndContainer implements IClasspathContainer, Serializable {
    private static final long serialVersionUID = 2L;
    public static final String DESCRIPTION = "Bnd Bundle Path";
    private static final IClasspathEntry[] EMPTY_ENTRIES = new IClasspathEntry[0];
    private final IClasspathEntry[] entries;
    private final long lastModified;
    private transient volatile List<IResource> resources;

    BndContainer() {
        this(EMPTY_ENTRIES, 0L, null);
    }

    BndContainer(List<IClasspathEntry> entries, long lastModified, List<IResource> resources) {
        this(entries.toArray(EMPTY_ENTRIES), lastModified, resources);
    }

    private BndContainer(IClasspathEntry[] entries, long lastModified, List<IResource> resources) {
        this.entries = entries;
        this.lastModified = lastModified;
        this.resources = (resources == null) || resources.isEmpty() ? null : resources;
    }

    @Override
    public IClasspathEntry[] getClasspathEntries() {
        return entries;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public int getKind() {
        return IClasspathContainer.K_APPLICATION;
    }

    @Override
    public IPath getPath() {
        return BndtoolsConstants.BND_CLASSPATH_ID;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    long lastModified() {
        return lastModified;
    }

    void refresh() throws CoreException {
        List<IResource> files = resources;
        if (files == null) {
            return;
        }
        if (ResourcesPlugin.getWorkspace().isTreeLocked()) {
            return;
        }
        for (IResource target : files) {
            int depth = target.getType() == IResource.FILE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
            if (!target.isSynchronized(depth)) {
                target.refreshLocal(depth, null);
            }
        }
        resources = null;
    }
}
