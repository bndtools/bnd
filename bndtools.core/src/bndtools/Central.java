package bndtools;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.plugin.Activator;
import aQute.bnd.plugin.ModelListener;
import aQute.bnd.service.Refreshable;

public class Central {
    static Workspace workspace = null;

    final Map<IJavaProject, Project> javaProjectToModel = new HashMap<IJavaProject, Project>();
    final List<ModelListener>        listeners          = new CopyOnWriteArrayList<ModelListener>();

    Central() { }

    public Project getModel(IJavaProject project) {
        try {
            Project model = javaProjectToModel.get(project);
             if (model == null) {
                File projectDir = project.getProject().getLocation()
                        .makeAbsolute().toFile();
                try {
                    model = Workspace.getProject(projectDir);
                } catch (IllegalArgumentException e) {
//                    initialiseWorkspace();
//                    model = Workspace.getProject(projectDir);
                    return null;
                }
                if (workspace == null) {
                    model.getWorkspace();
                }
                if (model != null) {
                    javaProjectToModel.put(project, model);
                }
            }
            return model;
        } catch (Exception e) {
            // TODO do something more useful here
            throw new RuntimeException(e);
        }
    }

    /**
     * Implementation of the resource changed interface. We are checking in the
     * POST_CHANGE phase if one of our tracked models needs to be updated.
     */
    public synchronized void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
            return;

        IResourceDelta rootDelta = event.getDelta();
        try {
            final Set<Project> changed = new HashSet<Project>();
            rootDelta.accept(new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    try {

                        IPath location = delta.getResource().getLocation();
                        if (location == null) {
                            System.out
                                    .println("Cannot convert resource to file: "
                                            + delta.getResource());
                        } else {
                            File file = location.toFile();
                            File parent = file.getParentFile();
                            boolean parentIsWorkspace = parent
                                    .equals(getWorkspace().getBase());

                            // file
                            // /development/osgi/svn/build/org.osgi.test.cases.distribution/bnd.bnd
                            // parent
                            // /development/osgi/svn/build/org.osgi.test.cases.distribution
                            // workspace /development/amf/workspaces/osgi
                            // false

                            if (parent != null && parentIsWorkspace) {
                                // We now are on project level, we do not go
                                // deeper
                                // because projects/workspaces should check for
                                // any
                                // changes.
                                // We are careful not to create unnecessary
                                // projects
                                // here.
                                if (file.getName().equals(Workspace.CNFDIR)) {
                                    if (workspace.refresh()) {
                                        changed.addAll(workspace
                                                .getCurrentProjects());
                                    }
                                    return false;
                                }
                                if (workspace.isPresent(file.getName())) {
                                    Project project = workspace.getProject(file
                                            .getName());
                                    changed.add(project);
                                } else {
                                    ; // Project not created yet, so we
                                    // have
                                    // no cached results

                                }
                                return false;
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new CoreException(new Status(Status.ERROR,
                                Activator.PLUGIN_ID,
                                "During checking project changes", e));
                    }
                }

            });

            for (Project p : changed) {
                p.refresh();
                changed(p);

            }
        } catch (CoreException e) {
            Activator.getDefault().error("While handling changes", e);
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Workspace getWorkspace() throws Exception {
        if (workspace != null)
            return workspace;

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        File wsdir = wsroot.getLocation().toFile();

        try {
            workspace = Workspace.getWorkspace(wsdir);
        } catch (IllegalArgumentException e) {
            return null;

            // // Ensure the workspace is configured
            // initialiseWorkspace();
            // // Retry
            // workspace = Workspace.getWorkspace(wsdir);
        }
        workspace.addBasicPlugin(new FilesystemUpdateListener());

        Activator.getDefault().getBundleContext().registerService(Workspace.class.getName(), workspace, null);

        return workspace;
    }

    private static void initialiseWorkspace() throws CoreException {
        IWorkspaceRunnable wsop = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                LocalRepositoryTasks.configureBndWorkspace(monitor);
            }
        };
        ResourcesPlugin.getWorkspace().run(wsop, null);
    }

    public void changed(Project model) {
        model.setChanged();
        for (ModelListener m : listeners)
            try {
                m.modelChanged(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public void addModelListener(ModelListener m) {
        if (!listeners.contains(m)) {
            listeners.add(m);
        }
    }

    public void removeModelListener(ModelListener m) {
        listeners.remove(m);
    }

    public IJavaProject getJavaProject(Project model) {
        for (IProject iproj : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            if (iproj.getName().equals(model.getName())) {
                IJavaProject ij = JavaCore.create(iproj);
                if (ij != null && ij.exists()) {
                    return ij;
                }
                // current project is not a Java project
            }
        }
        return null;
    }

    public static IPath toPath(Project project, File file) {
        String path = file.getAbsolutePath();
        String workspace = project.getWorkspace().getBase().getAbsolutePath();
        if (path.startsWith(workspace))
            path = path.substring(workspace.length());
        else
            return null;

        IPath p = new Path(path);
        return p;
    }

    public static void refresh(IPath path) {
        try {
            IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(
                    path);
            if (r != null)
                return;

            IPath p = (IPath) path.clone();
            while (p.segmentCount() > 0) {
                p = p.removeLastSegments(1);
                IResource resource = ResourcesPlugin.getWorkspace().getRoot()
                        .findMember(p);
                if (resource != null) {
                    resource.refreshLocal(2, null);
                    return;
                }
            }
        } catch( ResourceException re ) {
            // TODO Ignore for now
        }
        catch (Exception e) {
            Activator.getDefault().error("While refreshing path " + path, e);
        }
    }

    public void refreshPlugins() throws Exception {
        List<Refreshable> rps = getWorkspace().getPlugins(Refreshable.class);
        for (Refreshable rp : rps) {
            if (rp.refresh()) {
                File dir = rp.getRoot();
                refreshFile(dir);
            }
        }
    }

    public void refreshFile(File f) throws Exception {
        String path = toLocal(f);
        IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if (r != null) {
            r.refreshLocal(IResource.DEPTH_INFINITE, null);
        }
    }

    public void refresh(Project p) throws Exception {
        IJavaProject jp = getJavaProject(p);
        if (jp != null)
            jp.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    private String toLocal(File f) throws Exception {
        String root = getWorkspace().getBase().getAbsolutePath();
        String path = f.getAbsolutePath().substring(root.length());
        return path;
    }

    public void close() {
    }
}
