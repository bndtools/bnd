package bndtools.wizards.workspace;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;

import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.bnd.service.RepositoryPlugin;
import bndtools.LocalRepositoryTasks;
import bndtools.Plugin;

final class AddOBRResourcesToWorkspaceTask implements IRunnableWithProgress {

    private final Collection<Resource> adding;
    private final MultiStatus status;

    AddOBRResourcesToWorkspaceTask(Collection<Resource> adding, MultiStatus status) {
        this.adding = adding;
        this.status = status;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
            ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException {
                    SubMonitor progress = SubMonitor.convert(monitor, "Copying files to repository", 4 + adding.size());

                    LocalRepositoryTasks.configureBndWorkspace(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                    RepositoryPlugin localRepo = LocalRepositoryTasks.getLocalRepository();
                    LocalRepositoryTasks.installImplicitRepositoryContents(false, status, progress.newChild(2, SubMonitor.SUPPRESS_NONE), localRepo);

                    int workRemaining = adding.size() + 1;
                    for (Resource resource : adding) {
                        progress.setWorkRemaining(workRemaining);
                        try {
                            URL url = new URL(resource.getURI());
                            Number size = resource.getSize();
                            LocalRepositoryTasks.installBundle(localRepo, url, size != null ? size.intValue() : -1, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                            progress.worked(1);
                        } catch (IOException e) {
                            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error copying resource {0}.", resource.getId()), e));
                        } catch (CoreException e) {
                            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error copying resource {0}.", resource.getId()), e));
                        }
                        workRemaining --;
                    }
                    LocalRepositoryTasks.refreshWorkspaceForRepository(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                }
            }, monitor);
        } catch (CoreException e) {
            throw new InvocationTargetException(e);
        }
    }
}