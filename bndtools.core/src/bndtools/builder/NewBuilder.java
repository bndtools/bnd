package bndtools.builder;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.core.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import aQute.lib.osgi.Builder;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.api.IValidator;
import bndtools.classpath.BndContainerInitializer;
import bndtools.preferences.BndPreferences;
import bndtools.preferences.CompileErrorAction;
import bndtools.preferences.EclipseClasspathPreference;
import bndtools.utils.Predicate;

public class NewBuilder extends IncrementalProjectBuilder {

    public static final String BUILDER_ID = Plugin.PLUGIN_ID + ".bndbuilder";
    public static final String MARKER_BND_PROBLEM = Plugin.PLUGIN_ID + ".bndproblem";

    private static final int LOG_FULL = 2;
    private static final int LOG_BASIC = 1;
    private static final int LOG_NONE = 0;

    private Project model;

    private List<String> classpathErrors;
    private MultiStatus validationResults;

    private List<String> buildLog;
    private int logLevel = LOG_NONE;
    private ScopedPreferenceStore projectPrefs;

    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
        BndPreferences prefs = new BndPreferences();
        logLevel = prefs.getBuildLogging();
        projectPrefs = new ScopedPreferenceStore(new ProjectScope(getProject()), Plugin.PLUGIN_ID);

        // Prepare build listeners
        BuildListeners listeners = new BuildListeners();

        // Prepare validations
        classpathErrors = new LinkedList<String>();
        validationResults = new MultiStatus(Plugin.PLUGIN_ID, 0, "Validation errors in bnd project", null);
        buildLog = new ArrayList<String>(5);

        // Initialise workspace OBR index (should only happen once)
        boolean builtAny = false;

        // Get the initial project
        IProject myProject = getProject();
        listeners.fireBuildStarting(myProject);
        Project model = null;
        try {
            model = Workspace.getProject(myProject.getLocation().toFile());
        } catch (Exception e) {
            clearBuildMarkers();
            createBuildMarkers(Collections.singletonList(e.getMessage()), Collections.<String>emptyList());
        }
        if (model == null)
            return null;
        this.model = model;

        // Main build section
        try {
            // Clear errors and warnings
            model.clear();

            // CASE 1: CNF changed
            if (isCnfChanged()) {
                log(LOG_BASIC, "cnf project changed");
                model.refresh();
                model.getWorkspace().refresh();
                if (BndContainerInitializer.resetClasspaths(model, myProject, classpathErrors)) {
                    log(LOG_BASIC, "classpaths were changed");
                } else {
                    log(LOG_FULL, "classpaths did not need to change");
                }
                return calculateDependsOn();
            }

            // CASE 2: local Bnd file changed, or Eclipse asks for full build
            boolean localChange = false;
            if (kind == FULL_BUILD) {
                localChange = true;
                log(LOG_BASIC, "Eclipse requested full build");
            } else if (isLocalBndFileChange()) {
                localChange = true;
                log(LOG_BASIC, "local bnd files changed");
            }
            if (localChange) {
                model.refresh();
                if (BndContainerInitializer.resetClasspaths(model, myProject, classpathErrors)) {
                    log(LOG_BASIC, "classpaths were changed");
                    return calculateDependsOn();
                } else {
                    log(LOG_FULL, "classpaths were not changed");
                    rebuildIfLocalChanges();
                    return calculateDependsOn();
                }
            }
            // (NB: from now on the delta cannot be null, due to the check in isLocalBndFileChange)

            // CASE 3: JAR file in dependency project changed
            Project changedDependency = getDependencyTargetChange();
            if (changedDependency != null) {
                log(LOG_BASIC, "target files in dependency project %s changed", changedDependency.getName());
                model.propertiesChanged();
                if (BndContainerInitializer.resetClasspaths(model, myProject, classpathErrors)) {
                    log(LOG_BASIC, "classpaths were changed");
                    return calculateDependsOn();
                } else {
                    log(LOG_FULL, "classpaths were not changed");
                }
            }

            // CASE 4: local file changes
            builtAny = rebuildIfLocalChanges();

            return calculateDependsOn();
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Build Error!", e));
        } finally {
            if (!builtAny) {
                try {
                    Central.getWorkspaceObrProvider().initialise();
                } catch (Exception e) {
                    Plugin.logError("Error initialising workspace OBR provider", e);
                }
            }

            if (!buildLog.isEmpty() && logLevel > 0) {
                StringBuilder builder = new StringBuilder();
                builder.append(String.format("BUILD LOG for project %s (%d entries):", getProject(), buildLog.size()));
                for (String message : buildLog) {
                    builder.append("\n -> ").append(message);
                }
                Plugin.log(new Status(IStatus.INFO, Plugin.PLUGIN_ID, 0, builder.toString(), null));
            }
        }
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        try {
            IProject myProject = getProject();
            Project model = Workspace.getProject(myProject.getLocation().toFile());
            if (model == null)
                return;

            // Delete everything in the target directory
            File target = model.getTarget();
            if (target.isDirectory() && target.getParentFile() != null) {
                IO.delete(target);
                target.mkdirs();
            }

            // Tell Eclipse what we did...
            IFolder targetFolder = myProject.getFolder(calculateTargetDirPath(model));
            targetFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Build Error!", e));
        }
    }

    boolean isCnfChanged() throws Exception {
        IProject cnfProject = WorkspaceUtils.findCnfProject();
        if (cnfProject == null) {
            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd configuration project (cnf) is not available in the Eclipse workspace.", null));
            return false;
        }

        IResourceDelta cnfDelta = getDelta(cnfProject);
        if (cnfDelta == null) {
            log(LOG_FULL, "no delta available for cnf project, ignoring");
            return false;
        }

        final AtomicBoolean result = new AtomicBoolean(false);
        cnfDelta.accept(new IResourceDeltaVisitor() {
            public boolean visit(IResourceDelta delta) throws CoreException {
                if (!isChangeDelta(delta))
                    return false;

                if (IResourceDelta.MARKERS == delta.getFlags())
                    return false;

                IResource resource = delta.getResource();
                if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
                    return true;

                if (resource.getType() == IResource.FOLDER && resource.getName().equals("ext")) {
                    log(LOG_FULL, "detected change in cnf due to resource %s, kind=0x%x, flags=0x%x", resource.getFullPath(), delta.getKind(), delta.getFlags());
                    result.set(true);
                }

                if (resource.getType() == IResource.FILE) {
                    if (Workspace.BUILDFILE.equals(resource.getName())) {
                        result.set(true);
                        log(LOG_FULL, "detected change in cnf due to resource %s, kind=0x%x, flags=0x%x", resource.getFullPath(), delta.getKind(), delta.getFlags());
                    } else {
                        // TODO: check other file names included from build.bnd
                    }
                }

                return false;
            }
        });

        return result.get();
    }

    private boolean isLocalBndFileChange() throws CoreException {
        IResourceDelta myDelta = getDelta(getProject());
        if (myDelta == null) {
            log(LOG_BASIC, "local project delta is null, assuming changes exist", model.getName());
            return true;
        }

        final AtomicBoolean result = new AtomicBoolean(false);
        myDelta.accept(new IResourceDeltaVisitor() {
            public boolean visit(IResourceDelta delta) throws CoreException {
                if (!isChangeDelta(delta))
                    return false;

                IResource resource = delta.getResource();

                if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
                    return true;

                if (resource.getType() == IResource.FOLDER)
                    return true;

                String extension = ((IFile) resource).getFileExtension();
                if (resource.getType() == IResource.FILE && "bnd".equalsIgnoreCase(extension)) {
                    log(LOG_FULL, "detected change due to resource %s, kind=0x%x, flags=0x%x", resource.getFullPath(), delta.getKind(), delta.getFlags());
                    result.set(true);
                    return false;
                }

                return false;
            }

        });

        return result.get();
    }

    private Project getDependencyTargetChange() throws Exception {
        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        Collection<Project> dependson = model.getDependson();
        log(LOG_FULL, "project depends on: %s", dependson);

        for (Project dep : dependson) {
            File targetDir = dep.getTarget();
            if (targetDir != null && !(targetDir.isDirectory())) // Does not exist... deleted?
                return dep;

            IProject project = WorkspaceUtils.findOpenProject(wsroot, dep);
            if (project == null) {
                Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, String.format("Dependency project '%s' from project '%s' is not in the Eclipse workspace.", dep.getName(), model.getName()), null));
                return null;
            }

            IFile buildFile = project.getFolder(targetDir.getName()).getFile(Workspace.BUILDFILES);
            IPath buildFilePath = buildFile.getProjectRelativePath();
            IResourceDelta delta = getDelta(project);

            if (delta == null) {
                // May have changed
                log(LOG_FULL, "null delta in dependency project %s", dep.getName());
                return dep;
            } else if (!isChangeDelta(delta)) {
                continue;
            } else {
                IResourceDelta buildFileDelta = delta.findMember(buildFilePath);
                if (buildFileDelta != null && isChangeDelta(buildFileDelta)) {
                    log(LOG_FULL, "detected change due to file %s, kind=0x%x, flags=0x%x", buildFile, delta.getKind(), delta.getFlags());
                    return dep;
                }
            }
            // this dependency project did not change, move on to next
        }

        // no dependencies changed
        return null;
    }

    /**
     * @return Whether any files were built
     */
    private boolean rebuildIfLocalChanges() throws Exception {
        log(LOG_FULL, "calculating local changes...");

        final Set<File> changedFiles = new HashSet<File>();

        final IPath targetDirPath = calculateTargetDirPath(model);
        final Set<File> targetJars = findJarsInTarget();

        boolean force = false;

        IResourceDelta delta = getDelta(getProject());
        if (delta != null) {
            delta.accept(new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    if (!isChangeDelta(delta))
                        return false;

                    IResource resource = delta.getResource();
                    if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
                        return true;

                    if (resource.getType() == IResource.FOLDER) {
                        IPath folderPath = resource.getProjectRelativePath();
                        // ignore ALL files in target dir
                        return !folderPath.equals(targetDirPath);
                    }

                    if (resource.getType() == IResource.FILE) {
                        File file = resource.getLocation().toFile();
                        changedFiles.add(file);
                    }

                    return false;
                }
            });
            log(LOG_FULL, "%d local files (outside target) changed or removed: %s", changedFiles.size(), changedFiles);
        } else {
            log(LOG_BASIC, "no info on local changes available");
        }

        // Process the sub-builders to determine whether a rebuild, force rebuild, or nothing is required.
        for (Builder builder : model.getSubBuilders()) {
            // If the builder's output JAR has been removed, this could be because the user
            // deleted it, so we should force build in order to regenerate it.
            File targetFile = new File(model.getTarget(), builder.getBsn() + ".jar");
            if (!targetFile.isFile()) {
                log(LOG_FULL, "output file %s of builder %s was removed, will force a rebuild", targetFile, builder.getBsn());
                force = true;
                break;
            }

            // Account for this builder's target JAR
            targetJars.remove(targetFile);

            // Finally if any removed or changed files are in scope for the bundle, we simply force rebuild
            if (!changedFiles.isEmpty()) {
                if (changedFiles.contains(builder.getPropertiesFile())) {
                    log(LOG_FULL, "the properties file for builder %s was changes, will force a rebuild", builder.getBsn());
                    force = true;
                    break;
                } else if (builder.isInScope(changedFiles)) {
                    log(LOG_FULL, "some removed files were in scope for builder %s, will force a rebuild", builder.getBsn());
                    force = true;
                    break;
                }
            }
        }

        // Delete any unaccounted-for Jars from target dir
        for (File jar : targetJars) {
            try {
                jar.delete();
            } catch (Exception e) {
                Plugin.logError("Error deleting target JAR: " + jar, e);
            }
        }

        // Do it
        boolean builtAny = false;
        if (force) {
            builtAny = rebuild(true);
        } else if (!changedFiles.isEmpty()) {
            builtAny = rebuild(false);
        }
        return builtAny;
    }

    private Set<File> findJarsInTarget() throws Exception {
        File targetDir = model.getTarget();
        File[] targetJars = targetDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".jar");
            }
        });
        Set<File> result = new HashSet<File>();
        if (targetJars != null) for (File jar : targetJars) {
            result.add(jar);
        }
        return result;
    }

    private static IPath calculateTargetDirPath(Project model) throws Exception {
        IPath basePath = Path.fromOSString(model.getBase().getAbsolutePath());
        final IPath targetDirPath = Path.fromOSString(model.getTarget().getAbsolutePath()).makeRelativeTo(basePath);
        return targetDirPath;
    }

    private enum Action { build, delete };

    /**
     * @param force Whether to force bnd to build
     * @return Whether any files were built
     */
    @SuppressWarnings("unchecked")
    private boolean rebuild(boolean force) throws Exception {
        clearBuildMarkers();

        // Check if compilation errors exist, and if so check the project settings for what to do about that...
        Action buildAction = Action.build;
        if (hasBlockingErrors()) {
            ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()), Plugin.PLUGIN_ID);
            switch (CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY))) {
            case skip:
                addBuildMarker(String.format("Will not build OSGi bundle(s) for project %s until compilation problems are fixed.", model.getName()), IMarker.SEVERITY_ERROR);
                log(LOG_BASIC, "SKIPPING due to Java problem markers");
                return false;
            case build:
                buildAction = Action.build;
                break;
            case delete:
                buildAction = Action.delete;
                break;
            }
        } else if (!classpathErrors.isEmpty()) {
            ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()), Plugin.PLUGIN_ID);
            switch (CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY))) {
            case skip:
                addBuildMarker(String.format("Will not build OSGi bundle(s) for project %s until classpath resolution problems are fixed.", model.getName()), IMarker.SEVERITY_ERROR);
                log(LOG_BASIC, "SKIPPING due to classpath resolution problem markers");
                return false;
            case build:
                buildAction = Action.build;
                break;
            case delete:
                buildAction = Action.delete;
                break;
            }
        }
        log(LOG_BASIC, "REBUILDING, force=%b, action=%s", force, buildAction);

        File[] built;

        // Validate
        List<IValidator> validators = loadValidators();
        if (validators != null) {
            Collection<? extends Builder> builders = model.getSubBuilders();
            for (Builder builder : builders) {
                validate(builder, validators);
            }
        }

        // Clear errors & warnings before build
        model.clear();
        
        // Load Eclipse classpath containers
        model.clearClasspath();
        EclipseClasspathPreference classpathPref = EclipseClasspathPreference.parse(projectPrefs.getString(EclipseClasspathPreference.PREFERENCE_KEY));
        if (classpathPref == EclipseClasspathPreference.expose) {
            List<File> classpathFiles = new ArrayList<File>(20);
            accumulateClasspath(classpathFiles, JavaCore.create(getProject()), false, new ClasspathContainerFilter());
            for (File file : classpathFiles) {
                log(LOG_FULL, "Adding Eclipse classpath entry %s", file.getAbsolutePath());
                model.addClasspath(file);
            }
        }

        if (buildAction == Action.build) {
            // Build!
            model.setTrace(true);
            if (force)
                built = model.buildLocal(false);
            else
                built = model.build();
            if (built == null) built = new File[0];

            // Log rebuilt files
            log(LOG_BASIC, "requested rebuild of %d files", built.length);
            if (logLevel >= LOG_FULL) {
                for (File builtFile : built) {
                    log(LOG_FULL, "target file %s has an age of %d ms", builtFile, System.currentTimeMillis() - builtFile.lastModified());
                }
            }
        } else {
            // Delete target files since the project has compile errors and the delete action was selected.
            for (Builder builder : model.getSubBuilders()) {
                File targetFile = new File(model.getTarget(), builder.getBsn() + ".jar");
                boolean deleted = targetFile.delete();
                log(LOG_FULL, "deleted target file %s (%b)", targetFile, deleted);
            }
            built = new File[0];
        }


        // Make sure Eclipse knows about the changed files (should already have been done?)
        IFolder targetFolder = getProject().getFolder(calculateTargetDirPath(model));
        targetFolder.refreshLocal(IResource.DEPTH_INFINITE, null);

        // Replace into the workspace OBR provider
        if (built.length > 0) {
            try {
                Central.getWorkspaceObrProvider().replaceProjectFiles(model, built);
                Central.getWorkspaceRepoProvider().replaceProjectFiles(model, built);
            } catch (Exception e) {
                Plugin.logError("Error rebuilding workspace OBR index", e);
            }
        }

        // Report errors
        List<String> errors = new ArrayList<String>(model.getErrors());
        List<String> warnings = new ArrayList<String>(model.getWarnings());
        createBuildMarkers(errors, warnings);

        return built.length > 0;
    }

    List<IValidator> loadValidators() {
        List<IValidator> validators = null;
        IConfigurationElement[] validatorElems = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "validators");
        if (validatorElems != null && validatorElems.length > 0) {
            validators = new ArrayList<IValidator>(validatorElems.length);
            for (IConfigurationElement elem : validatorElems) {
                try {
                    validators.add((IValidator) elem.createExecutableExtension("class"));
                } catch (Exception e) {
                    Plugin.logError("Unable to instantiate validator: " + elem.getAttribute("name"), e);
                }
            }
        }
        return validators;
    }

    void validate(Builder builder, List<IValidator> validators) {
        for (IValidator validator : validators) {
            IStatus status = validator.validate(builder);
            if (!status.isOK())
                validationResults.add(status);
        }
    }

    private IProject[] calculateDependsOn() throws Exception {
        Collection<Project> dependsOn = model.getDependson();
        List<IProject> result = new ArrayList<IProject>(dependsOn.size() + 1);

        IProject cnfProject = WorkspaceUtils.findCnfProject();
        if (cnfProject != null)
            result.add(cnfProject);

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        for (Project project : dependsOn) {
            IProject targetProj = WorkspaceUtils.findOpenProject(wsroot, project);
            if (targetProj == null)
                Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, "No open project in workspace for Bnd '-dependson' dependency: " + project.getName(), null));
            else
                result.add(targetProj);
        }

        log(LOG_FULL, "returning depends-on list: %s", result);
        return result.toArray(new IProject[result.size()]);
    }
    
    private void accumulateClasspath(List<File> files, IJavaProject project, boolean exports, Predicate<IClasspathContainer>... containerFilters) throws JavaModelException {
        if (exports) {
            IPath outputPath = project.getOutputLocation();
            files.add(getFileForPath(outputPath));
        }
        
        IClasspathEntry[] entries = project.getRawClasspath();
        List<IClasspathEntry> queue = new ArrayList<IClasspathEntry>(entries.length);
        queue.addAll(Arrays.asList(entries));

        while (!queue.isEmpty()) {
            IClasspathEntry entry = queue.remove(0);
            
            if (exports && !entry.isExported())
                continue;
            
            IPath path = entry.getPath();
            
            switch(entry.getEntryKind()) {
            case IClasspathEntry.CPE_LIBRARY:
                files.add(getFileForPath(path));
                break;
            case IClasspathEntry.CPE_VARIABLE:
                IPath resolvedPath = JavaCore.getResolvedVariablePath(path);
                files.add(getFileForPath(resolvedPath));
                break;
            case IClasspathEntry.CPE_SOURCE:
                IPath outputLocation = entry.getOutputLocation();
                if (exports && outputLocation != null)
                    files.add(getFileForPath(outputLocation));
                break;
            case IClasspathEntry.CPE_CONTAINER:
                IClasspathContainer container = JavaCore.getClasspathContainer(path, project);
                boolean allow = true;
                for (Predicate<IClasspathContainer> filter : containerFilters)
                    if (!filter.select(container))
                        allow = false;
                if (allow)
                    queue.addAll(Arrays.asList(container.getClasspathEntries()));
                break;
            case IClasspathEntry.CPE_PROJECT:
                IProject targetProject = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
                IJavaProject targetJavaProject = JavaCore.create(targetProject);
                accumulateClasspath(files, targetJavaProject, true, containerFilters);
                break;
            }
        }
    }
    
    private File getFileForPath(IPath path) {
        File file;
        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if (resource != null && resource.exists())
            file = resource.getLocation().toFile();
        else
            file = path.toFile();
        return file;
    }
    
    private boolean hasBlockingErrors() {
        try {
            if (containsError(getProject().findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)))
                return true;
            return false;
        } catch (CoreException e) {
            Plugin.logError("Error looking for project problem markers", e);
            return false;
        }
    }

    private static boolean containsError(IMarker[] markers) {
        if (markers != null)
            for (IMarker marker : markers) {
                int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                if (severity == IMarker.SEVERITY_ERROR)
                    return true;
            }
        return false;
    }

    private static boolean isChangeDelta(IResourceDelta delta) {
        if (IResourceDelta.MARKERS == delta.getFlags())
            return false;
        if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) == 0)
            return false;
        return true;
    }

    private void createBuildMarkers(Collection<? extends String> errors, Collection<? extends String> warnings) throws CoreException {
        for (String error : errors) {
            addBuildMarker(error, IMarker.SEVERITY_ERROR);
        }
        for (String warning : warnings) {
            addBuildMarker(warning, IMarker.SEVERITY_WARNING);
        }
        for (String error : classpathErrors) {
            addClasspathMarker(error, IMarker.SEVERITY_ERROR);
        }

        if (!validationResults.isOK()) {
            for (IStatus status : validationResults.getChildren()) {
                addClasspathMarker(status);
            }
        }
    }

    private void clearBuildMarkers() throws CoreException {
        IFile bndFile = getProject().getFile(Project.BNDFILE);

        if (bndFile.exists()) {
            bndFile.deleteMarkers(MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
        }
    }

    private IResource getBuildMarkerTargetResource() {
        IProject project = getProject();
        IResource bndFile = project.getFile(Project.BNDFILE);
        if (bndFile == null || !bndFile.exists())
            return project;
        return bndFile;
    }

    private void addBuildMarker(String message, int severity) throws CoreException {
        IResource resource = getBuildMarkerTargetResource();

        IMarker marker = resource.createMarker(MARKER_BND_PROBLEM);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.MESSAGE, message);
        // marker.setAttribute(IMarker.LINE_NUMBER, 1);
    }

    private void addClasspathMarker(String message, int severity) throws CoreException {
        IResource resource = getBuildMarkerTargetResource();

        IMarker marker = resource.createMarker(BndContainerInitializer.MARKER_BND_CLASSPATH_PROBLEM);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.MESSAGE, message);
        // marker.setAttribute(IMarker.LINE_NUMBER, 1);
    }

    private void addClasspathMarker(IStatus status) throws CoreException {
        int severity;
        switch (status.getSeverity()) {
        case IStatus.CANCEL:
        case IStatus.ERROR:
            severity = IMarker.SEVERITY_ERROR;
            break;
        case IStatus.WARNING:
            severity = IMarker.SEVERITY_WARNING;
            break;
        default:
            severity = IMarker.SEVERITY_INFO;
        }
        addClasspathMarker(status.getMessage(), severity);
    }

    private void log(int level, String message, Object... args) {
        if (logLevel >= level)
            buildLog.add(String.format(message, args));
    }

}
