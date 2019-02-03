package bndtools.m2e;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import aQute.bnd.build.Run;
import aQute.lib.exceptions.Exceptions;

public interface MavenRunListenerHelper {

    final IMaven maven = MavenPlugin.getMaven();
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

    default MavenProject getMavenProject(IMavenProjectFacade mavenProjectFacade) {
        try {
            return mavenProjectFacade.getMavenProject(new NullProgressMonitor());
        } catch (CoreException e) {
            throw Exceptions.duck(e);
        }
    }

    default IMavenProjectFacade getMavenProjectFacade(IResource resource) {
        return mavenProjectRegistry.getProject(resource.getProject());
    }

    default boolean isMavenProject(IResource resource) {
        if ((resource != null) && (resource.getProject() != null) && (getMavenProjectFacade(resource) != null)) {
            return true;
        }

        return false;
    }

    default boolean hasBndMavenPlugin(IMavenProjectFacade projectFacade) throws CoreException {
        return projectFacade.getMojoExecutions("biz.aQute.bnd", "bnd-maven-plugin", new NullProgressMonitor(), "bnd-process")
            .stream()
            .findFirst()
            .isPresent();
    }

    default boolean hasBndResolverMavenPlugin(IMavenProjectFacade projectFacade) throws CoreException {
        return projectFacade.getMojoExecutions("biz.aQute.bnd", "bnd-resolver-maven-plugin", new NullProgressMonitor(), "resolve")
            .stream()
            .findFirst()
            .isPresent();
    }

    default boolean hasBndTestingMavenPlugin(IMavenProjectFacade projectFacade) throws CoreException {
        return projectFacade.getMojoExecutions("biz.aQute.bnd", "bnd-testing-maven-plugin", new NullProgressMonitor(), "testing")
            .stream()
            .findFirst()
            .isPresent();
    }

    default boolean isOffline() {
        try {
            return maven.getSettings()
                .isOffline();
        } catch (CoreException e) {
            throw Exceptions.duck(e);
        }
    }

    default <T> T lookupComponent(Class<T> clazz) {
        try {
            Method lookupComponentMethod = maven.getClass()
                .getMethod("lookupComponent", Class.class);

            return clazz.cast(lookupComponentMethod.invoke(maven, clazz));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

}
