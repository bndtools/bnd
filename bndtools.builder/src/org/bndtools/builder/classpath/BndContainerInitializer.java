package org.bndtools.builder.classpath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ModelListener;
import org.bndtools.builder.BndtoolsBuilder;
import org.bndtools.builder.BuildLogger;
import org.bndtools.builder.BuilderPlugin;
import org.bndtools.utils.jar.PseudoJar;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import aQute.bnd.build.CircularDependencyException;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter.SetLocation;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;

/**
 * ClasspathContainerInitializer for the aQute.bnd.classpath.container name.
 * <p>
 * Used in the .classpath file of bnd project to couple the bnd -buildpath into the Eclipse IDE.
 */
public class BndContainerInitializer extends ClasspathContainerInitializer implements ModelListener {

    static final ILogger logger = Logger.getLogger(BndContainerInitializer.class);
    private static final ClasspathContainerSerializationHelper<BndContainer> serializationHelper = new ClasspathContainerSerializationHelper<>();

    public BndContainerInitializer() {
        super();
        Central.onWorkspaceInit(new Success<Workspace,Void>() {
            @Override
            public Promise<Void> call(Promise<Workspace> resolved) throws Exception {
                Central.getInstance().addModelListener(BndContainerInitializer.this);
                return null;
            }
        });
    }

    @Override
    public void initialize(IPath containerPath, final IJavaProject javaProject) throws CoreException {
        IProject project = javaProject.getProject();

        /* If workspace is already initialized or there is no
         * saved container information, then update the container
         * now.
         */
        File containerFile = getContainerFile(project);
        if (Central.isWorkspaceInited() || !containerFile.isFile()) {
            Updater updater = new Updater(project, javaProject);
            updater.updateClasspathContainer(true);
            return;
        }

        /*
         * Read the saved container information and update the container now.
         * Request an update using the project information after the
         * workspace is initialized.
         */
        BndContainer container = loadClasspathContainer(project);
        Updater.setClasspathContainer(javaProject, container);
        Central.onWorkspaceInit(new Success<Workspace,Void>() {
            @Override
            public Promise<Void> call(Promise<Workspace> resolved) throws Exception {
                requestClasspathContainerUpdate(BndtoolsConstants.BND_CLASSPATH_ID, javaProject, null);
                return null;
            }
        });
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
        return BndContainer.DESCRIPTION;
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
    static IClasspathContainer getClasspathContainer(IJavaProject javaProject) {
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

    private static BndContainer loadClasspathContainer(IProject project) {
        File containerFile = getContainerFile(project);
        if (!containerFile.isFile()) {
            return new BndContainer(Updater.EMPTY_ENTRIES, 0L);
        }
        try {
            return serializationHelper.readClasspathContainer(containerFile);
        } catch (IOException | ClassNotFoundException e) {
            return new BndContainer(Updater.EMPTY_ENTRIES, 0L);
        }
    }

    static void storeClasspathContainer(IProject project, BndContainer container) {
        File containerFile = getContainerFile(project);
        try {
            serializationHelper.writeClasspathContainer(container, containerFile);
        } catch (IOException e) {
            logger.logError("Unable to store classpath container", e);
            IO.delete(containerFile);
        }
    }

    private static File getContainerFile(IProject p) {
        return IO.getFile(BuilderPlugin.getInstance().getStateLocation().toFile(), p.getName() + ".container");
    }

    private static class Updater {
        static final IClasspathEntry[] EMPTY_ENTRIES = new IClasspathEntry[0];
        private static final IAccessRule DISCOURAGED = JavaCore.newAccessRule(new Path("**"), IAccessRule.K_DISCOURAGED | IAccessRule.IGNORE_IF_BETTER);
        private static final IClasspathAttribute EMPTY_INDEX = JavaCore.newClasspathAttribute(IClasspathAttribute.INDEX_LOCATION_ATTRIBUTE_NAME,
                "platform:/plugin/" + BndtoolsBuilder.PLUGIN_ID + "/org/bndtools/builder/classpath/empty.index");
        private static final Pattern packagePattern = Pattern.compile("(?<=^|\\.)\\*(?=\\.|$)|\\.");
        private static final Map<File,JarInfo> jarInfo = Collections.synchronizedMap(new WeakHashMap<File,JarInfo>());

        private final IProject project;
        private final IJavaProject javaProject;
        private final IWorkspaceRoot root;
        private final Project model;
        private long lastModified;

        Updater(IProject project, IJavaProject javaProject) {
            assert project != null;
            assert javaProject != null;
            this.project = project;
            this.javaProject = javaProject;
            this.root = project.getWorkspace().getRoot();

            Project p = null;
            try {
                p = Central.getProject(project);
            } catch (Exception e) {
                // this can happen during first project creation in an empty workspace
                logger.logInfo("Unable to get bnd project for project " + project.getName(), e);
            }
            this.model = p;
        }

        void updateClasspathContainer(boolean init) throws CoreException {
            if (model == null) { // this can happen during new project creation
                setClasspathContainer(javaProject, new BndContainer(EMPTY_ENTRIES, 0L));
                return;
            }

            List<IClasspathEntry> newClasspath = Collections.emptyList();
            final List<IResource> filesToRefresh = new ArrayList<IResource>(20);
            try {
                newClasspath = Central.bndCall(new Callable<List<IClasspathEntry>>() {
                    @Override
                    public List<IClasspathEntry> call() throws Exception {
                        return calculateProjectClasspath(filesToRefresh);
                    }
                });
            } catch (Exception e) {
                SetLocation error = error("Unable to calculate classpath for project %s", e, project.getName());
                logger.logError(error.location().message, e);
            }

            newClasspath = BndContainerSourceManager.loadAttachedSources(project, newClasspath);

            if (!init) {
                IClasspathContainer container = getClasspathContainer(javaProject);
                if (container instanceof BndContainer) {
                    BndContainer bndContainer = (BndContainer) container;
                    List<IClasspathEntry> currentClasspath = Arrays.asList(bndContainer.getClasspathEntries());
                    if (newClasspath.equals(currentClasspath) && (lastModified <= bndContainer.lastModified())) {
                        return; // no change; so no need for new container
                    }
                }
            }

            BndContainer bndContainer = new BndContainer(newClasspath.toArray(new IClasspathEntry[0]), lastModified);
            setClasspathContainer(javaProject, bndContainer);
            storeClasspathContainer(project, bndContainer);
            if (!init) {
                refreshFiles(filesToRefresh);
            }
        }

        private void refreshFiles(List<IResource> filesToRefresh) throws CoreException {
            for (IResource target : filesToRefresh) {
                int depth = target.getType() == IResource.FILE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
                if (!target.isSynchronized(depth)) {
                    target.refreshLocal(depth, null);
                }
            }
        }

        static void setClasspathContainer(IJavaProject javaProject, BndContainer container) throws JavaModelException {
            JavaCore.setClasspathContainer(BndtoolsConstants.BND_CLASSPATH_ID, new IJavaProject[] {
                    javaProject
            }, new IClasspathContainer[] {
                    container
            }, null);

            BndPreferences prefs = new BndPreferences();
            if (prefs.getBuildLogging() == BuildLogger.LOG_FULL) {
                StringBuilder sb = new StringBuilder();
                sb.append(container.getDescription()).append(" for ").append(javaProject.getProject().getName());
                for (IClasspathEntry cpe : container.getClasspathEntries()) {
                    sb.append("\n--- ").append(cpe);
                }
                logger.logInfo(sb.append("\n").toString(), null);
            }
        }

        private List<IClasspathEntry> calculateProjectClasspath(List<IResource> filesToRefresh) {
            if (!project.isOpen())
                return Collections.emptyList();

            List<IClasspathEntry> classpath = new ArrayList<IClasspathEntry>(20);
            try {
                Collection<Container> containers = model.getBuildpath();
                calculateContainersClasspath(Constants.BUILDPATH, containers, classpath, filesToRefresh);

                containers = model.getTestpath();
                calculateContainersClasspath(Constants.TESTPATH, containers, classpath, filesToRefresh);

                containers = model.getBootclasspath();
                calculateContainersClasspath(Constants.BUILDPATH, containers, classpath, filesToRefresh);
            } catch (CircularDependencyException e) {
                error("Circular dependency during classpath calculation: %s", e, e.getMessage());
                return Collections.emptyList();
            } catch (Exception e) {
                error("Unexpected error during classpath calculation: %s", e, e.getMessage());
                return Collections.emptyList();
            }

            return classpath;
        }

        private void calculateContainersClasspath(String header, Collection<Container> containers, List<IClasspathEntry> classpath, List<IResource> filesToRefresh) {
            for (Container c : containers) {
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
                } catch (Exception e) {
                    error(c, header, "Failed to convert file %s to Eclipse path: %s", e, file, e.getMessage());
                    continue;
                }
                IResource resource = Central.toResource(file);
                if (resource != null) {
                    filesToRefresh.add(resource);
                }

                List<IClasspathAttribute> extraAttrs = calculateContainerAttributes(c);
                List<IAccessRule> accessRules = calculateContainerAccessRules(c);

                switch (c.getType()) {
                case PROJECT :
                    IPath projectPath = root.getFile(path).getProject().getFullPath();
                    addProjectEntry(classpath, projectPath, accessRules, extraAttrs);
                    if (!isVersionProject(c)) { // if not version=project, add entry for generated jar
                        /* Supply an empty index for the generated JAR of a workspace project dependency.
                         * This prevents the non-editable source files in the generated jar from appearing
                         * in the Open Type dialog. */
                        extraAttrs.add(EMPTY_INDEX);
                        addLibraryEntry(classpath, path, file, accessRules, extraAttrs);
                    }
                    break;
                default :
                    addLibraryEntry(classpath, path, file, accessRules, extraAttrs);
                    break;
                }
            }
        }

        private void addProjectEntry(List<IClasspathEntry> classpath, IPath path, List<IAccessRule> accessRules, List<IClasspathAttribute> extraAttrs) {
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
            classpath.add(JavaCore.newProjectEntry(path, toAccessRulesArray(accessRules), false, toClasspathAttributesArray(extraAttrs), false));
        }

        private IPath calculateSourceAttachmentPath(IPath path, File file) {
            JarInfo info = getJarInfo(file);
            return info.hasSource ? path : null;
        }

        private JarInfo getJarInfo(File file) {
            final long lastModified = file.lastModified();
            JarInfo info = jarInfo.get(file);
            if ((info != null) && (lastModified == info.lastModified)) {
                return info;
            }
            info = new JarInfo();
            if (!file.exists()) {
                return info;
            }
            info.lastModified = lastModified;
            try (PseudoJar jar = new PseudoJar(file)) {
                Manifest mf = jar.readManifest();
                if ((mf != null) && (mf.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION) != null)) {
                    Parameters exportPkgs = new Parameters(mf.getMainAttributes().getValue(Constants.EXPORT_PACKAGE));
                    Set<String> exports = exportPkgs.keySet();
                    info.exports = exports.toArray(new String[0]);
                }
                for (String entry = jar.nextEntry(); entry != null; entry = jar.nextEntry()) {
                    if (entry.startsWith("OSGI-OPT/src/")) {
                        info.hasSource = true; // use library path as source attachment path
                        break;
                    }
                }
            } catch (IOException e) {
                logger.logInfo("Failed to read " + file, e);
            }
            jarInfo.put(file, info);
            return info;
        }

        private void addLibraryEntry(List<IClasspathEntry> classpath, IPath path, File file, List<IAccessRule> accessRules, List<IClasspathAttribute> extraAttrs) {
            IPath sourceAttachmentPath = calculateSourceAttachmentPath(path, file);
            classpath.add(JavaCore.newLibraryEntry(path, sourceAttachmentPath, null, toAccessRulesArray(accessRules), toClasspathAttributesArray(extraAttrs), false));
            updateLastModified(file.lastModified());
        }

        private List<IClasspathAttribute> calculateContainerAttributes(Container c) {
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

            return attrs;
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
                JarInfo info = getJarInfo(c.getFile());
                if (info.exports == null) {
                    break; // no export; so full access
                }
                List<IAccessRule> accessRules = new ArrayList<IAccessRule>();
                for (String exportPkg : info.exports) {
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
            File accessPatternsFile = getAccessPatternsFile(p);
            String oldAccessPatterns = "";
            boolean exists = accessPatternsFile.exists();
            if (exists) { // read persisted access patterns
                try {
                    oldAccessPatterns = IO.collect(accessPatternsFile);
                    updateLastModified(accessPatternsFile.lastModified());
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
                if (!oldAccessPatterns.isEmpty()) { // if not empty, there are access patterns
                    for (String pathStr : patterns) {
                        accessRules.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
                    }
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
                    updateLastModified(accessPatternsFile.lastModified());
                } catch (final IOException e) {
                    logger.logError("Failed to write access patterns file for project " + p.getName(), e);
                }
            }

            return accessRules;
        }

        private File getAccessPatternsFile(Project p) {
            return IO.getFile(BuilderPlugin.getInstance().getStateLocation().toFile(), p.getName() + ".accesspatterns");
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

        private IClasspathAttribute[] toClasspathAttributesArray(List<IClasspathAttribute> attrs) {
            if (attrs == null) {
                return null;
            }
            IClasspathAttribute[] attrsArray = attrs.toArray(new IClasspathAttribute[0]);
            return attrsArray;
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

        private SetLocation error(String message, Throwable t, Object... args) {
            return model.error(message, t, args).context(model.getName()).header(Constants.BUILDPATH).file(model.getPropertiesFile().getAbsolutePath());
        }

        private SetLocation error(Container c, String header, String message, Object... args) {
            return model.error(message, args).context(c.getBundleSymbolicName()).header(header).file(model.getPropertiesFile().getAbsolutePath());
        }

        private SetLocation error(Container c, String header, String message, Throwable t, Object... args) {
            return model.error(message, t, args).context(c.getBundleSymbolicName()).header(header).file(model.getPropertiesFile().getAbsolutePath());
        }

        private void updateLastModified(long time) {
            if (time > lastModified) {
                lastModified = time;
            }
        }
    }

    private static class JarInfo {
        boolean hasSource;
        String[] exports;
        long lastModified;

        JarInfo() {}
    }
}
