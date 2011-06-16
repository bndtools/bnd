package bndtools;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import aQute.bnd.build.Project;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Jar;

class StaleRepositoryIndexDeleter implements RepositoryListenerPlugin {

    public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {
        try {
            if (repository != LocalRepositoryTasks.getLocalRepository())
                return;

            IProject cnfProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Project.BNDCNF);
            IFile repoFile = cnfProject.getFile("repository.xml");
            repoFile.delete(true, false, null);
        } catch (CoreException e) {
            Activator.instance.error("Error occurred deleting stale repository index file.", e);
        }
    }

}
