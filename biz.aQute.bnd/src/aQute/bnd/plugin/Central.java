package aQute.bnd.plugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.osgi.framework.*;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.classpath.*;
import aQute.bnd.service.*;

public class Central implements IResourceChangeListener {
    static IWorkspace                iworkspace;
    final Map<IJavaProject, Project> javaProjectToModel = new HashMap<IJavaProject, Project>();
    final List<ModelListener>        listeners          = new CopyOnWriteArrayList<ModelListener>();
    static Workspace                 workspace;
    final BundleContext              context;
    final Workspace                  ws;

    Central(BundleContext context) throws Exception {
        this.context = context;
        // Add a resource change listener if this is
        // the first project
        iworkspace = ResourcesPlugin.getWorkspace();
        iworkspace.addResourceChangeListener(this);

        ws = getWorkspace();
        context.registerService(Workspace.class.getName(), ws, null);
    }

    public Project getModel(IJavaProject project) {
        try {
            Project model = javaProjectToModel.get(project);
            if (model == null) {
                File projectDir = project.getProject().getLocation()
                        .makeAbsolute().toFile();
                model = Workspace.getProject(projectDir);
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
            	//p.updateModified(now, "Eclipse resource listener");
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

        IResource resource = iworkspace.getRoot().findMember("/cnf/build.bnd");
        if (resource != null) {
            IPath path = resource.getLocation();
            if (path != null) {
                File f = path.toFile();
                workspace = Workspace.getWorkspace(f.getAbsoluteFile()
                        .getParentFile().getParentFile().getAbsoluteFile());
                // workspace.setBundleContex(context);
                return workspace;
            }
        }

        workspace = Workspace.getWorkspace(iworkspace.getRoot().getLocation()
                .toFile());
        return workspace;
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
        for (IProject iproj : iworkspace.getRoot().getProjects()) {
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
