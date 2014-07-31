package bndtools.central;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.IStartupParticipant;
import org.bndtools.api.Logger;
import org.bndtools.api.ModelListener;
import org.bndtools.utils.Function;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;

public class Central implements IStartupParticipant {

    private static final ILogger logger = Logger.getLogger(Central.class);

    private static Central instance = null;

    static Workspace workspace = null;
    static final List<Function<Workspace,Void>> workspaceInitCallbackQueue = new LinkedList<Function<Workspace,Void>>();

    static WorkspaceR5Repository r5Repository = null;
    static RepositoryPlugin workspaceRepo = null;

    static final AtomicBoolean indexValid = new AtomicBoolean(false);
    static final ConcurrentMap<String,Map<String,SortedSet<Version>>> exportedPackageMap = new ConcurrentHashMap<String,Map<String,SortedSet<Version>>>();
    static final ConcurrentMap<String,Collection<String>> containedPackageMap = new ConcurrentHashMap<String,Collection<String>>();
    static final ConcurrentMap<String,Collection<IResource>> sourceFolderMap = new ConcurrentHashMap<String,Collection<IResource>>();

    private final BundleContext bundleContext;
    private final Map<IJavaProject,Project> javaProjectToModel = new HashMap<IJavaProject,Project>();
    private final List<ModelListener> listeners = new CopyOnWriteArrayList<ModelListener>();

    private RepositoryListenerPluginTracker repoListenerTracker;

    /**
     * WARNING: Do not instantiate this class. It must be public to allow instantiation by the Eclipse registry, but it
     * is not intended for direct creation by clients. Instead call Central.getInstance().
     */
    @Deprecated
    public Central() {
        bundleContext = FrameworkUtil.getBundle(Central.class).getBundleContext();
    }

    @Override
    public void start() {
        synchronized (Central.class) {
            instance = this;
        }

        repoListenerTracker = new RepositoryListenerPluginTracker(bundleContext);
        repoListenerTracker.open();
    }

    @Override
    public void stop() {
        repoListenerTracker.close();

        synchronized (Central.class) {
            instance = null;
        }
    }

    public static Central getInstance() {
        synchronized (Central.class) {
            return instance;
        }
    }

    public Project getModel(IJavaProject project) {
        try {
            Project model = javaProjectToModel.get(project);
            if (model == null) {
                File projectDir = project.getProject().getLocation().makeAbsolute().toFile();
                try {
                    model = getProject(projectDir);
                } catch (IllegalArgumentException e) {
                    // initialiseWorkspace();
                    // model = Central.getProject(projectDir);
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
     * Implementation of the resource changed interface. We are checking in the POST_CHANGE phase if one of our tracked
     * models needs to be updated.
     */
    public synchronized void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
            return;

        IResourceDelta rootDelta = event.getDelta();
        try {
            final Set<Project> changed = new HashSet<Project>();
            rootDelta.accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(IResourceDelta delta) throws CoreException {
                    try {

                        IPath location = delta.getResource().getLocation();
                        if (location == null) {
                            System.out.println("Cannot convert resource to file: " + delta.getResource());
                        } else {
                            File file = location.toFile();
                            File parent = file.getParentFile();
                            boolean parentIsWorkspace = parent.equals(getWorkspace().getBase());

                            // file
                            // /development/osgi/svn/build/org.osgi.test.cases.distribution/bnd.bnd
                            // parent
                            // /development/osgi/svn/build/org.osgi.test.cases.distribution
                            // workspace /development/amf/workspaces/osgi
                            // false

                            if (parentIsWorkspace) {
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
                                        changed.addAll(workspace.getCurrentProjects());
                                    }
                                    return false;
                                }
                                if (workspace.isPresent(file.getName())) {
                                    Project project = workspace.getProject(file.getName());
                                    changed.add(project);
                                } else {
                                    // Project not created yet, so we
                                    // have
                                    // no cached results

                                }
                                return false;
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new CoreException(new Status(Status.ERROR, BndtoolsConstants.CORE_PLUGIN_ID, "During checking project changes", e));
                    }
                }

            });

            for (Project p : changed) {
                p.refresh();
                changed(p);

            }
        } catch (CoreException e) {
            logger.logError("While handling changes", e);
        }
    }

    public static IFile getWorkspaceBuildFile() throws Exception {
        File file = Central.getWorkspace().getPropertiesFile();
        IFile[] matches = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());

        if (matches == null || matches.length != 1) {
            logger.logError("Cannot find workspace location for bnd configuration file " + file, null);
            return null;
        }

        return matches[0];
    }

    public synchronized static WorkspaceR5Repository getWorkspaceR5Repository() throws Exception {
        if (r5Repository != null)
            return r5Repository;

        r5Repository = new WorkspaceR5Repository();
        r5Repository.init();

        return r5Repository;
    }

    public synchronized static RepositoryPlugin getWorkspaceRepository() throws Exception {
        if (workspaceRepo != null)
            return workspaceRepo;

        workspaceRepo = new WorkspaceRepository(getWorkspace());
        return workspaceRepo;
    }

    public synchronized static Workspace getWorkspace() throws Exception {
        if (instance == null)
            throw new IllegalStateException("Central has not been initialised");

        if (workspace != null)
            return workspace;

        Workspace newWorkspace = null;

        try {
            Workspace.setDriver(Constants.BNDDRIVER_ECLIPSE);
            newWorkspace = Workspace.getWorkspace(getWorkspaceDirectory());

            newWorkspace.addBasicPlugin(new WorkspaceListener(newWorkspace));
            newWorkspace.addBasicPlugin(instance.repoListenerTracker);
            newWorkspace.addBasicPlugin(getWorkspaceR5Repository());

            // Initialize projects in synchronized block
            newWorkspace.getBuildOrder();

            // Monitor changes in cnf so we can refresh the workspace
            addCnfChangeListener(newWorkspace);

            // The workspace has been initialized fully, set the field now
            workspace = newWorkspace;

            // Call the queued workspace init callbacks
            while (!workspaceInitCallbackQueue.isEmpty()) {
                Function<Workspace,Void> callback = workspaceInitCallbackQueue.remove(0);
                callback.run(workspace);
            }

            return workspace;
        } catch (final Exception e) {
            if (newWorkspace != null) {
                newWorkspace.close();
            }
            throw e;
        }
    }

    public synchronized static void onWorkspaceInit(Function<Workspace,Void> callback) {
        if (workspace != null)
            callback.run(workspace);
        else
            workspaceInitCallbackQueue.add(callback);
    }

    private static File getWorkspaceDirectory() throws CoreException {
        IWorkspaceRoot eclipseWorkspace = ResourcesPlugin.getWorkspace().getRoot();

        IProject cnfProject = eclipseWorkspace.getProject("bnd");
        if (!cnfProject.exists())
            cnfProject = eclipseWorkspace.getProject("cnf");

        if (cnfProject.exists()) {
            if (!cnfProject.isOpen())
                cnfProject.open(null);
            return cnfProject.getLocation().toFile().getParentFile();
        }

        // Have to assume that the eclipse workspace == the bnd workspace,
        // and cnf hasn't been imported yet.
        return eclipseWorkspace.getLocation().toFile();
    }

    private static void addCnfChangeListener(final Workspace workspace) {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {

            @Override
            public void resourceChanged(IResourceChangeEvent event) {
                if (event.getType() != IResourceChangeEvent.POST_CHANGE)
                    return;

                IResourceDelta rootDelta = event.getDelta();
                if (isCnfChanged(rootDelta)) {
                    workspace.refresh();
                }
            }
        });
    }

    public static boolean isCnfChanged(IResourceDelta delta) {

        final AtomicBoolean result = new AtomicBoolean(false);
        try {
            delta.accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(IResourceDelta delta) throws CoreException {
                    try {

                        if (!isChangeDelta(delta))
                            return false;

                        IResource resource = delta.getResource();
                        if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT && resource.getName().equals(Workspace.CNFDIR))
                            return true;

                        if (resource.getType() == IResource.PROJECT)
                            return false;

                        if (resource.getType() == IResource.FOLDER && resource.getName().equals("ext")) {
                            result.set(true);
                            return false;
                        }

                        if (resource.getType() == IResource.FILE) {
                            if (Workspace.BUILDFILE.equals(resource.getName())) {
                                result.set(true);
                                return false;
                            }
                            // Check files included by the -include directive in build.bnd
                            List<File> includedFiles = workspace.getIncluded();
                            if (includedFiles == null) {
                                return false;
                            }
                            for (File includedFile : includedFiles) {
                                IPath location = resource.getLocation();
                                if (location != null && includedFile.equals(location.toFile())) {
                                    result.set(true);
                                    return false;
                                }
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        throw new CoreException(new Status(Status.ERROR, BndtoolsConstants.CORE_PLUGIN_ID, "During checking project changes", e));
                    }
                }

            });
        } catch (CoreException e) {
            logger.logError("Central.isCnfChanged() failed", e);
        }
        return result.get();
    }

    public static boolean isChangeDelta(IResourceDelta delta) {
        if (IResourceDelta.MARKERS == delta.getFlags())
            return false;
        if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) == 0)
            return false;
        return true;
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

    public static IJavaProject getJavaProject(Project model) {
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

    public static IPath toPath(File file) throws Exception {
        IPath result = null;

        File absolute = file.getCanonicalFile();

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] candidates = wsroot.findFilesForLocationURI(absolute.toURI());
        if (candidates != null && candidates.length > 0) {
            result = candidates[0].getFullPath();
        } else {
            String workspacePath = getWorkspace().getBase().getAbsolutePath();
            String absolutePath = absolute.getPath();
            if (absolutePath.startsWith(workspacePath))
                result = new Path(absolutePath.substring(workspacePath.length()));
        }

        return result;
    }

    public static void refresh(IPath path) {
        try {
            IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
            if (r != null)
                return;

            IPath p = (IPath) path.clone();
            while (p.segmentCount() > 0) {
                p = p.removeLastSegments(1);
                IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(p);
                if (resource != null) {
                    resource.refreshLocal(2, null);
                    return;
                }
            }
        } catch (Exception e) {
            logger.logError("While refreshing path " + path, e);
        }
    }

    public static void refreshPlugins() throws Exception {
        List<Refreshable> rps = getWorkspace().getPlugins(Refreshable.class);
        for (Refreshable rp : rps) {
            if (rp.refresh()) {
                File dir = rp.getRoot();
                refreshFile(dir);
            }
        }
    }

    public static void refreshFile(File f) throws Exception {
        String path = toLocal(f);
        IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if (r != null) {
            r.refreshLocal(IResource.DEPTH_INFINITE, null);
        }
    }

    public static void refresh(Project p) throws Exception {
        IJavaProject jp = getJavaProject(p);
        if (jp != null)
            jp.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    private static String toLocal(File f) throws Exception {
        String root = getWorkspace().getBase().getAbsolutePath();
        String path = f.getAbsolutePath().substring(root.length());
        return path;
    }

    public void close() {}

    public static void invalidateIndex() {
        indexValid.set(false);
    }

    public static boolean needsIndexing() {
        return indexValid.compareAndSet(false, true);
    }

    public static Map<String,SortedSet<Version>> getExportedPackageModel(IProject project) {
        String key = project.getFullPath().toPortableString();
        return exportedPackageMap.get(key);
    }

    public static Collection<String> getContainedPackageModel(IProject project) {
        String key = project.getFullPath().toPortableString();
        return containedPackageMap.get(key);
    }

    public static Collection<IResource> getSourceFolderModel(IProject project) {
        String key = project.getFullPath().toPortableString();
        return sourceFolderMap.get(key);
    }

    public static void setProjectPackageModel(IProject project, Map<String,SortedSet<Version>> exports, Collection<String> contained, Collection<IResource> sourceFolders) {
        String key = project.getFullPath().toPortableString();
        exportedPackageMap.put(key, exports);
        containedPackageMap.put(key, contained);
        if (sourceFolders == null) {
            sourceFolderMap.remove(key);
        } else {
            sourceFolderMap.put(key, sourceFolders);
        }
    }

    public static Project getProject(File projectDir) throws Exception {
        File projectDirAbsolute = projectDir.getAbsoluteFile();
        assert projectDirAbsolute.isDirectory();

        Workspace ws = getWorkspace();
        return ws.getProject(projectDir.getName());
    }
}
