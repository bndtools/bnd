package bndtools;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.central.Central;
import bndtools.central.RepositoryUtils;

public class RefreshReposHandler extends AbstractHandler {

    private static final ILogger logger = Logger.getLogger(RefreshReposHandler.class);

    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        try {
            final IFile buildFile = Central.getWorkspaceBuildFile();
            if (buildFile == null) {
                MessageDialog.openError(window.getShell(), "Error", "Unable to refresh repositories: workspace build file is missing.");
                return null;
            }

            window.run(true, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        buildFile.getWorkspace().run(new IWorkspaceRunnable() {
                            public void run(IProgressMonitor monitor) throws CoreException {
                                List<RepositoryPlugin> repos = RepositoryUtils.listRepositories(true);
                                for (RepositoryPlugin i : repos) {
                                    if (i instanceof Refreshable) {
                                        boolean success = false;
                                        try {
                                            success = ((Refreshable) i).refresh();
                                        } catch (Exception e) {
                                            logger.logError("Exception while refreshing repository: " + i.getName(), e);
                                        }
                                        if (!success) {
                                            logger.logError("Failed to refresh repository: " + i.getName(), null);
                                        }
                                    }
                                }

                                buildFile.touch(monitor);
                            }
                        }, monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (Exception e) {
            logger.logError("Error refreshing repositories.", e);
        }

        return null;
    }
}
