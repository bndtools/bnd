package org.bndtools.builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
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

public class NotYetDoneBuilder extends IncrementalProjectBuilder {

    public static final String PLUGIN_ID = "bndtools.builder";
    public static final String BUILDER_ID = BndtoolsConstants.BUILDER_ID;

    private static final ILogger logger = Logger.getLogger(NotYetDoneBuilder.class);

    static final int LOG_FULL = 2;
    static final int LOG_BASIC = 1;
    static final int LOG_NONE = 0;

    private Project model;
    private BuildListeners listeners;
    private Collection< ? extends Builder> subBuilders;
    private List<String> classpathErrors;
    private MultiStatus validationResults;
    private List<String> buildLog;
    private int logLevel = LOG_NONE;
    private ScopedPreferenceStore projectPrefs;
    private int nrFilesBuilt = 0;

    /**
     * Called from Eclipse when it thinks this project should be build. We're proposed to figure out if we've changed
     * and then build as quickly as possible.
     * <p>
     * We ensure we're called in proper order defined by bnd, if not we will make it be called in proper order.
     *
     * @param kind
     * @param args
     * @param monitor
     * @return
     * @throws CoreException
     */
    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {

        //
        // Reset any variables we'll use now
        //

        prepare();

        try {

            // Get the initial project
            IProject myProject = getProject();
            listeners.fireBuildStarting(myProject);
            //  reportDelta(myProject);

            model = getModel(myProject);
            if (model == null)
                return null;

            //
            // We should not report any run dependencies
            // here
            //

            model.setDelayRunDependencies(true);
            model.clear();

            //
            // Calculate what we depend on
            //

            IProject[] dependsOn = calculateDependsOn(model);

            //
            // Make sure we're building in the right order
            // This will tell eclipse to call in the right
            // order
            //

            if (setBuildOrder(model)) {

                //
                // We changed the build order so we want to
                // return and get called back later
                //

                super.rememberLastBuiltState();
                return dependsOn;
            }

            boolean rebuild = false;

            //
            // If we're cnf, we're the first project
            // if we're changed, we refresh all our projects
            //

            DeltaWrapper delta = new DeltaWrapper(model, getDelta(myProject));

            if (model.isCnf() && delta.hasCnfChanged())
                doCnfDirty();

            //
            // Check if we need to change the classpath.
            //

            resetClasspathContainer(myProject);

            if (model.isNoBundles())
                return dependsOn;

            switch (kind) {
            case FULL_BUILD :
                log(LOG_BASIC, "Eclipse requested full build");
                build(model);
                break;

            case AUTO_BUILD :
            case INCREMENTAL_BUILD :
                log(LOG_BASIC, "Eclipse requested auto/incremental build");
                if (delta.hasProjectChanged()) {
                    build(model);
                }
                break;
            }

            return dependsOn;
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Build Error!", e));
        } finally {
            flush();
        }
    }

    /*
     * The cnf project is dirty so we need to invalidate the workspace
     */
    private void doCnfDirty() throws Exception {
        log(LOG_BASIC, "cnf changed, invalidating workspace");
        model.refresh();

        for (Project p : model.getWorkspace().getAllProjects()) {
            p.refresh();
        }
        refreshRepositories();
    }

    /*
     * Flush the log and notify listeners
     */
    private void flush() {
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

    private void resetClasspathContainer(IProject myProject) throws CoreException {
        if (BndContainerInitializer.resetClasspaths(model, myProject, classpathErrors)) {
            log(LOG_BASIC, "classpaths were changed");
        } else {
            log(LOG_FULL, "classpaths did not need to change");
        }
    }

    private void refreshRepositories() throws Exception {
        List<RepositoryListenerPlugin> repoListeners = Central.getWorkspace().getPlugins(RepositoryListenerPlugin.class);
        for (RepositoryListenerPlugin repoListener : repoListeners) {
            repoListener.repositoriesRefreshed();
        }
    }

    private Project getModel(IProject myProject) throws CoreException, Exception {
        Project model = null;
        try {
            model = Central.getProject(myProject.getLocation().toFile());
        } catch (Exception e) {
            clearBuildMarkers();
            addBuildMarkers(e.getMessage(), IMarker.SEVERITY_ERROR);
        }
        return model;
    }

    /*/
    /* Called at the start of the build
    /*/

    private void prepare() {
        //
        // We have a few preferences for the build, let's get them
        //

        BndPreferences prefs = new BndPreferences();
        logLevel = prefs.getBuildLogging();
        projectPrefs = new ScopedPreferenceStore(new ProjectScope(getProject()), BndtoolsConstants.CORE_PLUGIN_ID);

        classpathErrors = new LinkedList<String>();
        validationResults = new MultiStatus(PLUGIN_ID, 0, "Validation errors in bnd project", null);
        buildLog = new ArrayList<String>(5);

        nrFilesBuilt = 0;

        // Prepare build listeners and build error handlers

        listeners = new BuildListeners();
    }

    /**
     * Calculate the order for the bnd workspace and set this as the build order for Eclise.
     */
    private boolean setBuildOrder(Project model) throws Exception {
        try {
            IWorkspace eclipseWs = getProject().getWorkspace();
            IWorkspaceDescription description = eclipseWs.getDescription();
            String[] older = description.getBuildOrder();

            Workspace ws = model.getWorkspace();
            Collection<Project> buildOrder = ws.getBuildOrder();
            if (isSame(buildOrder, older))
                return false;

            String[] newer = new String[buildOrder.size() + 1];
            int n = 1;
            newer[0] = "cnf";

            for (Project p : buildOrder) {
                newer[n++] = p.getName();
            }

            description.setBuildOrder(newer);
            eclipseWs.setDescription(description);
            log(LOG_FULL, "Changed the build order to " + buildOrder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean isSame(Collection<Project> buildOrder, String[] older) {
        if (buildOrder == null || older == null || buildOrder.size() != older.length)
            return false;

        int n = 0;
        for (Project p : buildOrder) {
            if (!p.getName().equals(older[n++]))
                return false;
        }
        return true;
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

    private enum Action {
        build, delete, skip
    }

    /**
     * @return Whether any files were built
     */
    private boolean build(Project model) throws Exception {
        File[] buildFiles = model.getBuildFiles();
        if (buildFiles != null)
            for (File f : buildFiles)
                IO.delete(f);

        model.clear();
        clearBuildMarkers();

        // Check if compilation errors exist, and if so check the project
        // settings for what to do about that...
        switch (inCaseOfErrors()) {
        case skip :
        case delete :
            return false;

        default :
        case build :
        }

        validate();

        getExtraClasspathEntriesFromEclipse(model);

        buildFiles = model.build();
        return true;

    }

    private void getExtraClasspathEntriesFromEclipse(Project model) throws JavaModelException {
        // Load Eclipse classpath containers
        EclipseClasspathPreference classpathPref = EclipseClasspathPreference.parse(projectPrefs.getString(EclipseClasspathPreference.PREFERENCE_KEY));
        if (classpathPref == EclipseClasspathPreference.expose) {
            model.clearClasspath();
            List<File> classpathFiles = new ArrayList<File>(20);
            accumulateClasspath(classpathFiles, JavaCore.create(getProject()), false, new ClasspathContainerFilter());
            for (File file : classpathFiles) {
                log(LOG_FULL, "Adding Eclipse classpath entry %s", file.getAbsolutePath());
                model.addClasspath(file);
            }
        }
    }

    private void validate() {
        List<IValidator> validators = loadValidators();
        if (validators != null) {
            for (Builder builder : subBuilders) {
                validate(builder, validators);
            }
        }
    }

    private Action inCaseOfErrors() throws Exception {
        if (hasBlockingErrors()) {
            ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()), BndtoolsConstants.CORE_PLUGIN_ID);
            switch (CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY))) {
            case skip :
            default :
                addBuildMarkers(String.format("Will not build OSGi bundle(s) for project %s until compilation problems are fixed.", model.getName()), IMarker.SEVERITY_ERROR);
                log(LOG_BASIC, "SKIPPING due to Java problem markers");
                return Action.skip;

            case build :
                return Action.build;

            case delete :
                return Action.delete;
            }
        } else if (!classpathErrors.isEmpty()) {
            ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()), BndtoolsConstants.CORE_PLUGIN_ID);
            switch (CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY))) {
            case skip :
            default :
                addBuildMarkers(String.format("Will not build OSGi bundle(s) for project %s until classpath resolution problems are fixed.", model.getName()), IMarker.SEVERITY_ERROR);
                log(LOG_BASIC, "SKIPPING due to classpath resolution problem markers");
                return Action.skip;
            case build :
                return Action.build;

            case delete :
                return Action.delete;
            }
        }
        return null;
    }

    private static IPath calculateTargetDirPath(Project model) throws Exception {
        IPath basePath = Path.fromOSString(model.getBase().getAbsolutePath());
        final IPath targetDirPath = Path.fromOSString(model.getTarget().getAbsolutePath()).makeRelativeTo(basePath);
        return targetDirPath;
    }

    /**
     * @param force
     *            Whether to force bnd to build
     * @return Whether any files were built
     */
    @SuppressWarnings("unchecked")
    private boolean rebuild(boolean force) throws Exception {

        File[] built;

        // Clear errors & warnings before build
        model.clear();

        //if (buildAction == Action.build) {
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
        //  } else {
        // Delete target files since the project has compile errors and the
        // delete action was selected.
        for (Builder builder : subBuilders) {
            File targetFile = new File(model.getTarget(), builder.getBsn() + ".jar");
            boolean deleted = targetFile.delete();
            log(LOG_FULL, "deleted target file %s (%b)", targetFile, deleted);
        }
        built = new File[0];
        //   }

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

        //System.out.println(model + " transitively dependsOn " + dependsOn);

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();

        //
        // The model maintains the dependencies transitively. However,
        // for Eclipse it seems better to keep them the direct level.
        // However, we also have the case that we depend on B & C but B also depends
        // on C. Than we should not depend on C since our dep on C will cause it to be build.
        // So the following calculates the direct deps - the direct deps that are also
        // deps of our transitive deps. Still here? It is actually simpler than
        // it sounds. We just remove all the deps of our deps ...
        //

        for (Project p : new ArrayList<Project>(dependsOn)) {
            if (p == model)
                continue;

            Collection<Project> sub = p.getDependson();
            dependsOn.removeAll(sub);
        }

        //System.out.println(model + " direct dependsOn " + dependsOn);

        List<IProject> result = new ArrayList<IProject>(dependsOn.size() + 1);

        IProject cnfProject = WorkspaceUtils.findCnfProject();
        if (cnfProject != null) {
            result.add(cnfProject);
        }

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

    void log(int level, String message, Object... args) {
        if (logLevel >= level)
            buildLog.add(String.format(message, args));
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
