package bndtools.classpath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
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
import aQute.libg.header.OSGiHeader;
import bndtools.Central;
import bndtools.ModelListener;
import bndtools.Plugin;
import bndtools.utils.JarUtils;

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
        // Silently fail as described in javadoc
        updateProjectClasspath(project, false);
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
        updateProjectClasspath(javaProject, true);
    }
    public static void updateProjectClasspath(IJavaProject javaProject, boolean updateMarkers) throws CoreException {
        IProject project = javaProject.getProject();
        if (!project.exists() || !project.isOpen())
            return;

        // Remove classpath problem markers
        if (updateMarkers) {
            project.deleteMarkers(MARKER_BND_CLASSPATH_PROBLEM, true, 0);
        }

        Project model = Plugin.getDefault().getCentral().getModel(javaProject);
        List<String> errors = new LinkedList<String>();

        if (model == null) {
            setClasspathEntries(javaProject, model, EMPTY_ENTRIES);
            if (updateMarkers) {
                errors.add("Bnd workspace is not configured.");
            }
        } else {
            model.clear();
            model.refresh();
            model.setChanged();

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
            LinkedHashMap<Project, List<IAccessRule>> projectAccessRules = new LinkedHashMap<Project, List<IAccessRule>>();
            for (Container c : containers) {
                if (c.getError() == null) {
                    File file = c.getFile();
                    assert file.isAbsolute();
                    calculateAccessRules(projectAccessRules, c);
                }
            }

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
                        IAccessRule[] accessRules = calculateAccessRules(projectAccessRules, c);
                        cpe = JavaCore.newLibraryEntry(p, sourceAttachment, null, accessRules, null, false);
                    }
                    result.add(cpe);
                } else {
                    errors.add(c.getError());
                }
            }


            errors.addAll(model.getErrors());
            if (updateMarkers) {
                replaceClasspathProblemMarkers(project, errors);
            }
            model.clear();

            setClasspathEntries(javaProject, model, result.toArray(new IClasspathEntry[result.size()]));
        }
    }

    static IAccessRule[] calculateAccessRules(Map<Project, List<IAccessRule>> projectAccessRules, Container c) {
        String packageList = c.getAttributes().get("packages");
        if (packageList != null) {
            List<IAccessRule> tmp = new LinkedList<IAccessRule>();
            StringTokenizer tokenizer = new StringTokenizer(packageList, ",");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                String pathStr = token.replace('.', '/') + "/*";
                tmp.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
            }
            if (c.getType() == TYPE.PROJECT) {
                addAccessRules(projectAccessRules, c.getProject(), tmp);
                return null;
            } else {
                tmp.add(JavaCore.newAccessRule(new Path("**"), IAccessRule.K_NON_ACCESSIBLE));
                return tmp.toArray(new IAccessRule[tmp.size()]);
            }
        } else if (c.getType() == TYPE.PROJECT) {
            Manifest mf = null;
            try {
                mf = JarUtils.loadJarManifest(new FileInputStream(c.getFile()));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            Map<String, Map<String, String>> exportedPackages = OSGiHeader.parseHeader(mf.getMainAttributes().getValue(new Name(Constants.EXPORT_PACKAGE)));
            List<IAccessRule> tmp = new LinkedList<IAccessRule>();
            for (String exportedPackage : exportedPackages.keySet()) {
                String pathStr = exportedPackage.replace('.', '/') + "/*";
                tmp.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
            }
            addAccessRules(projectAccessRules, c.getProject(), tmp);
        }
        return null;
    }

    static void addAccessRules(Map<Project, List<IAccessRule>> projectAccessRules, Project project, List<IAccessRule> accessRules) {
        List<IAccessRule> currentAccessRules = projectAccessRules.get(project);
        if (currentAccessRules == null) {
            projectAccessRules.put(project, accessRules);
        } else {
            currentAccessRules.addAll(accessRules);
        }
    }

    static void replaceClasspathProblemMarkers(final IProject project, final Collection<String> errors) {
        try {
            project.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException {
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
