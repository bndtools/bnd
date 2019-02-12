package bndtools.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntryResolver;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.strings.Strings;
import bndtools.Activator;
import bndtools.central.Central;

public class BndContainerRuntimeClasspathEntryResolver implements IRuntimeClasspathEntryResolver {

    @Override
    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, ILaunchConfiguration configuration) throws CoreException {
        IJavaProject project = entry.getJavaProject();

        if (project == null) {
            project = JavaRuntime.getJavaProject(configuration);
        }

        return resolveRuntimeClasspathEntry(entry, project);
    }

    @Override
    public IVMInstall resolveVMInstall(IClasspathEntry entry) throws CoreException {
        return null;
    }

    @Override
    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entryToResolve, IJavaProject entryProject) throws CoreException {
        if (entryToResolve == null || entryProject == null) {
            return new IRuntimeClasspathEntry[0];
        }

        try {
            return Central.bndCall(() -> {
                List<IRuntimeClasspathEntry> resolved = new ArrayList<>();
                Set<File> classpath = new HashSet<>();

                Project project = Central.getProject(entryProject.getProject());

                List<Container> containers = new ArrayList<>(project.getBuildpath());
                containers.addAll(project.getTestpath());

                int errorMarker = project.getErrors()
                    .size();

                for (Container container : containers) {
                    if (container.getError() != null) {
                        project.error("Cannot launch because %s has reported %s", container.getProject(), container.getError());
                    } else {
                        Collection<Container> members = container.getMembers();
                        for (Container m : members) {
                            if (!classpath.contains(m.getFile())) {
                                classpath.add(m.getFile());
                                IPath path = Path.fromOSString(m.getFile()
                                    .getAbsolutePath());
                                IRuntimeClasspathEntry entry = JavaRuntime.newArchiveRuntimeClasspathEntry(path);
                                resolved.add(entry);
                            }
                        }
                    }
                }
                if (project.getErrors()
                    .size() > errorMarker) {
                    Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, Status.ERROR, Strings.join("\n", project.getErrors()), null);
                    project.clear();
                    throw new CoreException(status);
                }

                return resolved.toArray(new IRuntimeClasspathEntry[0]);
            });
        } catch (CoreException ee) {
            throw ee;
        } catch (Exception e) {
            throw Exceptions.duck(e);
        }
    }

}