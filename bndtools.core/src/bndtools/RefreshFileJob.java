package bndtools;

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

    private final File file;
    private final boolean derived;

    private final IResource resource;
    private final int depth;

    public RefreshFileJob(File file, boolean derived) throws Exception {
        super("Refreshing file");
        this.file = file;
        this.derived = derived;

        IPath wsPath = Central.toPath(file);
        IResource target;
        if (wsPath == null)
            target = null;
        else
            target = ResourcesPlugin.getWorkspace().getRoot().findMember(wsPath);

        if (!file.isDirectory() && !file.isFile()) {
            // File has been deleted or is something else, e.g a pipe. Check the parent folder
            target = target.getParent();
            this.depth = 1;
        } else {
            this.depth = 0;
        }
        this.resource = target;
    }

    public boolean needsToSchedule() {
        return resource != null && !resource.isSynchronized(depth);
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        resource.refreshLocal(depth, monitor);
        resource.setDerived(derived);

        return Status.OK_STATUS;
    }
}