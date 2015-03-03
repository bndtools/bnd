package org.bndtools.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.builder.classpath.BndContainerInitializer;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.bndtools.utils.workspace.FileUtils;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Version;

import aQute.bnd.build.Project;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.lib.collections.SortedList;
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
    private IProject dependsOn[];
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
    protected IProject[] build(int kind, Map<String,String> args, IProgressMonitor monitor) throws CoreException {

        BndPreferences prefs = new BndPreferences();
        buildLog = new BuildLogger(prefs.getBuildLogging());

        CompileErrorAction actionOnCompileError = getActionOnCompileError();

        BuildListeners listeners = new BuildListeners();
        int files = -1;

        try {

            IProject myProject = getProject();
            MarkerSupport markers = new MarkerSupport(myProject);

            boolean force = kind == FULL_BUILD || kind == CLEAN_BUILD;

            //
            // First time after a restart
            //

            if (dependsOn == null) {
                dependsOn = getProject().getDescription().getDynamicReferences();
            }

            if (model == null) {
                try {
                    model = Central.getProject(myProject.getLocation().toFile());
                    if (model == null)
                        return dependsOn;

                } catch (Exception e) {
                    markers.deleteMarkers("*");
                    markers.createMarker(null, IMarker.SEVERITY_ERROR, e.getMessage(), BndtoolsConstants.MARKER_BND_PROBLEM);
                    return dependsOn;
                }
            }

            model.clear();

            listeners.fireBuildStarting(myProject);

            DeltaWrapper delta = new DeltaWrapper(model, getDelta(myProject), buildLog);

            boolean setupChanged = false;

            if (!postponed && delta.havePropertiesChanged(model)) {
                buildLog.basic("project was dirty from changed bnd files postponed = " + postponed);
                model.forceRefresh();
                setupChanged = true;
            }

            if (dirty.remove(model)) {
                buildLog.basic("project was dirty from a workspace refresh postponed = " + postponed);
                setupChanged = true && !postponed;
            }

            //
            // If we already know we are going to build, we
            // must handle the path errors. We make sure
            // prepare() is called so we get any build errors.
            // Since we do not want to do this every time in a auto
            // build we store these errors in a field and remove them.
            //

            if (force || setupChanged) {

                //
                // Check if the last action postponed
                // and this is less than a second ago. In that
                // case we assume we already refreshed everything
                //

                model.setChanged();
                model.setDelayRunDependencies(true);
                model.prepare();

                markers.setMarkers(model, BndtoolsConstants.MARKER_BND_PATH_PROBLEM);
                model.clear();

                dependsOn = calculateDependsOn(model);

                if (setBuildOrder(dependsOn)) {
                    buildLog.basic("Build order changed, postponing");
                    return postpone(dependsOn);
                }

                if (checkClasspathContainerUpdate(myProject)) {
                    // likely causes a recompile
                    buildLog.basic("classpaths were changed, postponing");
                    return postpone(dependsOn);
                }

                force = true;
            }

            //
            // We did not postpone, so reset the flag
            //
            force |= postponed;
            postponed = false;

            markers.validate(model);

            if (!force && delta.hasProjectChanged()) {
                buildLog.basic("project had changed files");
                force = true;
            }

            if (!force && hasUpstreamChanges(dependsOn)) {
                buildLog.basic("project had upstream changes");
                force = true;
            }

            if (!force && delta.hasNoTarget(model)) {
                buildLog.basic("project has no target files");
                force = true;
            }

            //
            // If we're not forced to build at this point
            // then we have an incremental builf and
            // no reason to rebuild. We then just report
            // any errors so far.
            //

            if (!force)
                return dependsOn;

            if (model.isNoBundles()) {
                buildLog.basic("-nobundles was set, so no build");
                files = 0;
                return report(dependsOn, markers);
            }

            if (markers.hasBlockingErrors(delta)) {
                if (actionOnCompileError != CompileErrorAction.build) {
                    if (actionOnCompileError == CompileErrorAction.delete) {
                        buildLog.basic("Blocking errors, delete build files, quit");
                        deleteBuildFiles(model);
                        model.error("Will not build project %s until the compilation and/or path problems are fixed but deleted the output files.", model.getName());
                    } else {
                        buildLog.basic("Blocking errors, quit leaving old files there");
                        model.error("Will not build project %s until the compilation and/or path problems are fixed, output files are kept", model.getName());
                    }
                    return report(dependsOn, markers);
                }
                buildLog.basic("Blocking errors, continuing anyway");
                model.warning("Project %s has blocking errors but requested to continue anyway", model.getName());
            }

            deleteBuildFiles(model);
            Central.invalidateIndex();

            File buildFiles[] = model.build();

            if (buildFiles != null) {
                listeners.updateListeners(buildFiles, myProject);
                files = buildFiles.length;
            }

            decorate(model);

            if (model.isCnf()) {
                model.getWorkspace().refresh(); // this is for bnd plugins built in cnf
            }

            return report(dependsOn, markers);

        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Build Error!", e));
        } finally {
            if (!buildLog.isEmpty())
                logger.logInfo(buildLog.toString(model, files), null);
            listeners.release();
        }
    }

    private IProject[] report(IProject[] dependsOn, MarkerSupport markers) throws Exception {
        markers.setMarkers(model, BndtoolsConstants.MARKER_BND_PROBLEM);
        return dependsOn;
    }

    private IProject[] postpone(IProject[] dependsOn) {
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
            Project model = Central.getProject(myProject.getLocation().toFile());
            if (model == null)
                return;

            model.clean();

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
    private boolean hasUpstreamChanges(IProject[] upstreams) throws Exception {

        for (IProject upstream : upstreams) {
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
     * Check if the classpath has changed for this project.
     * <p>
     * The classpath container may have added errors to the model which the caller must check for.
     *
     * @param project
     *            The IProject to check and update the classpath container.
     * @return {@code true} if the specified project has a bnd classpath container and the classpath was changed.
     *         {@code false} if the specified project does not have a bnd classpath container or the classpath was not
     *         changed.
     */
    private boolean checkClasspathContainerUpdate(IProject project) throws CoreException {
        IJavaProject javaProject = JavaCore.create(project);
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
    private boolean setBuildOrder(IProject[] newer) throws Exception {
        try {
            IProjectDescription projectDescription = getProject().getDescription();
            IProject[] older = projectDescription.getDynamicReferences();
            if (Arrays.equals(newer, older))
                return false;
            projectDescription.setDynamicReferences(newer);
            getProject().setDescription(projectDescription, null);
            buildLog.full("Changed the build order to %s", Arrays.toString(newer));

        } catch (Exception e) {
            logger.logError("Failed to set build order", e);
        }
        return true;
    }

    private void deleteBuildFiles(Project model) throws Exception {
        File[] buildFiles = model.getBuildFiles();
        if (buildFiles != null)
            for (File f : buildFiles)
                IO.delete(f);
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

        buildLog.full("Calculated depends-on list: %s", result);
        return result.toArray(new IProject[result.size()]);
    }

    /*
     * We can now decorate based on the build we just did.
     */
    private void decorate(Project model) throws Exception {

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

    private CompileErrorAction getActionOnCompileError() {
        ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()), BndtoolsConstants.CORE_PLUGIN_ID);
        return CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY));
    }

}
