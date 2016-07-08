package bndtools.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.launching.RuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry2;
import org.eclipse.jdt.launching.IRuntimeClasspathEntryResolver;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import bndtools.Plugin;

public class BndContainerRuntimeClasspathEntryResolver implements IRuntimeClasspathEntryResolver {

    /**
     * Cache of already resolved projects in container entries. Used to avoid cycles in project dependencies when
     * resolving classpath container entries. Counters used to know when entering/exiting to clear cache
     */
    private static ThreadLocal<List<IJavaProject>> resolvingProjects = new ThreadLocal<>();
    private static ThreadLocal<Integer> resolvingCount = new ThreadLocal<>();

    @Override
    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, ILaunchConfiguration configuration) throws CoreException {
        IJavaProject project = entry.getJavaProject();

        if (project == null) {
            project = JavaRuntime.getJavaProject(configuration);
        }

        return resolveRuntimeClasspathEntry(entry, project);
    }

    @Override
    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entryToResolve, IJavaProject entryProject) throws CoreException {
        if (entryToResolve == null || entryProject == null) {
            return new IRuntimeClasspathEntry[0];
        }

        final List<IRuntimeClasspathEntry> resolvedRuntimeClasspathEntries = new ArrayList<>();

        final IClasspathContainer container = JavaCore.getClasspathContainer(entryToResolve.getPath(), entryProject);

        if (container == null) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Could not resolve Bnd classpath container", null));
        }

        final IClasspathEntry[] classpathEntries = container.getClasspathEntries();

        List<IJavaProject> projects = resolvingProjects.get();
        Integer count = resolvingCount.get();
        if (projects == null) {
            projects = new ArrayList<>();
            resolvingProjects.set(projects);
            count = 0;
        }

        int intCount = count.intValue();
        intCount++;
        resolvingCount.set(intCount);

        try {
            for (IClasspathEntry classpathEntry : classpathEntries) {
                if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(classpathEntry.getPath().segment(0));
                    final IJavaProject javaProject = JavaCore.create(project);
                    if (!projects.contains(javaProject)) {
                        projects.add(javaProject);
                        final IRuntimeClasspathEntry2 defaultProjectClasspathEntry = (IRuntimeClasspathEntry2) JavaRuntime.newDefaultProjectClasspathEntry(javaProject);
                        final IRuntimeClasspathEntry[] projectRuntimeClasspathEntries = defaultProjectClasspathEntry.getRuntimeClasspathEntries(null);

                        for (IRuntimeClasspathEntry projectRuntimeClasspathEntry : projectRuntimeClasspathEntries) {
                            // the only reason for this entire class is the following check, for Projects that get resolved
                            // from our BndContainer we need to override the default behavior found here: JavaRuntime.resolveOutputLocations(IJavaProject, int)
                            // instead of resolving all output locations we simply just return the project runtime classpath entry itself
                            if (projectRuntimeClasspathEntry.getType() == IRuntimeClasspathEntry.PROJECT) {
                                IResource resource = projectRuntimeClasspathEntry.getResource();
                                if (resource instanceof IProject) {
                                    resolvedRuntimeClasspathEntries.add(projectRuntimeClasspathEntry);
                                }
                            } else {
                                IRuntimeClasspathEntry[] resolvedEntries = JavaRuntime.resolveRuntimeClasspathEntry(projectRuntimeClasspathEntry, javaProject);
                                for (IRuntimeClasspathEntry resolvedEntry : resolvedEntries) {
                                    resolvedRuntimeClasspathEntries.add(resolvedEntry);
                                }
                            }
                        }
                    }
                } else {
                    final IRuntimeClasspathEntry runtimeClasspathEntry = new RuntimeClasspathEntry(classpathEntry);
                    if (!resolvedRuntimeClasspathEntries.contains(runtimeClasspathEntry)) {
                        resolvedRuntimeClasspathEntries.add(runtimeClasspathEntry);
                    }
                }
            }
        } finally {
            intCount--;
            if (intCount == 0) {
                resolvingProjects.set(null);
                resolvingCount.set(null);
            } else {
                resolvingCount.set(intCount);
            }
        }

        for (IRuntimeClasspathEntry resolvedRuntimeClasspathEntry : resolvedRuntimeClasspathEntries) {
            resolvedRuntimeClasspathEntry.setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
        }

        return resolvedRuntimeClasspathEntries.toArray(new IRuntimeClasspathEntry[0]);
    }

    @Override
    public IVMInstall resolveVMInstall(IClasspathEntry entry) throws CoreException {
        return null;
    }

}
