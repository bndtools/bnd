package org.bndtools.builder.classpath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ModelListener;
import org.bndtools.builder.BuildLogger;
import org.bndtools.builder.BuilderPlugin;
import org.bndtools.utils.jar.PseudoJar;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
    static final ReentrantLock bndLock = new ReentrantLock();

    public BndContainerInitializer() {
        super();
        Central.getInstance().addModelListener(this);
    }

    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        IProject project = javaProject.getProject();

        final Updater updater = new Updater(project, javaProject);
        updater.updateClasspathContainer(true);
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

    private class Updater {
        private final IProject project;
        private final IJavaProject javaProject;
        private final IWorkspaceRoot root;
        private final Project model;

        Updater(IProject project, IJavaProject javaProject) {
            assert project != null;
            assert javaProject != null;
            this.project = project;
            this.javaProject = javaProject;
            this.root = project.getWorkspace().getRoot();

            Project p = null;
            try {
                p = Central.getProject(project.getLocation().toFile());
            } catch (Exception e) {
                logger.logError("Unable to get bnd project for project " + project.getName(), e);
            }
            this.model = p;
        }

        void updateClasspathContainer(boolean init) throws CoreException {
            if (model == null) { // this can happen during new project creation
                setClasspathEntries(EMPTY_ENTRIES);
                return;
            }

            List<IClasspathEntry> newClasspath = Collections.emptyList();
            boolean interrupted = Thread.interrupted();
            try {
                if (bndLock.tryLock(5, TimeUnit.SECONDS)) {
                    try {
                        newClasspath = calculateProjectClasspath();
                    } finally {
                        bndLock.unlock();
                    }
                } else {
                    SetLocation error = error("Unable to acquire lock to calculate classpath for project %s", project.getName());
                    logger.logError(error.location().message, null);
                }
            } catch (InterruptedException e) {
                SetLocation error = error("Unable to acquire lock to calculate classpath for project %s", e, project.getName());
                logger.logError(error.location().message, e);
                interrupted = true;
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }

            newClasspath = BndContainerSourceManager.loadAttachedSources(project, newClasspath);

            if (!init) {
                IClasspathContainer container = JavaCore.getClasspathContainer(BndtoolsConstants.BND_CLASSPATH_ID, javaProject);
                List<IClasspathEntry> currentClasspath = Arrays.asList(container.getClasspathEntries());
                if (newClasspath.equals(currentClasspath)) {
                    return; // no change; so no need to set entries
                }
            }

            setClasspathEntries(newClasspath.toArray(new IClasspathEntry[newClasspath.size()]));
        }

        private void setClasspathEntries(IClasspathEntry[] entries) throws JavaModelException {
            JavaCore.setClasspathContainer(BndtoolsConstants.BND_CLASSPATH_ID, new IJavaProject[] {
                javaProject
            }, new IClasspathContainer[] {
                new BndContainer(entries, getDescription(BndtoolsConstants.BND_CLASSPATH_ID, javaProject))
            }, null);

            BndPreferences prefs = new BndPreferences();
            if (prefs.getBuildLogging() == BuildLogger.LOG_FULL) {
                StringBuilder sb = new StringBuilder();
                sb.append("ClasspathEntries ").append(project.getName());
                for (IClasspathEntry cpe : entries) {
                    sb.append("\n--- ").append(cpe);
                }
                logger.logInfo(sb.append("\n").toString(), null);
            }
        }

        private List<IClasspathEntry> calculateProjectClasspath() {
            if (!project.exists() || !project.isOpen())
                return Collections.emptyList();

            List<IClasspathEntry> classpath = new ArrayList<IClasspathEntry>(20);
            List<File> filesToRefresh = new ArrayList<File>(20);
            try {
                Iterator<Container> containers = model.getBuildpath().iterator();
                if (containers.hasNext()) { // The first container is always the project directory; it is not part of this container.
                    containers.next();
                }
                calculateContainersClasspath(Constants.BUILDPATH, containers, classpath, filesToRefresh);

                containers = model.getTestpath().iterator();
                calculateContainersClasspath(Constants.TESTPATH, containers, classpath, filesToRefresh);

                containers = model.getBootclasspath().iterator();
                calculateContainersClasspath(Constants.BUILDPATH, containers, classpath, filesToRefresh);
            } catch (CircularDependencyException e) {
                error("Circular dependency during classpath calculation: %s", e, e.getMessage());
                return Collections.emptyList();
            } catch (Exception e) {
                error("Unexpected error during classpath calculation: %s", e, e.getMessage());
                return Collections.emptyList();
            }

            // Refresh once, instead of for each dependent project.
            RefreshFileJob refreshJob = new RefreshFileJob(filesToRefresh, false, project);
            if (refreshJob.needsToSchedule())
                refreshJob.schedule(100);

            return classpath;
        }

        private void calculateContainersClasspath(String header, Iterator<Container> containers, List<IClasspathEntry> classpath, List<File> filesToRefresh) {
            while (containers.hasNext()) {
                Container c = containers.next();
                File file = c.getFile();
                assert file.isAbsolute();

                if (!file.exists()) {
                    switch (c.getType()) {
                    case REPO :
                        error(c, header, "Repository file %s does not exist", file);
                        break;
                    case LIBRARY :
                        error(c, header, "Library file %s does not exist", file);
                        break;
                    case PROJECT :
                        error(c, header, "Project bundle %s does not exist", file);
                        break;
                    case EXTERNAL :
                        error(c, header, "External file %s does not exist", file);
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
                    error(c, header, "Failed to convert file %s to Eclipse path: %s", e, file, e.getMessage());
                }
                if (path == null) {
                    continue;
                }

                PseudoJar pseudoJar = new PseudoJar(file);
                try {
                    IClasspathAttribute[] extraAttrs = calculateContainerAttributes(c);
                    List<IAccessRule> accessRules = calculateContainerAccessRules(c, pseudoJar);

                    switch (c.getType()) {
                    case PROJECT :
                        IPath projectPath = root.getFile(path).getProject().getFullPath();
                        addProjectEntry(classpath, projectPath, accessRules, extraAttrs);
                        if (!isVersionProject(c)) { // if not version=project, add entry for generated jar
                            addLibraryEntry(classpath, path, accessRules, extraAttrs, pseudoJar);
                        }
                        break;
                    default :
                        addLibraryEntry(classpath, path, accessRules, extraAttrs, pseudoJar);
                        break;
                    }
                } finally {
                    IO.close(pseudoJar);
                }

            }
        }

        private void addProjectEntry(List<IClasspathEntry> classpath, IPath path, List<IAccessRule> accessRules, IClasspathAttribute[] extraAttrs) {
            for (int i = 0; i < classpath.size(); i++) {
                IClasspathEntry entry = classpath.get(i);
                if (entry.getEntryKind() != IClasspathEntry.CPE_PROJECT) {
                    continue;
                }
                if (!entry.getPath().equals(path)) {
                    continue;
                }

                // Found a project entry for the project
                List<IAccessRule> oldAccessRules = Arrays.asList(entry.getAccessRules());
                int last = oldAccessRules.size() - 1;
                if (last < 0) {
                    return; // project entry already has full access
                }
                List<IAccessRule> combinedAccessRules = null;
                if (accessRules != null) { // if not full access request
                    combinedAccessRules = new ArrayList<IAccessRule>(oldAccessRules);
                    if (DISCOURAGED.equals(combinedAccessRules.get(last))) {
                        combinedAccessRules.remove(last);
                    }
                    combinedAccessRules.addAll(accessRules);
                }
                classpath.set(i, JavaCore.newProjectEntry(path, toAccessRulesArray(combinedAccessRules), false, entry.getExtraAttributes(), false));
                return;
            }
            // Add a new project entry for the project
            classpath.add(JavaCore.newProjectEntry(path, toAccessRulesArray(accessRules), false, extraAttrs, false));
        }

        private boolean containsSource(PseudoJar jar) throws IOException {
            String entry = jar.nextEntry();
            while (entry != null) {
                if ("OSGI-INF/src/".equals(entry))
                    return true;
                entry = jar.nextEntry();
            }
            return false;
        }

        private void addLibraryEntry(List<IClasspathEntry> classpath, IPath path, List<IAccessRule> accessRules, IClasspathAttribute[] extraAttrs, PseudoJar pseudoJar) {
            IPath sourcePath;
            try {
                sourcePath = containsSource(pseudoJar) ? path : null;
            } catch (IOException e) {
                sourcePath = null;
            }

            classpath.add(JavaCore.newLibraryEntry(path, sourcePath, null, toAccessRulesArray(accessRules), extraAttrs, false));
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

        private List<IAccessRule> calculateContainerAccessRules(Container c, PseudoJar pseudoJar) {
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
                    mf = pseudoJar.readManifest();
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

        private IPath fileToPath(File file) throws Exception {
            IPath path = Central.toPath(file);
            if (path == null)
                path = Path.fromOSString(file.getAbsolutePath());
            return path;
        }

        private boolean isVersionProject(Container c) {
            return Constants.VERSION_ATTR_PROJECT.equals(c.getAttributes().get(Constants.VERSION_ATTRIBUTE));
        }

        private SetLocation error(String message, Object... args) {
            return model.error(message, args).context(model.getName()).header(Constants.BUILDPATH).file(model.getPropertiesFile().getAbsolutePath());
        }

        private SetLocation error(String message, Throwable t, Object... args) {
            return model.error(message, t, args).context(model.getName()).header(Constants.BUILDPATH).file(model.getPropertiesFile().getAbsolutePath());
        }

        private SetLocation error(Container c, String header, String message, Object... args) {
            return model.error(message, args).context(c.getBundleSymbolicName()).header(header).file(model.getPropertiesFile().getAbsolutePath());
        }

        private SetLocation error(Container c, String header, String message, Throwable t, Object... args) {
            return model.error(message, t, args).context(c.getBundleSymbolicName()).header(header).file(model.getPropertiesFile().getAbsolutePath());
        }
    }
}
