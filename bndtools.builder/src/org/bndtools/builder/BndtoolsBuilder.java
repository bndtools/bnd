package org.bndtools.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.builder.classpath.BndContainerInitializer;
import org.bndtools.builder.decorator.ui.PackageDecorator;
import org.bndtools.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import aQute.bnd.build.Project;
import aQute.lib.io.IO;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;
import bndtools.preferences.CompileErrorAction;

/**
 * This a Builder for bndtools.It will use the bnd project/workspace model to incrementally build bundles. This is a
 * rewrite of the NewBuilder. Left out are the Eclipse classpath include in a build option.
 *
 * <pre>
 *  create project test with 1 class
 *  Clean
 *  Add non-existent entry on -buildpath, should not build
 *  Remove non-existing entry, should remove errors and build
 *  delete output file, should rebuild
 *  create compile error -> no build
 *  remove compile error -> build again
 *  add Foo=foo to build.bnd -> check in test.jar
 *  add include bar.bnd to build.bnd, add Bar=bar -> check in test.jar
 *  touch bar.bnd -> see if manifest is updated in JAR (Jar viewer does not refresh very well, so reopen)
 *  touch build.bnd -> verify rebuild
 *  touch bnd.bnd in test -> verify rebuild
 *
 *  create project test.2, add -buildpath: test
 * </pre>
 */
public class BndtoolsBuilder extends IncrementalProjectBuilder {
    public static final String PLUGIN_ID = "bndtools.builder";
    public static final String BUILDER_ID = BndtoolsConstants.BUILDER_ID;
    private static final ILogger logger = Logger.getLogger(BndtoolsBuilder.class);
    static final Set<Project> dirty = Collections.newSetFromMap(new ConcurrentHashMap<Project,Boolean>());

    static {
        CnfWatcher.install();
    }

    private Project model;
    private BuildLogger buildLog;
    private IProject[] dependsOn;
    private boolean postponed;

    /**
     * Called from Eclipse when it thinks this project should be build. We're proposed to figure out if we've changed
     * and then build as quickly as possible.
     * <p>
     * We ensure we're called in proper order defined by bnd, if not we will make it be called in proper order.
     *
     * @param kind
     * @param args
     * @param monitor
     * @return List of projects we depend on
     */
    @Override
    protected IProject[] build(final int kind, Map<String,String> args, final IProgressMonitor monitor) throws CoreException {

        BndPreferences prefs = new BndPreferences();
        buildLog = new BuildLogger(prefs.getBuildLogging());

        final BuildListeners listeners = new BuildListeners();

        final IProject myProject = getProject();
        try {

            listeners.fireBuildStarting(myProject);

            final MarkerSupport markers = new MarkerSupport(myProject);

            //
            // First time after a restart
            //

            if (dependsOn == null) {
                dependsOn = myProject.getDescription().getDynamicReferences();
            }

            if (model == null) {
                try {
                    model = Central.getProject(myProject);
                } catch (Exception e) {
                    markers.deleteMarkers("*");
                }
                if (model == null)
                    return noreport();
            }

            try {
                return Central.bndCall(new Callable<IProject[]>() {
                    @Override
                    public IProject[] call() throws Exception {
                        boolean force = kind == FULL_BUILD || kind == CLEAN_BUILD;
                        model.clear();

                        DeltaWrapper delta = new DeltaWrapper(model, getDelta(myProject), buildLog);

                        boolean setupChanged = false;

                        if (!postponed && (delta.havePropertiesChanged(model) || delta.hasChangedSubbundles())) {
                            buildLog.basic("project was dirty from changed bnd files postponed = " + postponed);
                            model.forceRefresh();
                            setupChanged = true;
                        }

                        if (dirty.remove(model)) {
                            buildLog.basic("project was dirty from a workspace refresh postponed = " + postponed);
                            setupChanged = true && !postponed;
                        }

                        if (!force && !setupChanged && delta.hasEclipseChanged()) {
                            buildLog.basic("Eclipse project had a buildpath change");
                            setupChanged = true;
                        }

                        if (!force && !setupChanged && suggestClasspathContainerUpdate()) {
                            buildLog.basic("Project classpath may need to be updated");
                            setupChanged = true;
                        }

                        //
                        // If we already know we are going to build, we
                        // must handle the path errors. We make sure
                        // prepare() is called so we get any build errors.
                        //

                        if (force || setupChanged) {
                            model.setChanged();
                            model.setDelayRunDependencies(true);
                            model.prepare();

                            markers.validate(model);
                            markers.setMarkers(model, BndtoolsConstants.MARKER_BND_PATH_PROBLEM);
                            model.clear();

                            dependsOn = calculateDependsOn(model);

                            //
                            // We have a setup change so we MUST check both class path
                            // changes and build order changes. Careful not to use an OR
                            // operation (as I did) because they are shortcutted. Since it
                            // is also nice to see why we had a change, we just collect the
                            // reason of the change so we can report it to the log.
                            //

                            String changed = ""; // if empty, no change
                            String del = "";
                            if (requestClasspathContainerUpdate()) {
                                changed += "Classpath container updated";
                                del = " & ";
                            }

                            if (setBuildOrder(monitor)) {
                                changed += del + "Build order changed";
                            }

                            if (!changed.equals("")) {
                                buildLog.basic("Setup changed: " + changed);
                                return postpone();
                            }

                            force = true;
                        }

                        //
                        // We did not postpone, so reset the flag
                        //
                        if (postponed)
                            buildLog.full("Was postponed");

                        force |= postponed;
                        postponed = false;

                        if (!force && delta.hasProjectChanged()) {
                            buildLog.basic("project had changed files");
                            force = true;
                        }

                        if (!force && hasUpstreamChanges()) {
                            buildLog.basic("project had upstream changes");
                            force = true;
                        }

                        if (!force && delta.hasNoTarget(model)) {
                            buildLog.basic("project has no target files");
                            force = true;
                        }

                        //
                        // If we're not forced to build at this point
                        // then we have an incremental build and
                        // no reason to rebuild.
                        //

                        if (!force) {
                            buildLog.full("Auto/Incr. build, no changes detected");
                            return noreport();
                        }

                        if (model.isNoBundles()) {
                            buildLog.basic("-nobundles was set, so no build");
                            buildLog.setFiles(0);
                            return report(markers);
                        }

                        if (markers.hasBlockingErrors(delta)) {
                            CompileErrorAction actionOnCompileError = getActionOnCompileError();
                            if (actionOnCompileError != CompileErrorAction.build) {
                                if (actionOnCompileError == CompileErrorAction.delete) {
                                    buildLog.basic("Blocking errors, delete build files, quit");
                                    deleteBuildFiles(model);
                                    model.error("Will not build project %s until the compilation and/or path problems are fixed, output files are deleted.", myProject.getName());
                                } else {
                                    buildLog.basic("Blocking errors, leave old build files, quit");
                                    model.error("Will not build project %s until the compilation and/or path problems are fixed, output files are kept.", myProject.getName());
                                }
                                return report(markers);
                            }
                            buildLog.basic("Blocking errors, continuing anyway");
                            model.warning("Project %s has blocking errors but requested to continue anyway", myProject.getName());
                        }

                        deleteBuildFiles(model);
                        Central.invalidateIndex();

                        File buildFiles[] = model.build();

                        if (buildFiles != null) {
                            listeners.updateListeners(buildFiles, myProject);
                            buildLog.setFiles(buildFiles.length);
                        }

                        // We can now decorate based on the build we just did.
                        PackageDecorator.updateDecoration(myProject, model);

                        if (model.isCnf()) {
                            model.getWorkspace().refresh(); // this is for bnd plugins built in cnf
                        }

                        return report(markers);
                    }
                }, monitor);
            } catch (TimeoutException | InterruptedException e) {
                logger.logWarning("Unable to build project " + myProject.getName(), e);
                return postpone();
            }
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Build Error!", e));
        } finally {
            if (buildLog.isActive())
                logger.logInfo(buildLog.toString(myProject.getName()), null);
            listeners.release(myProject);
        }
    }

    private IProject[] noreport() {
        return dependsOn;
    }

    private IProject[] report(MarkerSupport markers) throws Exception {
        markers.setMarkers(model, BndtoolsConstants.MARKER_BND_PROBLEM);
        return dependsOn;
    }

    private IProject[] postpone() {
        postponed = true;
        rememberLastBuiltState();
        return dependsOn;
    }

    /**
     * Clean the output and target directories
     */
    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        try {
            IProject myProject = getProject();
            final Project model;
            try {
                model = Central.getProject(myProject);
            } catch (Exception e) {
                MarkerSupport markers = new MarkerSupport(myProject);
                markers.deleteMarkers("*");
                markers.createMarker(null, IMarker.SEVERITY_ERROR, "Cannot find bnd project", BndtoolsConstants.MARKER_BND_PATH_PROBLEM);
                return;
            }
            if (model == null)
                return;

            try {
                Central.bndCall(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        model.clean();
                        return null;
                    }
                }, monitor);
            } catch (TimeoutException | InterruptedException e) {
                logger.logWarning("Unable to clean project " + myProject.getName(), e);
                return;
            }

            // Tell Eclipse what we did...
            IFolder targetFolder = myProject.getFolder(calculateTargetDirPath(model));
            targetFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Build Error!", e));
        }
    }

    /*
     * Check if any of the projects of which we depend has changes.
     * We use the generated/buildfiles as the marker.
     */
    private boolean hasUpstreamChanges() throws Exception {

        for (IProject upstream : dependsOn) {
            if (!upstream.exists())
                continue;

            Project up = Central.getProject(upstream);
            if (up == null)
                continue;

            IResourceDelta delta = getDelta(upstream);
            DeltaWrapper dw = new DeltaWrapper(up, delta, buildLog);
            if (dw.hasBuildfile()) {
                buildLog.full("Upstream project %s changed", up);
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the classpath container this project needs to be updated.
     *
     * @return {@code true} if this project has a bnd classpath container and a classpath update should be requested.
     *         {@code false} if this project does not have a bnd classpath container or a classpath update does not need
     *         to be requested.
     */
    private boolean suggestClasspathContainerUpdate() throws Exception {
        IJavaProject javaProject = JavaCore.create(getProject());
        if (javaProject == null) {
            return false; // project is not a java project
        }

        return BndContainerInitializer.suggestClasspathContainerUpdate(javaProject);
    }

    /**
     * Request the classpath container be updated for this project.
     * <p>
     * The classpath container may have added errors to the model which the caller must check for.
     *
     * @return {@code true} if this project has a bnd classpath container and the classpath was changed. {@code false}
     *         if this project does not have a bnd classpath container or the classpath was not changed.
     */
    private boolean requestClasspathContainerUpdate() throws CoreException {
        IJavaProject javaProject = JavaCore.create(getProject());
        if (javaProject == null) {
            return false; // project is not a java project
        }

        IClasspathContainer oldContainer = BndContainerInitializer.getClasspathContainer(javaProject);
        if (oldContainer == null) {
            return false; // project does not have a BndContainer
        }

        BndContainerInitializer.requestClasspathContainerUpdate(javaProject);

        return oldContainer != BndContainerInitializer.getClasspathContainer(javaProject);
    }

    /*
     * Set the project's dependencies to influence the build order for Eclipse.
     */
    private boolean setBuildOrder(IProgressMonitor monitor) throws Exception {
        try {
            IProjectDescription projectDescription = getProject().getDescription();
            IProject[] older = projectDescription.getDynamicReferences();
            if (Arrays.equals(dependsOn, older))
                return false;
            projectDescription.setDynamicReferences(dependsOn);
            getProject().setDescription(projectDescription, monitor);
            buildLog.full("Changed the build order to %s", Arrays.toString(dependsOn));
        } catch (Exception e) {
            logger.logError("Failed to set build order", e);
        }
        return true;
    }

    private void deleteBuildFiles(Project model) throws Exception {
        File[] buildFiles = model.getBuildFiles(false);
        if (buildFiles != null)
            for (File f : buildFiles) {
                if (f != null)
                    IO.delete(f);
            }
        IO.delete(new File(model.getTarget(), Project.BUILDFILES));
    }

    private static IPath calculateTargetDirPath(Project model) throws Exception {
        IPath basePath = Path.fromOSString(model.getBase().getAbsolutePath());
        final IPath targetDirPath = Path.fromOSString(model.getTarget().getAbsolutePath()).makeRelativeTo(basePath);
        return targetDirPath;
    }

    private IProject[] calculateDependsOn(Project model) throws Exception {
        Collection<Project> dependsOn = model.getDependson();

        IWorkspaceRoot wsroot = getProject().getWorkspace().getRoot();

        List<IProject> result = new ArrayList<IProject>(dependsOn.size() + 1);

        IProject cnfProject = WorkspaceUtils.findCnfProject(wsroot, model.getWorkspace());
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

        buildLog.full("Calculated dependsOn list: %s", result);
        return result.toArray(new IProject[0]);
    }

    private CompileErrorAction getActionOnCompileError() {
        ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()), BndtoolsConstants.CORE_PLUGIN_ID);
        return CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY));
    }

}
