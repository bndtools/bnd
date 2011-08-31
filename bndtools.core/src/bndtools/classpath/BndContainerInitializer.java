package bndtools.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
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

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.Central;
import bndtools.ModelListener;
import bndtools.Plugin;

/**
 * A bnd container reads the bnd.bnd file in the project directory and use the
 * information in there to establish the classpath. The classpath is defined by
 * the -build-env instruction. This instruction contains a list of bsn's that
 * are searched in the available repositories and returned as File objects.
 *
 * This initializer establishes the link between the container object and the
 * BndModel. The container object is just a delegator because for some unknown
 * reasons, you can only update the container (refresh the contents) when you
 * give it a new object ;-(
 *
 * Because this plugin uses the Bnd Builder in different places, the Bnd Model
 * is centralized and available from the Activator.
 */
public class BndContainerInitializer extends ClasspathContainerInitializer
        implements ModelListener {

    public static final Path PATH_ID = new Path("aQute.bnd.classpath.container");
    public static final String MARKER_BND_CLASSPATH_PROBLEM = Plugin.PLUGIN_ID + ".bnd_classpath_problem";

    static final IClasspathEntry[] EMPTY_ENTRIES = new IClasspathEntry[0];

    final Central central = Plugin.getDefault().getCentral();

    public BndContainerInitializer() {
        central.addModelListener(this);
    }

    @Override
    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        requestClasspathContainerUpdate(containerPath, project, null);
    }

    @Override
    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return true;
    }

    @Override
    // The suggested classpath container is ignored here; always recalculated from the project.
    public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
        updateProjectClasspath(project);
    }

    public void modelChanged(Project model) throws Exception {
        IJavaProject project = central.getJavaProject(model);
        if (model == null || project == null) {
            System.out.println("Help! No IJavaProject for " + model);
        } else {
            requestClasspathContainerUpdate(PATH_ID, project, null);
        }
    }

    public void workspaceChanged(Workspace ws) throws Exception {
        System.out.println("Workspace changed");
    }

    static void setClasspathEntries(IJavaProject javaProject, Project model, IClasspathEntry[] entries) throws JavaModelException {
        JavaCore.setClasspathContainer(BndContainerInitializer.PATH_ID, new IJavaProject[] { javaProject }, new IClasspathContainer[] { new BndContainer(javaProject, entries, null) }, null);
    }

    public static void updateProjectClasspath(IJavaProject javaProject) throws CoreException {
        IProject project = javaProject.getProject();
        if (!project.exists() || !project.isOpen())
            return;

        Project model = Plugin.getDefault().getCentral().getModel(javaProject);
        List<String> errors = new LinkedList<String>();
        if (model == null) {
            setClasspathEntries(javaProject, model, EMPTY_ENTRIES);
            errors.add("Bnd workspace is not configured.");
        } else {
            model.clear();
            model.refresh();
            model.setChanged();

            Collection<Container> buildPath = getProjectBuildPath(model);
            Collection<Container> bootClasspath = getProjectBootClasspath(model);

            List<Container> containers = new ArrayList<Container>(buildPath.size() + bootClasspath.size());
            containers.addAll(buildPath);

            // The first file is always the project directory,
            // Eclipse already includes that for us.
            if (containers.size() > 0) {
                containers.remove(0);
            }
            containers.addAll(bootClasspath);

            ArrayList<IClasspathEntry> result = new ArrayList<IClasspathEntry>(containers.size());
            for (Container c : containers) {
                IClasspathEntry cpe;
                IPath sourceAttachment = null;

                if (c.getError() == null) {
                    File file = c.getFile();
                    assert file.isAbsolute();

                    IPath p = fileToPath(model, file);
                    if (c.getType() == Container.TYPE.PROJECT) {
                        File sourceDir = c.getProject().getSrc();
                        if (sourceDir.isDirectory())
                            sourceAttachment = Central.toPath(c.getProject(), sourceDir);
    //                } else {
    //                    File sourceBundle = c.getSourceBundle();
    //                    if (sourceBundle != null && sourceBundle.isAbsolute()) {
    //                        sourceAttachment = fileToPath(project, sourceBundle);
    //                    }
                    }

                    IAccessRule[] accessRules = calculateAccessRules(c);
                    cpe = JavaCore.newLibraryEntry(p, sourceAttachment, null, accessRules, null, false);
                    result.add(cpe);
                } else {
                    errors.add(c.getError());
                }
            }
            errors.addAll(model.getErrors());
            replaceClasspathProblemMarkers(project, errors);
            model.clear();

            setClasspathEntries(javaProject, model, result.toArray(new IClasspathEntry[result.size()]));
        }
    }

    static IAccessRule[] calculateAccessRules(Container c) {
        IAccessRule[] accessRules;
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
            accessRules = tmp.toArray(new IAccessRule[tmp.size()]);
        } else {
            accessRules = null;
        }
        return accessRules;
    }

    static void replaceClasspathProblemMarkers(final IProject project, final Collection<String> errors) {
        try {
            project.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException {
                    project.deleteMarkers(MARKER_BND_CLASSPATH_PROBLEM, true, 1);
                    for (String error : errors) {
                        IMarker marker = project.createMarker(MARKER_BND_CLASSPATH_PROBLEM);
                        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                        marker.setAttribute(IMarker.MESSAGE, error);
                    }
                }
            }, null);
        } catch (CoreException e) {
            Plugin.logError("Error replacing project classpath problem markers.", e);
        }
    }

    static Collection<Container> getProjectBootClasspath(Project model) {
        Collection<Container> bootclasspath;
        try {
            bootclasspath = model.getBootclasspath();
        } catch (Exception e) {
            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting project boot classpath.", e));
            bootclasspath = Collections.emptyList();
        }
        return bootclasspath;
    }

    static Collection<Container> getProjectBuildPath(Project model) {
        Collection<Container> buildpath;
        try {
            buildpath = model.getBuildpath();
        } catch (Exception e) {
            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting project build path.", e));
            buildpath = Collections.emptyList();
        }
        return buildpath;
    }

    protected static IPath fileToPath(Project project, File file) {
        IPath path = Central.toPath(project, file);
        if (path == null)
            path = Path.fromOSString(file.getAbsolutePath());

        try {
            Central.refresh(path);
        } catch (Throwable e) {
        }

        return path;
    }
}