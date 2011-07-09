package bndtools;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.classpath.BndContainerInitializer;

public class UpdateClasspathJob extends WorkspaceJob {

    private final boolean cnf;
    private final List<IResource> affectedBnds;

    public UpdateClasspathJob(boolean cnf, List<IResource> affectedBnds) {
        super("Update classpaths");
        this.cnf = cnf;
        this.affectedBnds = affectedBnds;
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        IStatus status = Status.OK_STATUS;
        if (cnf) {
            refreshAll();
        } else {
            try {
                for (IResource resource : affectedBnds) {
                    IProject project = resource.getProject();

                    if (hasBndClasspathContainer(project)) {
                        IJavaProject javaProject = JavaCore.create(project);
                        Project model = Plugin.getDefault().getCentral().getModel(javaProject);

                        refreshProject(javaProject, model);
                    }
                }
            } catch (JavaModelException e) {
                status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error udpating project classpath", e);
                Plugin.log(status);
            } catch (CoreException e) {
                status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error udpating project classpath", e);
                Plugin.log(status);
            }
        }
        return status;
    }

    boolean hasBndClasspathContainer(IProject project) throws CoreException {
        IJavaProject javaProject = JavaCore.create(project);

        IClasspathEntry[] entries = javaProject.getRawClasspath();
        for (IClasspathEntry entry : entries) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && BndContainerInitializer.PATH_ID.equals(entry.getPath()))
                return true;
        }

        return false;
    }

    void refreshProject(IJavaProject javaProject, Project model) throws CoreException {
        if (model != null) {
            model.refresh();
            model.setChanged();
        }

        BndContainerInitializer.updateProjectClasspath(javaProject);
    }

    void refreshAll() {
        try {
            Workspace ws = Central.getWorkspace();
            ws.refresh();

            Collection<Project> projects = ws.getAllProjects();
            for (Project model : projects) {
                IJavaProject javaProject = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(model.getName()));
                refreshProject(javaProject, model);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
