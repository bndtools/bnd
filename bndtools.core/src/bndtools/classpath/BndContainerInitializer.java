package bndtools.classpath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Constants;

import aQute.bnd.build.CircularDependencyException;
import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Parameters;
import bndtools.Central;
import bndtools.Logger;
import bndtools.ModelListener;
import bndtools.Plugin;
import bndtools.RefreshFileJob;
import bndtools.api.ILogger;
import bndtools.utils.JarUtils;

/**
 * A bnd container reads the bnd.bnd file in the project directory and use the information in there to establish the
 * classpath. The classpath is defined by the -build-env instruction. This instruction contains a list of bsn's that are
 * searched in the available repositories and returned as File objects. This initializer establishes the link between
 * the container object and the BndModel. The container object is just a delegator because for some unknown reasons, you
 * can only update the container (refresh the contents) when you give it a new object ;-( Because this plugin uses the
 * Bnd Builder in different places, the Bnd Model is centralized and available from the Activator.
 */
public class BndContainerInitializer extends ClasspathContainerInitializer implements ModelListener {
    private static final ILogger logger = Logger.getLogger();

    public static final Path PATH_ID = new Path("aQute.bnd.classpath.container");
    public static final String MARKER_BND_CLASSPATH_PROBLEM = Plugin.PLUGIN_ID + ".bnd_classpath_problem";

    static final IClasspathEntry[] EMPTY_ENTRIES = new IClasspathEntry[0];

    final Central central = Plugin.getDefault().getCentral();

    public BndContainerInitializer() {
        central.addModelListener(this);
    }

    @Override
    public void initialize(IPath containerPath, final IJavaProject project) throws CoreException {
        final ArrayList<String> errors = new ArrayList<String>();
        calculateAndUpdateClasspathEntries(project, errors);

        WorkspaceJob replaceMarkersJob = new WorkspaceJob("Update bnd classpath markers") {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                replaceClasspathProblemMarkers(project.getProject(), errors);
                return Status.OK_STATUS;
            }
        };
        replaceMarkersJob.setRule(project.getProject());
        replaceMarkersJob.schedule();
    }

    @Override
    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return true;
    }

    @Override
    // The suggested classpath container is ignored here; always recalculated
    // from the project.
    public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
        BndContainerSourceManager.saveAttachedSources(project, Arrays.asList(containerSuggestion.getClasspathEntries()));

        ArrayList<String> errors = new ArrayList<String>();
        calculateAndUpdateClasspathEntries(project, errors);
        replaceClasspathProblemMarkers(project.getProject(), errors);
    }

    public static boolean resetClasspaths(Project model, IProject project, Collection<String> errors) throws CoreException {
        IJavaProject javaProject = JavaCore.create(project);
        return resetClasspaths(model, javaProject, errors);
    }

    public static boolean resetClasspaths(Project model, IJavaProject javaProject, Collection<String> errors) throws CoreException {
        IClasspathContainer container = JavaCore.getClasspathContainer(BndContainerInitializer.PATH_ID, javaProject);
        List<IClasspathEntry> currentClasspath = Arrays.asList(container.getClasspathEntries());

        List<IClasspathEntry> newClasspath = BndContainerInitializer.calculateProjectClasspath(model, javaProject, errors);
        newClasspath = BndContainerSourceManager.loadAttachedSources(javaProject, newClasspath);

        replaceClasspathProblemMarkers(javaProject.getProject(), errors);

        if (!newClasspath.equals(currentClasspath)) {
            BndContainerInitializer.setClasspathEntries(javaProject, newClasspath.toArray(new IClasspathEntry[newClasspath.size()]));
            return true;
        }
        return false;
    }

    private static void calculateAndUpdateClasspathEntries(IJavaProject project, Collection< ? super String> errors) throws CoreException {
        IClasspathEntry[] entries = new IClasspathEntry[0];
        Project model;
        try {
            model = Workspace.getProject(project.getProject().getLocation().toFile());
        } catch (Exception e) {
            // Abort quickly if there is no Bnd workspace
            setClasspathEntries(project, entries);
            return;
        }

        try {
            if (model != null) {
                List<IClasspathEntry> classpath = calculateProjectClasspath(model, project, errors);
                if (classpath != null) {
                    classpath = BndContainerSourceManager.loadAttachedSources(project, classpath);
                    entries = classpath.toArray(new IClasspathEntry[classpath.size()]);
                }
            }
            setClasspathEntries(project, entries);
        } catch (Exception e) {
            logger.logError("Error requesting bnd classpath update.", e);
        }
    }

    public void modelChanged(Project model) throws Exception {
        IJavaProject project = Central.getJavaProject(model);
        if (model == null || project == null) {
            System.out.println("Help! No IJavaProject for " + model);
        } else {
            requestClasspathContainerUpdate(PATH_ID, project, null);
        }
    }

    @SuppressWarnings("unused")
    public static void workspaceChanged(Workspace ws) throws Exception {
        System.out.println("Workspace changed");
    }

    public static void setClasspathEntries(IJavaProject javaProject, IClasspathEntry[] entries) throws JavaModelException {
        JavaCore.setClasspathContainer(BndContainerInitializer.PATH_ID, new IJavaProject[] {
            javaProject
        }, new IClasspathContainer[] {
            new BndContainer(javaProject, entries, null)
        }, null);
    }

    public static List<IClasspathEntry> calculateProjectClasspath(Project model, IJavaProject javaProject, Collection< ? super String> errors) throws CoreException {
        IProject project = javaProject.getProject();
        if (!project.exists() || !project.isOpen())
            return null;

        if (model == null) {
            setClasspathEntries(javaProject, EMPTY_ENTRIES);
            errors.add("bnd workspace is not configured.");
            return null;
        }

        model.clear();

        Collection<Container> buildPath;
        Collection<Container> bootClasspath;
        List<Container> containers;

        try {
            buildPath = model.getBuildpath();
            bootClasspath = model.getBootclasspath();

            containers = new ArrayList<Container>(buildPath.size() + bootClasspath.size());
            containers.addAll(buildPath);

            // The first file is always the project directory,
            // Eclipse already includes that for us.
            if (containers.size() > 0) {
                containers.remove(0);
            }
            containers.addAll(bootClasspath);
        } catch (CircularDependencyException e) {
            errors.add("Circular dependency: " + e.getMessage());
            containers = Collections.emptyList();
        } catch (Exception e) {
            errors.add("Unexpected error during classpath calculation: " + e);
            containers = Collections.emptyList();
        }

        ArrayList<IClasspathEntry> result = new ArrayList<IClasspathEntry>(containers.size());
        LinkedHashMap<Project,List<IAccessRule>> projectAccessRules = new LinkedHashMap<Project,List<IAccessRule>>();
        for (Container c : containers) {
            if (c.getType() == TYPE.PROJECT && c.getError() == null) {
                calculateWorkspaceBundleAccessRules(projectAccessRules, c);
            }
        }

        for (Container c : containers) {
            IClasspathEntry cpe;

            if (c.getError() == null) {
                File file = c.getFile();
                assert file.isAbsolute();

                if (!file.exists()) {
                    switch (c.getType()) {
                    case REPO :
                        errors.add("Repository file " + c.getFile() + " does not exist");
                        break;
                    case LIBRARY :
                        errors.add("Library file " + c.getFile() + " does not exist");
                        break;
                    case PROJECT :
                        errors.add("Project bundle " + c.getFile() + " does not exist");
                        break;
                    case EXTERNAL :
                        errors.add("External file " + c.getFile() + " does not exist");
                        break;
                    default :
                        break;
                    }
                }

                IPath p = null;
                try {
                    p = fileToPath(file);
                } catch (Exception e) {
                    errors.add(String.format("Failed to convert file %s to Eclipse path", file));
                }
                if (p != null) {
                    if (c.getType() == Container.TYPE.PROJECT) {
                        IResource resource = ResourcesPlugin.getWorkspace().getRoot().getFile(p);
                        List<IAccessRule> rules = projectAccessRules.get(c.getProject());
                        IAccessRule[] accessRules = null;
                        if (rules != null) {
                            rules.add(JavaCore.newAccessRule(new Path("**"), IAccessRule.K_NON_ACCESSIBLE));
                            accessRules = rules.toArray(new IAccessRule[rules.size()]);
                        }
                        cpe = JavaCore.newProjectEntry(resource.getProject().getFullPath(), accessRules, false, null, true);
                    } else {
                        IAccessRule[] accessRules = calculateRepoBundleAccessRules(c);
                        cpe = JavaCore.newLibraryEntry(p, null, null, accessRules, null, false);
                    }
                    result.add(cpe);
                }
            } else {
                errors.add(c.getError());
            }
        }

        errors.addAll(model.getErrors());

        return result;
    }

    // TODO: this is a workaround for bug #89 in bnd
    static boolean isProjectContainer(Container container) {
        return container.getType() == TYPE.PROJECT && !container.getFile().isFile();
    }

    static IAccessRule[] calculateRepoBundleAccessRules(Container c) {
        String packageList = c.getAttributes().get("packages");
        if (packageList != null) {
            List<IAccessRule> tmp = new LinkedList<IAccessRule>();
            StringTokenizer tokenizer = new StringTokenizer(packageList, ",");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                String pathStr = token.replace('.', '/') + "/*";
                tmp.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
            }
            tmp.add(JavaCore.newAccessRule(new Path("**"), IAccessRule.K_NON_ACCESSIBLE));
            return tmp.toArray(new IAccessRule[tmp.size()]);
        }
        return null;
    }

    static void calculateWorkspaceBundleAccessRules(Map<Project,List<IAccessRule>> projectAccessRules, Container c) {
        String packageList = c.getAttributes().get("packages");
        if (packageList != null) {
            List<IAccessRule> tmp = new LinkedList<IAccessRule>();
            StringTokenizer tokenizer = new StringTokenizer(packageList, ",");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                String pathStr = token.replace('.', '/') + "/*";
                tmp.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
            }
            addAccessRules(projectAccessRules, c.getProject(), tmp);
        } else if (isProjectContainer(c)) {
            // No access rules please.
            addAccessRules(projectAccessRules, c.getProject(), null);
        } else if (c.getType() == TYPE.PROJECT) {
            Manifest mf = null;
            try {
                mf = JarUtils.loadJarManifest(new FileInputStream(c.getFile()));
            } catch (IOException e) {
                logger.logError("Unable to generate access rules from bundle " + c.getFile(), e);
                return;
            }
            Parameters exportPkgs = new Parameters(mf.getMainAttributes().getValue(new Name(Constants.EXPORT_PACKAGE)));
            List<IAccessRule> tmp = new LinkedList<IAccessRule>();
            for (String exportPkg : exportPkgs.keySet()) {
                String pathStr = exportPkg.replace('.', '/') + "/*";
                tmp.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
            }
            addAccessRules(projectAccessRules, c.getProject(), tmp);
        }
    }

    static void addAccessRules(Map<Project,List<IAccessRule>> projectAccessRules, Project project, List<IAccessRule> accessRules) {
        if (projectAccessRules.containsKey(project)) {
            List<IAccessRule> currentAccessRules = projectAccessRules.get(project);

            if (currentAccessRules != null) {
                if (accessRules == null)
                    projectAccessRules.put(project, null);
                else
                    currentAccessRules.addAll(accessRules);
            }
        } else {
            projectAccessRules.put(project, accessRules);
        }
    }

    /*
     * static void replaceClasspathProblemMarkers(final IProject project, final Collection<String> errors, boolean
     * submitMarkerJob) throws CoreException{ final IFile bndFile = project.getFile(Project.BNDFILE); if (bndFile ==
     * null) { return; } if (submitMarkerJob) { Job markerJob = new Job("Bndtools: Resolving markers") {
     * @Override protected IStatus run(IProgressMonitor monitor) { try { replaceMarkersJob(bndFile, project, errors); }
     * catch (CoreException e) { return e.getStatus(); } return Status.OK_STATUS; } }; markerJob.setRule(project);
     * markerJob.setRule(bndFile); markerJob.setPriority(Job.BUILD); markerJob.schedule(); } else {
     * replaceMarkersJob(bndFile, project, errors); } }
     */

    public static void replaceClasspathProblemMarkers(IProject project, Collection<String> errors) throws CoreException {
        assert project != null;

        if (!project.exists() || !project.isOpen()) {
            logger.logError(String.format("Cannot replace bnd classpath problem markers: project %s is not in the Eclipse workspace or is not open.", project.getName()), null);
            return;
        }

        IResource resource = project.getFile(Project.BNDFILE);
        if (resource == null || !resource.exists())
            resource = project;

        project.deleteMarkers(MARKER_BND_CLASSPATH_PROBLEM, true, IResource.DEPTH_INFINITE);
        for (String error : errors) {
            IMarker marker = resource.createMarker(MARKER_BND_CLASSPATH_PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, error);
        }
    }

    protected static IPath fileToPath(File file) throws Exception {
        IPath path = Central.toPath(file);
        if (path == null)
            path = Path.fromOSString(file.getAbsolutePath());

        RefreshFileJob refreshJob = new RefreshFileJob(file, false);
        if (refreshJob.needsToSchedule())
            refreshJob.schedule(100);

        return path;
    }
}
