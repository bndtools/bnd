package org.bndtools.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

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
 * rewrite of the NewBuilder. Left out are the Eclipse classpath include in a build option and being able to continue
 * even when there are errors.
 */
public class FutureBuilder extends IncrementalProjectBuilder {

    public static final String PLUGIN_ID = "bndtools.builder";
    public static final String BUILDER_ID = BndtoolsConstants.BUILDER_ID;

    private static final ILogger logger = Logger.getLogger(FutureBuilder.class);
    private static final Set<Project> dirty = new HashSet<Project>();
    private static CnfWatcher cnfWatcher = CnfWatcher.install();

    private Project model;
    private BuildLogger buildLog;
    private int revision;

    /**
     * Called from Eclipse when it thinks this project should be build. We're proposed to figure out if we've changed
     * and then build as quickly as possible.
     * <p>
     * We ensure we're called in proper order defined by bnd, if not we will make it be called in proper order.
     *
     * @param kind
     * @param args
     * @param monitor
     * @return List of projetcs we depend on
     */
    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {

        BndPreferences prefs = new BndPreferences();
        CompileErrorAction actionOnCompileError = getActionOnCompileError();

        buildLog = new BuildLogger(prefs.getBuildLogging());

        BuildListeners listeners = new BuildListeners();
        int files = -1;

        try {

            IProject myProject = getProject();

            MarkerSupport markers = new MarkerSupport(this);

            try {
                model = Central.getProject(myProject.getLocation().toFile());
            } catch (Exception e) {
                markers.clearBuildMarkers();
                markers.addBuildMarkers(null, IMarker.SEVERITY_ERROR, e.getMessage());
            }

            if (model == null)
                return null;

            model.setDelayRunDependencies(true);
            model.clear();

            listeners.fireBuildStarting(myProject);

            IProject[] dependsOn = calculateDependsOn(model);

            if (setBuildOrder(dependsOn)) {
                buildLog.basic("Build order changed");
            }

            markers.clearBuildMarkers();

            DeltaWrapper delta = new DeltaWrapper(model, getDelta(myProject), buildLog);

            boolean force = false;

            if (revision != cnfWatcher.getRevision()) {
                buildLog.basic("Was out of date relative to cnf %s-%s", revision, cnfWatcher.getRevision());
                this.revision = cnfWatcher.getRevision();
                force = true;
            }

            force |= resetClasspathContainer(myProject, model.getErrors());

            if (dirty.remove(model)) {
                buildLog.basic("project was dirty from a workspace refresh");
                force = true;
            }

            if (delta.hasProjectChanged()) { // side effect of refresh, not superfluous!
                force = true;
            }

            if (model.isNoBundles()) {
                buildLog.basic("-nobundles was set, so no build");
                files = 0;
                return dependsOn;
            }

            if (!force) {

                switch (kind) {
                default :
                case FULL_BUILD :
                    buildLog.basic("Eclipse requested full build, so we will oblige");
                    break;

                case AUTO_BUILD :
                case INCREMENTAL_BUILD :
                    if (!hasUpstreamChanges(dependsOn)) {
                        buildLog.basic("incremental build but we had no project changes, goodbye");
                        return dependsOn;
                    }
                    break;
                }
            }

            markers.validate(model);

            if (markers.hasBlockingErrors()) {
                markers.addBuildMarkers(model, IMarker.SEVERITY_ERROR, "Will not build OSGi bundle(s) for project %s until  the compilation problems are fixed.", model.getName());

                if (actionOnCompileError != CompileErrorAction.build) {
                    if (actionOnCompileError == CompileErrorAction.delete) {
                        buildLog.basic("Blocking errors, delete build files, quit");
                        deleteBuildFiles(model);
                    } else
                        buildLog.basic("Blocking errors, quit leaving old files there");

                    return dependsOn;
                }
                buildLog.basic("Blocking errors, continuing anyway");
            }

            deleteBuildFiles(model);
            Central.invalidateIndex();

            File buildFiles[] = model.build();

            if (buildFiles != null) {
                listeners.updateListeners(buildFiles, myProject);
                files = buildFiles.length;
            }

            decorate(model);

            markers.createBuildMarkers(model);

            if (model.isCnf())
                model.getWorkspace().refresh();

            return dependsOn;

        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Build Error!", e));
        } finally {
            if (!buildLog.isEmpty())
                logger.logInfo(buildLog.toString(model, files), null);
            listeners.release();
        }
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

            Project up = Central.getProject(upstream);
            if (up != null) {

                IResourceDelta delta = getDelta(upstream);
                DeltaWrapper dw = new DeltaWrapper(up, delta, buildLog);
                if (dw.hasBuildfile()) {
                    buildLog.full("Upstream project %s changed", up);
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * Check if the classpath has changed for this project
     */
    private boolean resetClasspathContainer(IProject myProject, List<String> errors) throws CoreException {
        if (BndContainerInitializer.resetClasspaths(model, myProject, errors)) {
            buildLog.basic("classpaths were changed " + errors);
            return true;
        }
        return false;
    }

    /*
     * Calculate the order for the bnd workspace and set this as the build order for Eclipse.
     */
    private boolean setBuildOrder(IProject[] newer) throws Exception {
        try {
            IProjectDescription projectDescription = getProject().getDescription();
            IProject[] older = projectDescription.getDynamicReferences();
            if (Arrays.equals(newer, older))
                return false;
            projectDescription.setDynamicReferences(newer);
            getProject().setDescription(projectDescription, null);
            buildLog.full("Changed the build order to " + Arrays.toString(newer));

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
