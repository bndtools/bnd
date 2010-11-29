package bndtools;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

public class RefreshFileJob extends WorkspaceJob {

    private final File file;
    private final Path changedPath;

    public RefreshFileJob(File file) {
        super("update");
        this.file = file;

        changedPath = new Path(file.toString());
    }

    public boolean isFileInWorkspace() {
        return ResourcesPlugin.getWorkspace().getRoot().getLocation().isPrefixOf(changedPath);
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
        IPath workspaceLocation = wsRoot.getLocation();
        IPath relativeChangedPath = changedPath.removeFirstSegments(workspaceLocation.segmentCount());

        IResource resource;
        int depth;
        if (file.isDirectory()) {
            resource = wsRoot.getFolder(relativeChangedPath);
            depth = 0;
        } else if (file.isFile()) {
            resource = wsRoot.getFile(relativeChangedPath);
            depth = 0;
        } else {
            // File has been deleted or is something else, e.g a pipe.
            // Check the parent folder
            resource = wsRoot.getFolder(relativeChangedPath.removeLastSegments(1));
            depth = 1;
        }
        resource.refreshLocal(depth, monitor);
        resource.setDerived(true);
        return Status.OK_STATUS;
    }
}