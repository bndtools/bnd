package bndtools.m2e;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

import aQute.bnd.build.Run;

public interface MavenRunListenerHelper {

    final IMavenProjectRegistry mavenProjectRegistry = MavenPlugin.getMavenProjectRegistry();

    default IResource getResource(Run run) {
        File propertiesFile = run.getPropertiesFile();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IFile[] locations = root.findFilesForLocationURI(propertiesFile.toURI());
        IFile shortest = null;
        for (IFile f : locations) {
            if (shortest == null || (f.getProjectRelativePath()
                .segmentCount() < shortest.getProjectRelativePath()
                    .segmentCount())) {
                shortest = f;
            }
        }
        return shortest;
    }

    default boolean isMavenProject(IResource resource) {
        if ((resource != null) && (resource.getProject() != null) && (mavenProjectRegistry.getProject(resource.getProject()) != null)) {
            return true;
        }

        return false;
    }

}
