package bndtools.wizards.repo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import aQute.bnd.build.Project;
import bndtools.RepositoryIndexerJob;
import bndtools.bindex.IRepositoryIndexProvider;

public class LocalRepositoryIndexProvider implements IRepositoryIndexProvider {

    private File getFile() {
        IProject cnf = ResourcesPlugin.getWorkspace().getRoot().getProject(Project.BNDCNF);
        IFile repoFile = cnf.getFile("repository.xml");
        return new File(repoFile.getLocationURI());
    }

    public void initialise(IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, "Updating local repository index...", 1);
        RepositoryIndexerJob.runIfNeeded();
        RepositoryIndexerJob.joinRunningInstance(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
    }

    public URL getUrl() {
        try {
            return getFile().toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
