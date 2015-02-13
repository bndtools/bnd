package org.bndtools.builder.classpath;

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

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ModelListener;
import org.bndtools.utils.jar.JarUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
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
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Descriptors.PackageRef;
import bndtools.central.Central;
import bndtools.central.RefreshFileJob;

/**
 * A bnd container reads the bnd.bnd file in the project directory and use the information in there to establish the
 * classpath. The classpath is defined by the -build-env instruction. This instruction contains a list of bsn's that are
 * searched in the available repositories and returned as File objects. This initializer establishes the link between
 * the container object and the BndModel. The container object is just a delegator because for some unknown reasons, you
 * can only update the container (refresh the contents) when you give it a new object ;-( Because this plugin uses the
 * Bnd Builder in different places, the Bnd Model is centralized and available from the Activator.
 */
public class BndContainerInitializer extends ClasspathContainerInitializer implements ModelListener {
    private static final ILogger logger = Logger.getLogger(BndContainerInitializer.class);

    private static final IClasspathEntry[] EMPTY_ENTRIES = new IClasspathEntry[0];
    private static final IAccessRule NON_ACCESSIBLE = JavaCore.newAccessRule(new Path("**"), IAccessRule.K_NON_ACCESSIBLE);
    private static final IAccessRule DISCOURAGED = JavaCore.newAccessRule(new Path("**"), IAccessRule.K_DISCOURAGED);
    private static final IAccessRule IGNORE_IF_BETTER = JavaCore.newAccessRule(new Path("**"), IAccessRule.K_NON_ACCESSIBLE | IAccessRule.IGNORE_IF_BETTER);

    public BndContainerInitializer() {
        Central.getInstance().addModelListener(this);
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
        BndContainerSourceManager.saveAttachedSources(project, (containerSuggestion == null) ? null : Arrays.asList(containerSuggestion.getClasspathEntries()));

        ArrayList<String> errors = new ArrayList<String>();
        calculateAndUpdateClasspathEntries(project, errors);
        replaceClasspathProblemMarkers(project.getProject(), errors);
    }

    public static boolean resetClasspaths(Project model, IProject project, Collection<String> errors) throws CoreException {
        IJavaProject javaProject = JavaCore.create(project);
        return resetClasspaths(model, javaProject, errors);
    }

    private static boolean resetClasspaths(Project model, IJavaProject javaProject, Collection<String> errors) throws CoreException {
        IClasspathContainer container = JavaCore.getClasspathContainer(BndtoolsConstants.BND_CLASSPATH_ID, javaProject);
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
            model = Central.getProject(project.getProject().getLocation().toFile());
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

    @Override
    public void modelChanged(Project model) throws Exception {
        IJavaProject project = Central.getJavaProject(model);
        if (model == null || project == null) {
            System.out.println("Help! No IJavaProject for " + model);
        } else {
            requestClasspathContainerUpdate(BndtoolsConstants.BND_CLASSPATH_ID, project, null);
        }
    }

    private static void setClasspathEntries(IJavaProject javaProject, IClasspathEntry[] entries) throws JavaModelException {
        JavaCore.setClasspathContainer(BndtoolsConstants.BND_CLASSPATH_ID, new IJavaProject[] {
            javaProject
        }, new IClasspathContainer[] {
            new BndContainer(javaProject, entries, null)
        }, null);
    }

    private static List<IClasspathEntry> calculateProjectClasspath(Project model, IJavaProject javaProject, Collection< ? super String> errors) throws CoreException {
        final IProject project = javaProject.getProject();
        final IWorkspaceRoot root = project.getWorkspace().getRoot();
        if (!project.exists() || !project.isOpen())
            return null;

        if (model == null) {
            setClasspathEntries(javaProject, EMPTY_ENTRIES);
            errors.add("bnd workspace is not configured.");
            return null;
        }

        model.clear();

        List<Container> containers;

        try {
            Collection<Container> buildPath = model.getBuildpath();
            Collection<Container> testPath = model.getTestpath();
            Collection<Container> bootClasspath = model.getBootclasspath();

            containers = new ArrayList<Container>(buildPath.size() + testPath.size() + bootClasspath.size());
            containers.addAll(buildPath);

            // The first file is always the project directory,
            // Eclipse already includes that for us.
            if (containers.size() > 0) {
                containers.remove(0);
            }
            containers.addAll(testPath);
            containers.addAll(bootClasspath);
        } catch (CircularDependencyException e) {
            errors.add("Circular dependency: " + e.getMessage());
            containers = Collections.emptyList();
        } catch (Exception e) {
            errors.add("Unexpected error during classpath calculation: " + e);
            containers = Collections.emptyList();
        }

        List<IClasspathEntry> result = new ArrayList<IClasspathEntry>(containers.size());
        LinkedHashMap<Project,List<IAccessRule>> projectAccessRulesExports = new LinkedHashMap<Project,List<IAccessRule>>();

        boolean newaccessrules = Boolean.parseBoolean(model.getProperty("eclipse.newaccessrules", "false"));

        if (!newaccessrules) {
            for (Container c : containers) {
                if (c.getType() == TYPE.PROJECT && c.getError() == null) {
                    calculateWorkspaceBundleAccessRules(projectAccessRulesExports, c);
                }
            }
        }

        List<File> filesToRefresh = new ArrayList<File>();

        for (Container c : containers) {
            if (c.getError() != null) {
                errors.add(c.getError());
                continue;
            }

            File file = c.getFile();
            assert file.isAbsolute();

            if (!file.exists()) {
                switch (c.getType()) {
                case REPO :
                    errors.add("Repository file " + file + " does not exist");
                    break;
                case LIBRARY :
                    errors.add("Library file " + file + " does not exist");
                    break;
                case PROJECT :
                    errors.add("Project bundle " + file + " does not exist");
                    break;
                case EXTERNAL :
                    errors.add("External file " + file + " does not exist");
                    break;
                default :
                    break;
                }
            }

            IPath p = null;
            try {
                p = fileToPath(file);
                filesToRefresh.add(file);
            } catch (Exception e) {
                errors.add(String.format("Failed to convert file %s to Eclipse path: %s: %s", file, e.getClass().getName(), e.getMessage()));
            }
            if (p != null) {
                IClasspathAttribute[] extraAttrs = calculateExtraClasspathAttrs(c);

                if (!newaccessrules) {
                    // oldaccessrules
                    IClasspathEntry cpe;
                    if (c.getType() == Container.TYPE.PROJECT) {
                        IResource resource = root.getFile(p);
                        IAccessRule[] accessRules = getProjectAccessRules(projectAccessRulesExports, c);
                        cpe = JavaCore.newProjectEntry(resource.getProject().getFullPath(), accessRules, false, extraAttrs, true);
                    } else {
                        IAccessRule[] accessRules = calculateRepoBundleAccessRules(c);
                        cpe = JavaCore.newLibraryEntry(p, null, null, accessRules, extraAttrs, false);
                    }
                    result.add(cpe);
                } else {
                    // newaccessrules
                    IAccessRule[] accessRules = toAccessRulesArray(calculateContainerAccessRules(c));
                    switch (c.getType()) {
                    case PROJECT :
                        IPath projectPath = root.getFile(p).getProject().getFullPath();
                        result.add(JavaCore.newProjectEntry(projectPath, toAccessRulesArray(IGNORE_IF_BETTER), false, extraAttrs, false));
                        result.add(JavaCore.newLibraryEntry(p, projectPath, null, accessRules, extraAttrs, false));
                        break;
                    default :
                        result.add(JavaCore.newLibraryEntry(p, null, null, accessRules, extraAttrs, false));
                        break;
                    }
                }
            }
        }

        //[cs] set project variable: "eclipse.debug: true" to enable some extra eclipse output for debugging.
        if (Boolean.parseBoolean(model.getProperty("eclipse.debug", "false"))) {
            StringBuilder sb = new StringBuilder();
            sb.append("IClasspathEntrys: project = " + model.getName() + "\n");
            for (IClasspathEntry f : result) {
                sb.append("--- " + f + "\n");
            }
            //TODO - should/could be switched to logger (if logger goes to console in eclipse)
            System.out.println(sb);
        }

        // Refresh once, instead of for each dependent project.
        RefreshFileJob refreshJob = new RefreshFileJob(filesToRefresh, false, javaProject.getProject());
        if (refreshJob.needsToSchedule())
            refreshJob.schedule(100);

        errors.addAll(model.getErrors());

        return result;
    }

    // TODO: this is a workaround for bug #89 in bnd
    private static boolean isProjectContainer(Container container) {
        return container.getType() == TYPE.PROJECT && !container.getFile().isFile();
    }

    private static IAccessRule[] getProjectAccessRules(Map<Project,List<IAccessRule>> projectAccessRules, Container c) {
        List<IAccessRule> rules = projectAccessRules.get(c.getProject());
        if (rules == null) {
            return null;
        }
        final int size = rules.size();
        IAccessRule[] accessRules = rules.toArray(new IAccessRule[size + 1]);
        accessRules[size] = NON_ACCESSIBLE;
        return accessRules;
    }

    private static IClasspathAttribute[] calculateExtraClasspathAttrs(Container c) {
        List<IClasspathAttribute> attrs = new ArrayList<IClasspathAttribute>();
        attrs.add(JavaCore.newClasspathAttribute("bsn", c.getBundleSymbolicName()));
        attrs.add(JavaCore.newClasspathAttribute("type", c.getType().name()));
        attrs.add(JavaCore.newClasspathAttribute("project", c.getProject().getName()));

        String version = c.getAttributes().get("version");
        if (version != null) {
            attrs.add(JavaCore.newClasspathAttribute("version", version));
        }

        String packages = c.getAttributes().get("packages");
        if (packages != null) {
            attrs.add(JavaCore.newClasspathAttribute("packages", packages));
        }

        return attrs.toArray(new IClasspathAttribute[attrs.size()]);
    }

    private static IAccessRule[] calculateRepoBundleAccessRules(Container c) {
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

    private static void calculateWorkspaceBundleAccessRules(Map<Project,List<IAccessRule>> projectAccessRules, Container c) {
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

    private static List<IAccessRule> calculateContainerAccessRules(Container c) {
        List<IAccessRule> accessRules = new ArrayList<IAccessRule>();

        String packageList = c.getAttributes().get("packages");
        if (packageList != null) {
            // Use packages=** for full access
            for (String exportPkg : packageList.split("\\s*,\\s*")) {
                String pathStr = exportPkg.replace('.', '/') + "/*";
                accessRules.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
            }
            return accessRules;
        }

        File file = c.getFile();
        switch (c.getType()) {
        case PROJECT :
            if (!file.isFile()) { // not a file; so version=project
                Project p = c.getProject();
                if (p.getContained().isEmpty()) {
                    break; // no builder information; so full access
                }
                for (PackageRef exportPkg : p.getExports().keySet()) {
                    String pathStr = exportPkg.getBinary() + "/*";
                    accessRules.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
                }
                return accessRules;
            }
            //$FALL-THROUGH$
        case REPO :
        case EXTERNAL :
            Manifest mf = null;
            try {
                mf = JarUtils.loadJarManifest(new FileInputStream(file));
            } catch (IOException e) {
                logger.logError("Unable to generate access rules from bundle " + file, e);
            }
            if (mf == null) {
                break; // no manifest; so full access
            }
            if (mf.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION) == null) {
                break; // not a bundle; so full access
            }
            Parameters exportPkgs = new Parameters(mf.getMainAttributes().getValue(Constants.EXPORT_PACKAGE));
            for (String exportPkg : exportPkgs.keySet()) {
                String pathStr = exportPkg.replace('.', '/') + "/*";
                accessRules.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
            }
            return accessRules;
        default :
            break;
        }

        return null; // full access
    }

    private static IAccessRule[] toAccessRulesArray(List<IAccessRule> rules) {
        if (rules == null) {
            return null;
        }
        final int size = rules.size();
        IAccessRule[] accessRules = rules.toArray(new IAccessRule[size + 1]);
        accessRules[size] = DISCOURAGED;
        return accessRules;
    }

    private static IAccessRule[] toAccessRulesArray(IAccessRule... rules) {
        return rules;
    }

    private static void addAccessRules(Map<Project,List<IAccessRule>> projectAccessRules, Project project, List<IAccessRule> accessRules) {
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

        project.deleteMarkers(BndtoolsConstants.MARKER_BND_CLASSPATH_PROBLEM, true, IResource.DEPTH_INFINITE);
        for (String error : errors) {
            IMarker marker = resource.createMarker(BndtoolsConstants.MARKER_BND_CLASSPATH_PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, error);
        }
    }

    private static IPath fileToPath(File file) throws Exception {
        IPath path = Central.toPath(file);
        if (path == null)
            path = Path.fromOSString(file.getAbsolutePath());
        return path;
    }
}
