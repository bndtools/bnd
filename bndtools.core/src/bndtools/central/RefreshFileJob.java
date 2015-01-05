package bndtools.central;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
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
    private final List<File> files;

    public RefreshFileJob(File file, boolean derived) throws Exception {
        super("Refreshing " + file);
        this.derived = derived;
        this.files = Collections.singletonList(file);
    }

    public RefreshFileJob(List<File> filesToRefresh, boolean derived, IProject project) {
        super("Refreshing " + project.getName());
        this.derived = derived;
        this.files = filesToRefresh;
    }

    public boolean needsToSchedule() {
        return true;
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        IStatus ret = Status.OK_STATUS;
        for (File file : files) {
            IPath wsPath;
            try {
                // This call is taking a long time. Thus it was moved out of the constructor into runInWorkspace()
                wsPath = Central.toPathMustBeInEclipseWorkspace(file);
            } catch (Exception e) {
                // If we had a reference to something not in a project, this would cause a LOT of extra dialogs to pop up. Yes, I tried it.
                //ret = new Status(Status.ERROR, "RefreshFileJob", "Unable to find file=" + file);
                continue;
            }
            int depth = 0;
            IResource target;
            if (wsPath == null) {
                target = null;
            } else if (file.isFile()) {
                target = ResourcesPlugin.getWorkspace().getRoot().getFile(wsPath);
            } else if (file.isDirectory()) {
                target = ResourcesPlugin.getWorkspace().getRoot().getFolder(wsPath);
                depth = IResource.DEPTH_INFINITE;
            } else {
                target = ResourcesPlugin.getWorkspace().getRoot().getFolder(wsPath.removeLastSegments(1));
                depth = IResource.DEPTH_INFINITE;
            }

            if (target != null && !target.isSynchronized(depth)) {
                target.refreshLocal(depth, monitor);
                target.setDerived(derived, null);
            }
        }

        return ret;
    }
}
