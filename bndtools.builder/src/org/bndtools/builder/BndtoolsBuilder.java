package org.bndtools.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.builder.classpath.BndContainerInitializer;
import org.bndtools.builder.decorator.ui.PackageDecorator;
import org.bndtools.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;
import bndtools.preferences.CompileErrorAction;

/**
 * This a Builder for bndtools.It will use the bnd project/workspace model to
 * incrementally build bundles. This is a rewrite of the NewBuilder. Left out
 * are the Eclipse classpath include in a build option.
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
	public static final String		PLUGIN_ID	= "bndtools.builder";
	public static final String		BUILDER_ID	= BndtoolsConstants.BUILDER_ID;
	private static final ILogger	logger		= Logger.getLogger(BndtoolsBuilder.class);
	static final Set<Project>		dirty		= Collections.newSetFromMap(new ConcurrentHashMap<Project, Boolean>());

	static {
		CnfWatcher.install();
	}

	private Project		model;
	private BuildLogger	buildLog;
	private IProject[]	dependsOn;
	private boolean		postponed;

	/**
	 * Called from Eclipse when it thinks this project should be build. We're
	 * proposed to figure out if we've changed and then build as quickly as
	 * possible.
	 * <p>
	 * We ensure we're called in proper order defined by bnd, if not we will
	 * make it be called in proper order.
	 *
	 * @param kind
	 * @param args
	 * @param monitor
	 * @return List of projects we depend on
	 */
	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {

		IProject myProject = getProject();
		BndPreferences prefs = new BndPreferences();
		buildLog = new BuildLogger(prefs.getBuildLogging(), myProject.getName(), kind);

		BuildListeners listeners = new BuildListeners();

		try {

			listeners.fireBuildStarting(myProject);

			MarkerSupport markers = new MarkerSupport(myProject);

			//
			// First time after a restart
			//

			if (dependsOn == null) {
				dependsOn = myProject.getDescription()
					.getDynamicReferences();
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
				return Central.bndCall(() -> {
					boolean force = kind == FULL_BUILD;
					model.clear();

					DeltaWrapper delta = new DeltaWrapper(model, getDelta(myProject), buildLog);

					boolean setupChanged = false;

					if (!postponed && (delta.havePropertiesChanged(model) || delta.hasChangedSubbundles())) {
						buildLog.basic("project was dirty from changed bnd files postponed = " + postponed);
						model.forceRefresh();
						setupChanged = true;
					}

					if (dirty.remove(model) && !setupChanged) {
						buildLog.basic("project was dirty from a workspace refresh postponed = " + postponed);
						setupChanged = !postponed;
					}

					if (!force && !setupChanged && delta.hasEclipseChanged()) {
						buildLog.basic("Eclipse project had a buildpath change");
						force = true;
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

						// perform both actions before potentially postponing
						boolean changedBuildOrder = setBuildOrder(monitor);
						boolean changedClasspath = setupChanged && requestClasspathContainerUpdate(myProject);

						if (changedBuildOrder) {
							buildLog.basic("Build order changed");
							return postpone();
						}

						if (changedClasspath) {
							buildLog.basic("Classpath changed");
							return postpone();
						}

						force = true;
					}

					//
					// We did not postpone, so reset the flag
					//
					if (postponed) {
						buildLog.full("Was postponed");
						force = true;
						postponed = false;
					}

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

					WorkingSetTracker.doWorkingSets(model, myProject);

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
								model.error(
									"Will not build project %s until the compilation and/or path problems are fixed, output files are deleted.",
									myProject.getName());
							} else {
								buildLog.basic("Blocking errors, leave old build files, quit");
								model.error(
									"Will not build project %s until the compilation and/or path problems are fixed, output files are kept.",
									myProject.getName());
							}
							return report(markers);
						}
						buildLog.basic("Blocking errors, continuing anyway");
						model.warning("Project %s has blocking errors but requested to continue anyway",
							myProject.getName());
					}

					Central.invalidateIndex();

					File buildFiles[] = model.build();

					if (buildFiles != null) {
						listeners.updateListeners(buildFiles, myProject);
						buildLog.setFiles(buildFiles.length);
					}

					// We can now decorate based on the build we just did.
					BndProjectInfoAdapter adapter = new BndProjectInfoAdapter(model);

					PackageDecorator.updateDecoration(myProject, adapter);

					ComponentMarker.updateComponentMarkers(myProject, adapter);

					if (model.isCnf()) {
						model.getWorkspace()
							.refresh(); // this is for bnd plugins built in cnf
					}

					return report(markers);
				}, monitor);
			} catch (TimeoutException | InterruptedException e) {
				logger.logWarning("Unable to build project " + myProject.getName(), e);
				return postpone();
			}
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Build Error!", e));
		} finally {
			if (buildLog.isActive())
				logger.logInfo(buildLog.format(), null);
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
		IProject myProject = getProject();
		BndPreferences prefs = new BndPreferences();
		buildLog = new BuildLogger(prefs.getBuildLogging(), myProject.getName(), CLEAN_BUILD);
		try {
			MarkerSupport markers = new MarkerSupport(myProject);
			markers.deleteMarkers("*");

			Project model;
			try {
				model = Central.getProject(myProject);
			} catch (Exception e) {
				markers.createMarker(null, IMarker.SEVERITY_ERROR, "Cannot find bnd project",
					BndtoolsConstants.MARKER_BND_PATH_PROBLEM);
				return;
			}
			if (model == null)
				return;

			try {
				Central.bndCall(() -> {
					model.clean();
					return null;
				}, monitor);
			} catch (TimeoutException | InterruptedException e) {
				logger.logWarning("Unable to clean project " + myProject.getName(), e);
				return;
			}

			// Tell Eclipse what we did...
			Central.refreshFile(model.getTarget(), monitor, true);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Build Error!", e));
		} finally {
			if (buildLog.isActive())
				logger.logInfo(buildLog.format(), null);
		}
	}

	/*
	 * Check if any of the projects of which we depend has changes. We use the
	 * generated/buildfiles as the marker.
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

	/*
	 * Set the project's dependencies to influence the build order for Eclipse.
	 */
	@SuppressWarnings("deprecation")
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

	private boolean requestClasspathContainerUpdate(IProject myProject) throws CoreException {
		IJavaProject javaProject = JavaCore.create(myProject);
		return (javaProject == null) ? false : BndContainerInitializer.requestClasspathContainerUpdate(javaProject);
	}

	private void deleteBuildFiles(Project model) throws Exception {
		File[] buildFiles = model.getBuildFiles(false);
		if (buildFiles != null)
			for (File f : buildFiles) {
				if (f != null)
					IO.delete(f);
			}
		IO.delete(new File(model.getTarget(), Constants.BUILDFILES));
	}

	private IProject[] calculateDependsOn(Project model) throws Exception {
		Collection<Project> dependsOn = model.getDependson();

		IWorkspaceRoot wsroot = getProject().getWorkspace()
			.getRoot();

		List<IProject> result = new ArrayList<>(dependsOn.size() + 1);

		IProject cnfProject = WorkspaceUtils.findCnfProject(wsroot, model.getWorkspace());
		if (cnfProject != null) {
			result.add(cnfProject);
		}

		for (Project project : dependsOn) {
			IProject targetProj = WorkspaceUtils.findOpenProject(wsroot, project);
			if (targetProj == null)
				logger.logWarning("No open project in workspace for Bnd '-dependson' dependency: " + project.getName(),
					null);
			else
				result.add(targetProj);
		}

		buildLog.full("Calculated dependsOn list: %s", result);
		return result.toArray(new IProject[0]);
	}

	private CompileErrorAction getActionOnCompileError() {
		ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()),
			BndtoolsConstants.CORE_PLUGIN_ID);
		return CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY));
	}

}
