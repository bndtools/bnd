package org.bndtools.builder;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.IValidator;
import org.bndtools.api.Logger;
import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.bndtools.build.api.BuildErrorDetailsHandlers;
import org.bndtools.build.api.DefaultBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.bndtools.builder.classpath.BndContainerInitializer;
import org.bndtools.utils.Predicate;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.bndtools.utils.workspace.FileUtils;
import org.bndtools.utils.workspace.WorkspaceUtils;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Version;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.lib.collections.SortedList;
import aQute.lib.io.IO;
import aQute.service.reporter.Report.Location;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;
import bndtools.preferences.CompileErrorAction;
import bndtools.preferences.EclipseClasspathPreference;

public class NewBuilder extends IncrementalProjectBuilder {

    public static final String PLUGIN_ID = "bndtools.builder";
    public static final String BUILDER_ID = BndtoolsConstants.BUILDER_ID;

    private static final ILogger logger = Logger.getLogger(NewBuilder.class);

    private static final int LOG_FULL = 2;
    private static final int LOG_BASIC = 1;
    private static final int LOG_NONE = 0;

    private Project model;
    private BuildListeners listeners;
    private Collection< ? extends Builder> subBuilders;

    private List<String> classpathErrors;
    private MultiStatus validationResults;

    private List<String> buildLog;
    private int logLevel = LOG_NONE;
    private ScopedPreferenceStore projectPrefs;

    private int nrFilesBuilt = 0;

    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
        BndPreferences prefs = new BndPreferences();
        logLevel = prefs.getBuildLogging();
        projectPrefs = new ScopedPreferenceStore(new ProjectScope(getProject()), BndtoolsConstants.CORE_PLUGIN_ID);

        // Prepare validations
        classpathErrors = new LinkedList<String>();
        validationResults = new MultiStatus(PLUGIN_ID, 0, "Validation errors in bnd project", null);
        buildLog = new ArrayList<String>(5);

        nrFilesBuilt = 0;

        try {
            // Prepare build listeners and build error handlers
            listeners = new BuildListeners();

            // Get the initial project
            IProject myProject = getProject();
            reportDelta(myProject);

            listeners.fireBuildStarting(myProject);
            Project model = null;
            try {
                model = Central.getProject(myProject.getLocation().toFile());
            } catch (Exception e) {
                clearBuildMarkers();
                addBuildMarkers(e.getMessage(), IMarker.SEVERITY_ERROR);
            }
            if (model == null)
                return null;
            this.model = model;
            model.setDelayRunDependencies(true);

            // Main build section
            IProject[] dependsOn = calculateDependsOn(model);

            // Clear errors and warnings
            model.clear();

            // CASE 1: CNF changed
            if (isCnfChanged()) {
                log(LOG_BASIC, "cnf project changed");
                model.refresh();
                if (BndContainerInitializer.resetClasspaths(model, myProject, classpathErrors)) {
                    log(LOG_BASIC, "classpaths were changed");
                } else {
                    log(LOG_FULL, "classpaths did not need to change");
                }

                // Notify the repository listeners that ALL repo contents may have changed.
                List<RepositoryListenerPlugin> repoListeners = Central.getWorkspace().getPlugins(RepositoryListenerPlugin.class);
                for (RepositoryListenerPlugin repoListener : repoListeners) {
                    repoListener.repositoriesRefreshed();
                }

                return dependsOn;
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
                    return dependsOn;
                }
                log(LOG_FULL, "classpaths were not changed");
                this.subBuilders = model.getSubBuilders();
                rebuildIfLocalChanges(dependsOn, true);
                return dependsOn;
            }
            // (NB: from now on the delta cannot be null, due to the check in
            // isLocalBndFileChange)

            // CASE 3: JAR file in dependency project changed
            Project changedDependency = getDependencyTargetChange();
            if (changedDependency != null) {
                log(LOG_BASIC, "target files in dependency project %s changed", changedDependency.getName());
                model.propertiesChanged();
                if (BndContainerInitializer.resetClasspaths(model, myProject, classpathErrors)) {
                    log(LOG_BASIC, "classpaths were changed");
                    return dependsOn;
                }
                log(LOG_FULL, "classpaths were not changed");
            }

            // CASE 4: local file changes
            this.subBuilders = model.getSubBuilders();
            rebuildIfLocalChanges(dependsOn, false);

            return dependsOn;
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Build Error!", e));
        } finally {
            listeners.release();
            if (!buildLog.isEmpty() && logLevel > 0) {
                StringBuilder builder = new StringBuilder();
                builder.append(String.format("BUILD LOG %2s %s", (nrFilesBuilt > 0 ? nrFilesBuilt : ""), getProject()));
                for (String message : buildLog) {
                    builder.append("\n -> ").append(message);
                }
                logger.logInfo(builder.toString(), null);
            }

            model = null;
            subBuilders = null;
        }
    }

    /*
     * Report the full delta in the log
     */
    private void reportDelta(IProject project) throws CoreException {
        IResourceDelta delta = getDelta(project);
        if (delta == null) {
            log(LOG_FULL, "No delta, is null");
            return;
        }

        delta.accept(new IResourceDeltaVisitor() {

            @Override
            public boolean visit(IResourceDelta delta) throws CoreException {
                log(LOG_FULL, delta.toString() + " " + delta.getResource().getModificationStamp() + " " + delta.getKind());
                return true;
            }
        });

    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        try {
            IProject myProject = getProject();
            Project model = Central.getProject(myProject.getLocation().toFile());
            if (model == null)
                return;

            // Delete everything in the target directory
            File target = model.getTarget();
            if (target.isDirectory() && target.getParentFile() != null) {
                IO.delete(target);
                if (!target.exists() && !target.mkdirs()) {
                    throw new IOException("Could not create directory " + target);
                }
            }

            // Tell Eclipse what we did...
            IFolder targetFolder = myProject.getFolder(calculateTargetDirPath(model));
            targetFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Build Error!", e));
        }
    }

    boolean isCnfChanged() throws Exception {
        IProject cnfProject = WorkspaceUtils.findCnfProject();
        if (cnfProject == null) {
            logger.logError("Bnd configuration project (cnf) is not available in the Eclipse workspace.", null);
            return false;
        }

        IResourceDelta cnfDelta = getDelta(cnfProject);
        if (cnfDelta == null) {
            log(LOG_FULL, "no delta available for cnf project, ignoring");
            return false;
        }

        final AtomicBoolean result = new AtomicBoolean(false);
        cnfDelta.accept(new IResourceDeltaVisitor() {
            @Override
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
                        // Check files included by the -include directive in build.bnd
                        List<File> includedFiles = model.getWorkspace().getIncluded();
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
            @Override
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

                if (resource.getType() == IResource.FILE) {
                    // Check files included by the -include directive in bnd.bnd
                    List<File> includedFiles = model.getIncluded();
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
            // Does not exist... was it deleted?
            if (targetDir == null || !(targetDir.isDirectory()))
                return dep;

            IProject project = WorkspaceUtils.findOpenProject(wsroot, dep);
            if (project == null) {
                logger.logWarning(String.format("Dependency project '%s' from project '%s' is not in the Eclipse workspace.", dep.getName(), model.getName()), null);
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
    private boolean rebuildIfLocalChanges(IProject[] dependsOn, boolean forceBuild) throws Exception {
        log(LOG_FULL, "calculating local changes...");

        final Set<File> changedFiles = new HashSet<File>();

        final Set<File> targetJars = findJarsInTarget();

        boolean force = forceBuild;
        IResourceDelta delta;

        ProjectDeltaVisitor deltaVisitor = new ProjectDeltaVisitor(getProject(), changedFiles);

        // Get delta on local project
        delta = getDelta(getProject());
        if (delta != null) {
            delta.accept(deltaVisitor);

            log(LOG_FULL, "%d files in local project (outside target) changed or removed: %s, forced=%s", changedFiles.size(), changedFiles, deltaVisitor.force);

            if (deltaVisitor.force || changedFiles.size() > 0) {
                log(LOG_FULL, "Project changed: files=%s, force = %s", changedFiles, deltaVisitor.force);
                force = true;
            }
        } else {
            log(LOG_BASIC, "no info on local changes, doing a full build");
            force = true;
        }

        if (!force) {
            // Get deltas on dependency projects
            for (IProject depProject : dependsOn) {
                delta = getDelta(depProject);
                if (delta != null) {
                    Set<File> changedByProject = new HashSet<File>();
                    ProjectDeltaVisitor depVisitor = new ProjectDeltaVisitor(depProject, changedByProject);
                    delta.accept(depVisitor);

                    changedFiles.addAll(changedByProject);

                    //
                    // If the visitor detected a project that we depend on
                    // and that has a change in its output folder then we
                    // must rebuild. no use checking any further
                    //

                    if (depVisitor.force) {
                        log(LOG_FULL, "Project %s, which we depend on has changed its output, forcing rebuild, files changed are %s", depProject, changedByProject);
                        force = true;
                        break;
                    }

                    log(LOG_FULL, "%d files in dependency project '%s' changed or removed: %s", changedFiles.size(), depProject.getName(), changedByProject);

                } else {
                    log(LOG_BASIC, "no info available on changes from project '%s'", depProject.getName());
                }
            }
        } else
            log(LOG_FULL, "Ignoring project dependencies because we already decided to build");

        // Process the sub-builders to determine whether a rebuild, force
        // rebuild, or nothing is required.
        if (!model.isNoBundles())
            for (Builder builder : subBuilders) {
                // If the builder's output JAR has been removed, this could be
                // because the user
                // deleted it, so we should force build in order to regenerate
                // it.
                File targetFile = new File(model.getTarget(), builder.getBsn() + ".jar");
                if (!targetFile.isFile()) {
                    log(LOG_FULL, "output file %s of builder %s was removed, will force a rebuild", targetFile, builder.getBsn());
                    force = true;
                    break;
                }

                // Account for this builder's target JAR
                targetJars.remove(targetFile);

                // Finally if any removed or changed files are in scope for the
                // bundle, we simply force rebuild
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
                logger.logError("Error deleting target JAR: " + jar, e);
            }
        }

        // Remove files not in scope

        if (!force && !changedFiles.isEmpty() && !model.isNoBundles()) {
            for (Iterator<File> itr = changedFiles.iterator(); itr.hasNext();) {
                File changeFile = itr.next();
                boolean inScope = false;
                for (Builder builder : subBuilders) {
                    if (builder.isInScope(Collections.singleton(changeFile))) {
                        inScope = true;
                        break;
                    }
                }
                if (!inScope) {
                    itr.remove();
                    log(LOG_FULL, "removed file %s, was not in scope for project %s", changeFile, getProject().getName());
                }
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
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".jar");
            }
        });
        Set<File> result = new HashSet<File>();
        if (targetJars != null)
            for (File jar : targetJars) {
                result.add(jar);
            }
        return result;
    }

    private static IPath calculateTargetDirPath(Project model) throws Exception {
        IPath basePath = Path.fromOSString(model.getBase().getAbsolutePath());
        final IPath targetDirPath = Path.fromOSString(model.getTarget().getAbsolutePath()).makeRelativeTo(basePath);
        return targetDirPath;
    }

    private enum Action {
        build, delete
    }

    /**
     * @param force
     *            Whether to force bnd to build
     * @return Whether any files were built
     */
    @SuppressWarnings("unchecked")
    private boolean rebuild(boolean force) throws Exception {
        clearBuildMarkers();

        // Check if compilation errors exist, and if so check the project
        // settings for what to do about that...
        Action buildAction = Action.build;
        if (hasBlockingErrors()) {
            ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()), BndtoolsConstants.CORE_PLUGIN_ID);
            switch (CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY))) {
            case skip :
            default :
                addBuildMarkers(String.format("Will not build OSGi bundle(s) for project %s until compilation problems are fixed.", model.getName()), IMarker.SEVERITY_ERROR);
                log(LOG_BASIC, "SKIPPING due to Java problem markers");
                return false;
            case build :
                buildAction = Action.build;
                break;
            case delete :
                buildAction = Action.delete;
                break;
            }
        } else if (!classpathErrors.isEmpty()) {
            ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()), BndtoolsConstants.CORE_PLUGIN_ID);
            switch (CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY))) {
            case skip :
            default :
                addBuildMarkers(String.format("Will not build OSGi bundle(s) for project %s until classpath resolution problems are fixed.", model.getName()), IMarker.SEVERITY_ERROR);
                log(LOG_BASIC, "SKIPPING due to classpath resolution problem markers");
                return false;
            case build :
                buildAction = Action.build;
                break;
            case delete :
                buildAction = Action.delete;
                break;
            }
        }

        File[] built;

        // Clear errors & warnings before build
        model.clear();

        // Validate
        List<IValidator> validators = loadValidators();
        if (validators != null) {
            for (Builder builder : subBuilders) {
                validate(builder, validators);
            }
        }

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
            boolean stale = model.isStale();

            if (force || stale) {
                log(LOG_BASIC, "REBUILDING: force=%b; stale=%b", force, stale);
                long now = System.currentTimeMillis();
                built = model.buildLocal(false);
                if (built == null)
                    built = new File[0]; // shouldn't happen but just in case

                nrFilesBuilt += built.length;
                log(LOG_BASIC, "Build took %s secs", (System.currentTimeMillis() - now) / 1000);
                decorate();
            } else {
                log(LOG_BASIC, "NOT REBUILDING: force=%b;stale=%b", force, stale);
                built = new File[0];
            }

            // Notify the build listeners
            if (listeners != null && built.length > 0) {
                IPath[] paths = new IPath[built.length];
                for (int i = 0; i < built.length; i++)
                    paths[i] = Central.toPath(built[i]);
                listeners.fireBuiltBundles(getProject(), paths);
            }

            // Log rebuilt files
            log(LOG_BASIC, "%d files were rebuilt", built.length);
            if (logLevel >= LOG_FULL) {
                for (File builtFile : built) {
                    log(LOG_FULL, "target file %s has an age of %d ms", builtFile, System.currentTimeMillis() - builtFile.lastModified());
                }
            }
        } else {
            // Delete target files since the project has compile errors and the
            // delete action was selected.
            for (Builder builder : subBuilders) {
                File targetFile = new File(model.getTarget(), builder.getBsn() + ".jar");
                boolean deleted = targetFile.delete();
                log(LOG_FULL, "deleted target file %s (%b)", targetFile, deleted);
            }
            built = new File[0];
        }

        // Notify central that there are new bundles
        if (built.length > 0)
            Central.invalidateIndex();

        // Make sure Eclipse knows about the changed files (should already have
        // been done?)
        IFolder targetFolder = getProject().getFolder(calculateTargetDirPath(model));
        targetFolder.refreshLocal(IResource.DEPTH_INFINITE, null);

        // Report errors
        createBuildMarkers(model);

        return built.length > 0;
    }

    static List<IValidator> loadValidators() {
        List<IValidator> validators = null;
        IConfigurationElement[] validatorElems = Platform.getExtensionRegistry().getConfigurationElementsFor(BndtoolsConstants.CORE_PLUGIN_ID, "validators");
        if (validatorElems != null && validatorElems.length > 0) {
            validators = new ArrayList<IValidator>(validatorElems.length);
            for (IConfigurationElement elem : validatorElems) {
                try {
                    validators.add((IValidator) elem.createExecutableExtension("class"));
                } catch (Exception e) {
                    logger.logError("Unable to instantiate validator: " + elem.getAttribute("name"), e);
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

    private IProject[] calculateDependsOn(Project model) throws Exception {
        Collection<Project> dependsOn = model.getDependson();
        List<IProject> result = new ArrayList<IProject>(dependsOn.size() + 1);

        IProject cnfProject = WorkspaceUtils.findCnfProject();
        if (cnfProject != null)
            result.add(cnfProject);

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        for (Project project : dependsOn) {
            IProject targetProj = WorkspaceUtils.findOpenProject(wsroot, project);
            if (targetProj == null)
                logger.logWarning("No open project in workspace for Bnd '-dependson' dependency: " + project.getName(), null);
            else
                result.add(targetProj);
        }

        log(LOG_FULL, "Calculated depends-on list: %s", result);
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

            switch (entry.getEntryKind()) {
            case IClasspathEntry.CPE_LIBRARY :
                files.add(getFileForPath(path));
                break;
            case IClasspathEntry.CPE_VARIABLE :
                IPath resolvedPath = JavaCore.getResolvedVariablePath(path);
                files.add(getFileForPath(resolvedPath));
                break;
            case IClasspathEntry.CPE_SOURCE :
                IPath outputLocation = entry.getOutputLocation();
                if (exports && outputLocation != null)
                    files.add(getFileForPath(outputLocation));
                break;
            case IClasspathEntry.CPE_CONTAINER :
                IClasspathContainer container = JavaCore.getClasspathContainer(path, project);
                boolean allow = true;
                for (Predicate<IClasspathContainer> filter : containerFilters)
                    if (!filter.select(container))
                        allow = false;
                if (allow)
                    queue.addAll(Arrays.asList(container.getClasspathEntries()));
                break;
            case IClasspathEntry.CPE_PROJECT :
                IProject targetProject = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
                IJavaProject targetJavaProject = JavaCore.create(targetProject);
                accumulateClasspath(files, targetJavaProject, true, containerFilters);
                break;
            default :
                logger.logError("Unhandled IPath entryKind of " + entry.getEntryKind(), null);
                break;
            }
        }
    }

    private static File getFileForPath(IPath path) {
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
            logger.logError("Error looking for project problem markers", e);
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

    private void createBuildMarkers(Project model) throws Exception {
        List<String> errors = model.getErrors();
        List<String> warnings = model.getWarnings();

        for (String error : errors) {
            addBuildMarkers(error, IMarker.SEVERITY_ERROR);
        }
        for (String warning : warnings) {
            addBuildMarkers(warning, IMarker.SEVERITY_WARNING);
        }
        for (String error : classpathErrors) {
            addClasspathMarker(error, IMarker.SEVERITY_ERROR);
        }

        if (!validationResults.isOK()) {
            for (IStatus status : validationResults.getChildren()) {
                addBuildMarkers(status);
            }
        }
    }

    private void clearBuildMarkers() throws CoreException {
        getProject().deleteMarkers(BndtoolsConstants.MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
    }

    private void addBuildMarkers(IStatus status) throws Exception {
        if (status.isMultiStatus()) {
            for (IStatus child : status.getChildren()) {
                addBuildMarkers(child);
            }
            return;
        }

        addBuildMarkers(status.getMessage(), iStatusSeverityToIMarkerSeverity(status));
    }

    private void addBuildMarkers(String message, int severity) throws Exception {
        Location location = model != null ? model.getLocation(message) : null;
        if (location != null) {
            String type = location.details != null ? location.details.getClass().getName() : null;
            BuildErrorDetailsHandler handler = BuildErrorDetailsHandlers.INSTANCE.findHandler(type);

            List<MarkerData> markers = handler.generateMarkerData(getProject(), model, location);
            for (MarkerData markerData : markers) {
                IResource resource = markerData.getResource();
                //
                // TODO pkr: Just fixed an NPE java.lang.NullPointerException
                //                at org.bndtools.builder.NewBuilder.addBuildMarkers(NewBuilder.java:822)
                //                at org.bndtools.builder.NewBuilder.createBuildMarkers(NewBuilder.java:783)
                //                at org.bndtools.builder.NewBuilder.rebuild(NewBuilder.java:637)
                //                at org.bndtools.builder.NewBuilder.rebuildIfLocalChanges(NewBuilder.java:481)
                //                at org.bndtools.builder.NewBuilder.build(NewBuilder.java:184)
                //                at org.eclipse.core.internal.events.BuildManager$2.run(BuildManager.java:734)
                //                at org.eclipse.core.runtime.SafeRunner.run(SafeRunner.java:42)
                //                at org.eclipse.core.internal.events.BuildManager.basicBuild(BuildManager.java:206)
                //                at org.eclipse.core.internal.events.BuildManager.basicBuild(BuildManager.java:246)
                //                at org.eclipse.core.internal.events.BuildManager$1.run(BuildManager.java:299)
                //                at org.eclipse.core.runtime.SafeRunner.run(SafeRunner.java:42)
                //                at org.eclipse.core.internal.events.BuildManager.basicBuild(BuildManager.java:302)
                //                at org.eclipse.core.internal.events.BuildManager.basicBuildLoop(BuildManager.java:358)
                //                at org.eclipse.core.internal.events.BuildManager.build(BuildManager.java:381)
                //                at org.eclipse.core.internal.events.AutoBuildJob.doBuild(AutoBuildJob.java:143)
                //                at org.eclipse.core.internal.events.AutoBuildJob.run(AutoBuildJob.java:241)
                //                at org.eclipse.core.internal.jobs.Worker.run(Worker.java:54)
                if (resource != null) {
                    IMarker marker = resource.createMarker(BndtoolsConstants.MARKER_BND_PROBLEM);
                    marker.setAttribute(IMarker.SEVERITY, severity);
                    marker.setAttribute("$bndType", type);
                    marker.setAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, markerData.hasResolutions());
                    for (Entry<String,Object> attrib : markerData.getAttribs().entrySet())
                        marker.setAttribute(attrib.getKey(), attrib.getValue());
                }
            }
        } else {
            IMarker marker = DefaultBuildErrorDetailsHandler.getDefaultResource(getProject()).createMarker(BndtoolsConstants.MARKER_BND_PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, severity);
            marker.setAttribute(IMarker.MESSAGE, message);
        }
    }

    private void addClasspathMarker(String message, int severity) throws CoreException {
        IResource resource = DefaultBuildErrorDetailsHandler.getDefaultResource(getProject());

        IMarker marker = resource.createMarker(BndtoolsConstants.MARKER_BND_CLASSPATH_PROBLEM);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.MESSAGE, message);
    }

    private int iStatusSeverityToIMarkerSeverity(IStatus status) {
        int severity;
        switch (status.getSeverity()) {
        case IStatus.CANCEL :
        case IStatus.ERROR :
            severity = IMarker.SEVERITY_ERROR;
            break;
        case IStatus.WARNING :
            severity = IMarker.SEVERITY_WARNING;
            break;
        default :
            severity = IMarker.SEVERITY_INFO;
        }

        return severity;
    }

    //    private void addClasspathMarker(IStatus status) throws CoreException {
    //        if (status.isMultiStatus()) {
    //            for (IStatus child : status.getChildren()) {
    //                addClasspathMarker(child);
    //            }
    //            return;
    //        }
    //
    //        addClasspathMarker(status.getMessage(), iStatusSeverityToIMarkerSeverity(status));
    //    }

    private void log(int level, String message, Object... args) {
        if (logLevel >= level)
            buildLog.add(String.format(message, args));
    }

    private static class ProjectDeltaVisitor implements IResourceDeltaVisitor {

        final Project model;
        final Set<File> changedFiles;
        final IPath targetDirFullPath;
        final IPath bin;

        boolean force = false;

        ProjectDeltaVisitor(final IProject project, final Set<File> changedFiles) throws Exception {
            this.changedFiles = changedFiles;
            this.model = Central.getProject(project.getLocation().toFile());
            if (this.model == null) {
                this.targetDirFullPath = null;
                this.bin = null;
                return;
            }

            this.targetDirFullPath = project.getFullPath().append(calculateTargetDirPath(model));
            this.bin = targetDirFullPath.removeLastSegments(1).append(model.getOutput().getName());
        }

        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {
            if (targetDirFullPath == null) {
                return false;
            }
            if (!isChangeDelta(delta))
                return false;

            IResource resource = delta.getResource();
            if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
                return true;

            if (resource.getType() == IResource.FOLDER) {
                IPath folderPath = resource.getFullPath();

                // #860

                if (folderPath.equals(bin)) {
                    //
                    // Any changes in bin should result in a rebuild of any dependent
                    // project.
                    // TODO We could of course optimize to see if this represents
                    // an exported package?
                    //
                    force = true;
                    return false;
                }
                // ignore ALL files in target dir
                return !folderPath.equals(targetDirFullPath);
            }

            if (resource.getType() == IResource.FILE) {
                File file = resource.getLocation().toFile();
                changedFiles.add(file);
            }

            return false;
        }
    }

    /*
     * We can now decorate based on the build we just did.
     */
    private void decorate() throws Exception {

        //
        // Calculate the source path resources
        //

        File projectBaseFile = getProject().getLocation().toFile().getAbsoluteFile();
        Collection<File> modelSourcePaths = model.getSourcePath();
        Collection<IResource> modelSourcePathsResources = null;
        if (modelSourcePaths != null && !modelSourcePaths.isEmpty()) {
            modelSourcePathsResources = new HashSet<IResource>();
            for (File modelSourcePath : modelSourcePaths) {
                if (projectBaseFile.equals(modelSourcePath.getAbsoluteFile())) {
                    continue;
                }
                IResource modelSourcePathResource = FileUtils.toProjectResource(getProject(), modelSourcePath);
                if (modelSourcePathResource != null) {
                    modelSourcePathsResources.add(modelSourcePathResource);
                }
            }
        }

        //
        // Gobble up the information for exports and contained
        //

        Map<String,SortedSet<Version>> allExports = new HashMap<String,SortedSet<Version>>();
        Set<String> allContained = new HashSet<String>();

        //
        // First the exports
        //

        for (Map.Entry<PackageRef,Attrs> entry : model.getExports().entrySet()) {
            String v = entry.getValue().getVersion();
            Version version = v == null ? Version.emptyVersion : new Version(v);
            allExports.put(entry.getKey().getFQN(), new SortedList<Version>(version));
        }

        for (Map.Entry<PackageRef,Attrs> entry : model.getContained().entrySet()) {
            allContained.add(entry.getKey().getFQN());
        }

        Central.setProjectPackageModel(getProject(), allExports, allContained, modelSourcePathsResources);

        Display display = PlatformUI.getWorkbench().getDisplay();
        SWTConcurrencyUtil.execForDisplay(display, true, new Runnable() {
            @Override
            public void run() {
                PlatformUI.getWorkbench().getDecoratorManager().update("bndtools.packageDecorator");
            }
        });
    }

}
