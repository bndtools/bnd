package bndtools.central;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class RefreshFileJob extends WorkspaceJob {

    private final boolean derived;

    private final IResource resource;
    private final int depth;

    public RefreshFileJob(File file, boolean derived) throws Exception {
        super("Refreshing " + file);
        this.derived = derived;

        IPath wsPath = Central.toPath(file);
        IResource target;
        if (wsPath == null) {
            target = null;
            this.depth = 0;
        } else if (file.isFile()) {
            target = ResourcesPlugin.getWorkspace().getRoot().getFile(wsPath);
            this.depth = 0;
        } else if (file.isDirectory()) {
            target = ResourcesPlugin.getWorkspace().getRoot().getFolder(wsPath);
            this.depth = IResource.DEPTH_INFINITE;
        } else {
            target = ResourcesPlugin.getWorkspace().getRoot().getFolder(wsPath.removeLastSegments(1));
            this.depth = IResource.DEPTH_INFINITE;
        }

        this.resource = target;
    }

    public boolean needsToSchedule() {
        return resource != null && !resource.isSynchronized(depth);
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        resource.refreshLocal(depth, monitor);
        resource.setDerived(derived, null);

        return Status.OK_STATUS;
    }
}