package org.bndtools.builder.classpath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ModelListener;
import org.bndtools.builder.BuildLogger;
import org.bndtools.builder.BuilderPlugin;
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
import org.eclipse.jdt.internal.core.JavaModelManager;

import aQute.bnd.build.CircularDependencyException;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter.SetLocation;
import bndtools.central.Central;
import bndtools.central.RefreshFileJob;
import bndtools.preferences.BndPreferences;

/**
 * ClasspathContainerInitializer for the aQute.bnd.classpath.container name.
 * <p>
 * Used in the .classpath file of bnd project to couple the bnd -buildpath into the Eclipse IDE.
 */
public class BndContainerInitializer extends ClasspathContainerInitializer implements ModelListener {
    static final ILogger logger = Logger.getLogger(BndContainerInitializer.class);
    static final IClasspathEntry[] EMPTY_ENTRIES = new IClasspathEntry[0];
    static final IAccessRule DISCOURAGED = JavaCore.newAccessRule(new Path("**"), IAccessRule.K_DISCOURAGED);
    static final Pattern packagePattern = Pattern.compile("(?<=^|\\.)\\*(?=\\.|$)|\\.");

    public BndContainerInitializer() {
        super();
        Central.getInstance().addModelListener(this);
    }

    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        IProject project = javaProject.getProject();

        final Updater updater = new Updater(project, javaProject);
        updater.updateClasspathContainer(true);

        WorkspaceJob replaceMarkersJob = new WorkspaceJob("Update bnd classpath markers") {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                updater.replaceClasspathProblemMarkers();
                return Status.OK_STATUS;
            }
        };

        replaceMarkersJob.setRule(project);
        replaceMarkersJob.schedule();
    }

    @Override
    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject javaProject) {
        return true;
    }

    @Override
    public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject javaProject, IClasspathContainer containerSuggestion) throws CoreException {
        IProject project = javaProject.getProject();
        if (containerSuggestion != null) {
            BndContainerSourceManager.saveAttachedSources(project, containerSuggestion.getClasspathEntries());
        }

        Updater updater = new Updater(project, javaProject);
        updater.updateClasspathContainer(false);
        updater.replaceClasspathProblemMarkers();
    }

    @Override
    public String getDescription(IPath containerPath, IJavaProject project) {
        return "Bnd Bundle Path";
    }

    /**
     * ModelListener modelChanged method.
     */
    @Override
    public void modelChanged(Project model) throws Exception {
        IJavaProject javaProject = Central.getJavaProject(model);
        if (javaProject == null) {
            return; // bnd project is not loaded in the workspace
        }
        requestClasspathContainerUpdate(javaProject);
    }

    /**
     * Return the BndContainer for the project, if there is one. This will not create one if there is not already one.
     *
     * @param javaProject
     *            The java project of interest. Must not be null.
     * @return The BndContainer for the java project.
     */
    public static IClasspathContainer getClasspathContainer(IJavaProject javaProject) {
        return JavaModelManager.getJavaModelManager().containerGet(javaProject, BndtoolsConstants.BND_CLASSPATH_ID);
    }

    /**
     * Request the BndContainer for the project, if there is one, be updated. This will not create one if there is not
     * already one.
     *
     * @param javaProject
     *            The java project of interest. Must not be null.
     * @throws CoreException
     */
    public static void requestClasspathContainerUpdate(IJavaProject javaProject) throws CoreException {
        if (getClasspathContainer(javaProject) == null) {
            return; // project does not have a BndContainer
        }
        ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(BndtoolsConstants.BND_CLASSPATH_ID.segment(0));
        if (initializer != null) {
            initializer.requestClasspathContainerUpdate(BndtoolsConstants.BND_CLASSPATH_ID, javaProject, null);
        }
    }

    /**
     * Returns true if the specified project has bnd classpath problem markers.
     *
     * @param javaProject
     *            The java project of interest. Must not be null.
     * @return true if the specified project has bnd classpath problem markers. false otherwise.
     * @throws CoreException
     */
    public static boolean hasClasspathProblemMarkers(IJavaProject javaProject) throws CoreException {
        IMarker[] markers = javaProject.getProject().findMarkers(BndtoolsConstants.MARKER_BND_CLASSPATH_PROBLEM, true, IResource.DEPTH_INFINITE);
        return markers.length > 0; // true if classpath problems
    }

    private class Updater {
        private final IProject project;
        private final IJavaProject javaProject;
        private final List<String> errors;
        private final Project model;
        private boolean updated = false;

        Updater(IProject project, IJavaProject javaProject) {
            assert project != null;
            assert javaProject != null;
            this.project = project;
            this.javaProject = javaProject;
            this.errors = new ArrayList<String>();
            Project p = null;
            try {
                p = Central.getProject(project.getLocation().toFile());
            } catch (Exception e) {
                logger.logError("Unable to get bnd project for project " + project.getName(), e);
            }
            this.model = p;
        }

        void updateClasspathContainer(boolean init) throws CoreException {
            if (model == null) {
                errors.add("Classpath container set to empty. Unable to get bnd project for project " + project.getName());
                setClasspathEntries(EMPTY_ENTRIES);
                return;
            }

            List<IClasspathEntry> newClasspath = calculateProjectClasspath();
            newClasspath = BndContainerSourceManager.loadAttachedSources(project, newClasspath);
            if (init) {
                setClasspathEntries(newClasspath.toArray(new IClasspathEntry[newClasspath.size()]));
                return;
            }

            IClasspathContainer container = JavaCore.getClasspathContainer(BndtoolsConstants.BND_CLASSPATH_ID, javaProject);
            List<IClasspathEntry> currentClasspath = Arrays.asList(container.getClasspathEntries());
            if (newClasspath.equals(currentClasspath)) {
                return;
            }
            setClasspathEntries(newClasspath.toArray(new IClasspathEntry[newClasspath.size()]));
        }

        private void setClasspathEntries(IClasspathEntry[] entries) throws JavaModelException {
            JavaCore.setClasspathContainer(BndtoolsConstants.BND_CLASSPATH_ID, new IJavaProject[] {
                javaProject
            }, new IClasspathContainer[] {
                new BndContainer(entries, getDescription(BndtoolsConstants.BND_CLASSPATH_ID, javaProject))
            }, null);
            updated = true;

            BndPreferences prefs = new BndPreferences();
            if (prefs.getBuildLogging() == BuildLogger.LOG_FULL) {
                StringBuilder sb = new StringBuilder();
                sb.append("ClasspathEntries ").append(model.getName());
                for (IClasspathEntry cpe : entries) {
                    sb.append("\n--- ").append(cpe);
                }
                logger.logInfo(sb.append("\n").toString(), null);
            }
        }

        private List<IClasspathEntry> calculateProjectClasspath() {
            final IWorkspaceRoot root = project.getWorkspace().getRoot();
            if (!project.exists() || !project.isOpen())
                return Collections.emptyList();

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
            List<File> filesToRefresh = new ArrayList<File>(containers.size());
            for (Container c : containers) {
                if (c.getError() != null) {
                    SetLocation location = model.error("%s-%s: %s", c.getBundleSymbolicName(), c.getVersion(), c.getError());
                    location.context(c.getBundleSymbolicName());
                    location.header(Constants.BUILDPATH);
                    location.file(model.getPropertiesFile().getAbsolutePath());
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

                IPath path = null;
                try {
                    path = fileToPath(file);
                    filesToRefresh.add(file);
                } catch (Exception e) {
                    errors.add(String.format("Failed to convert file %s to Eclipse path: %s: %s", file, e.getClass().getName(), e.getMessage()));
                }
                if (path == null) {
                    continue;
                }

                IClasspathAttribute[] extraAttrs = calculateContainerAttributes(c);
                IAccessRule[] accessRules = toAccessRulesArray(calculateContainerAccessRules(c));
                switch (c.getType()) {
                case PROJECT :
                    IPath projectPath = root.getFile(path).getProject().getFullPath();
                    result.add(JavaCore.newProjectEntry(projectPath, accessRules, false, extraAttrs, false));
                    if (!isVersionProject(c)) { // if not version=project, add entry for generated jar
                        result.add(JavaCore.newLibraryEntry(path, path, null, accessRules, extraAttrs, false));
                    }
                    break;
                default :
                    result.add(JavaCore.newLibraryEntry(path, path, null, accessRules, extraAttrs, false));
                    break;
                }
            }

            // Refresh once, instead of for each dependent project.
            RefreshFileJob refreshJob = new RefreshFileJob(filesToRefresh, false, project);
            if (refreshJob.needsToSchedule())
                refreshJob.schedule(100);

            errors.addAll(model.getErrors());

            return result;
        }

        private IClasspathAttribute[] calculateContainerAttributes(Container c) {
            List<IClasspathAttribute> attrs = new ArrayList<IClasspathAttribute>();
            attrs.add(JavaCore.newClasspathAttribute("bsn", c.getBundleSymbolicName()));
            attrs.add(JavaCore.newClasspathAttribute("type", c.getType().name()));
            attrs.add(JavaCore.newClasspathAttribute("project", c.getProject().getName()));

            String version = c.getAttributes().get(Constants.VERSION_ATTRIBUTE);
            if (version != null) {
                attrs.add(JavaCore.newClasspathAttribute(Constants.VERSION_ATTRIBUTE, version));
            }

            String packages = c.getAttributes().get("packages");
            if (packages != null) {
                attrs.add(JavaCore.newClasspathAttribute("packages", packages));
            }

            return attrs.toArray(new IClasspathAttribute[attrs.size()]);
        }

        private List<IAccessRule> calculateContainerAccessRules(Container c) {
            String packageList = c.getAttributes().get("packages");
            if (packageList != null) {
                // Use packages=* for full access
                List<IAccessRule> accessRules = new ArrayList<IAccessRule>();
                for (String exportPkg : packageList.trim().split("\\s*,\\s*")) {
                    Matcher m = packagePattern.matcher(exportPkg);
                    StringBuffer pathStr = new StringBuffer(exportPkg.length() + 1);
                    while (m.find()) {
                        m.appendReplacement(pathStr, m.group().equals("*") ? "**" : "/");
                    }
                    m.appendTail(pathStr).append("/*");
                    accessRules.add(JavaCore.newAccessRule(new Path(pathStr.toString()), IAccessRule.K_ACCESSIBLE));
                }
                return accessRules;
            }

            switch (c.getType()) {
            case PROJECT :
                if (isVersionProject(c)) { // if version=project, try Project for exports
                    return calculateProjectAccessRules(c.getProject());
                }
                //$FALL-THROUGH$
            case REPO :
            case EXTERNAL :
                Manifest mf = null;
                try {
                    mf = JarUtils.loadJarManifest(c.getFile());
                } catch (IOException e) {
                    break; // unable to open manifest; so full access
                }
                if (mf == null) {
                    break; // no manifest; so full access
                }
                if (mf.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION) == null) {
                    break; // not a bundle; so full access
                }
                List<IAccessRule> accessRules = new ArrayList<IAccessRule>();
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

        private List<IAccessRule> calculateProjectAccessRules(Project p) {
            File accessPatternsFile = new File(BuilderPlugin.getInstance().getStateLocation().toFile(), p.getName() + ".accesspatterns");
            String oldAccessPatterns = "";
            boolean exists = accessPatternsFile.exists();
            if (exists) { // read persisted access patterns
                try {
                    oldAccessPatterns = IO.collect(accessPatternsFile);
                } catch (final IOException e) {
                    logger.logError("Failed to read access patterns file for project " + p.getName(), e);
                }
            }

            if (p.getContained().isEmpty()) { // project not recently built; use persisted access patterns
                if (!exists) {
                    return null; // no persisted access patterns; full access
                }
                String[] patterns = oldAccessPatterns.split(",");
                List<IAccessRule> accessRules = new ArrayList<IAccessRule>(patterns.length);
                for (String pathStr : patterns) {
                    accessRules.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
                }
                return accessRules;
            }

            Set<PackageRef> exportPkgs = p.getExports().keySet();
            List<IAccessRule> accessRules = new ArrayList<IAccessRule>(exportPkgs.size());
            StringBuilder sb = new StringBuilder(oldAccessPatterns.length());
            for (PackageRef exportPkg : exportPkgs) {
                String pathStr = exportPkg.getBinary() + "/*";
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(pathStr);
                accessRules.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
            }

            String newAccessPatterns = sb.toString();
            if (!exists || !newAccessPatterns.equals(oldAccessPatterns)) { // if state changed; persist updated access patterns
                try {
                    IO.store(newAccessPatterns, accessPatternsFile);
                } catch (final IOException e) {
                    logger.logError("Failed to write access patterns file for project " + p.getName(), e);
                }
            }

            return accessRules;
        }

        private IAccessRule[] toAccessRulesArray(List<IAccessRule> rules) {
            if (rules == null) {
                return null;
            }
            final int size = rules.size();
            IAccessRule[] accessRules = rules.toArray(new IAccessRule[size + 1]);
            accessRules[size] = DISCOURAGED;
            return accessRules;
        }

        void replaceClasspathProblemMarkers() throws CoreException {
            if (!project.exists() || !project.isOpen()) {
                logger.logError(String.format("Cannot replace bnd classpath problem markers: project %s is not in the Eclipse workspace or is not open.", project.getName()), null);
                return;
            }

            if (!updated && errors.isEmpty()) {
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

        private IPath fileToPath(File file) throws Exception {
            IPath path = Central.toPath(file);
            if (path == null)
                path = Path.fromOSString(file.getAbsolutePath());
            return path;
        }

        private boolean isVersionProject(Container c) {
            return Constants.VERSION_ATTR_PROJECT.equals(c.getAttributes().get(Constants.VERSION_ATTRIBUTE));
        }
    }
}
