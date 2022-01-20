package org.bndtools.builder.impl;

import static org.bndtools.builder.impl.BuilderConstants.PLUGIN_ID;
import static org.eclipse.core.resources.IncrementalProjectBuilder.CLEAN_BUILD;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.IProjectValidator;
import org.bndtools.api.IValidator;
import org.bndtools.api.Logger;
import org.bndtools.builder.classpath.BndContainer;
import org.bndtools.builder.classpath.BndContainerInitializer;
import org.bndtools.builder.decorator.ui.PackageDecorator;
import org.bndtools.builder.facade.ProjectBuilderDelegate;
import org.bndtools.builder.handlers.component.ComponentMarker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter.SetLocation;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;
import bndtools.preferences.CompileErrorAction;

/**
 * This a Builder for bndtools. It will use the bnd project/workspace model to
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
@Component(service = {
	ProjectBuilderDelegate.class, BndtoolsBuilder.class
}, scope = ServiceScope.PROTOTYPE)
public class BndtoolsBuilder extends ProjectBuilderDelegate {
	public static final String			BUILDER_ID	= BndtoolsConstants.BUILDER_ID;
	private static final ILogger		logger		= Logger.getLogger(BndtoolsBuilder.class);
	static final Set<Project>			dirty		= Collections
		.newSetFromMap(new ConcurrentHashMap<Project, Boolean>());
	static BndPreferences				prefs		= new BndPreferences();

	private BuildLogger					buildLog;
	private boolean						postponed;

	@Reference
	volatile List<IValidator>			validators;

	@Reference
	volatile List<IProjectValidator>	projectValidators;

	void validate(Project model) throws Exception {
		// Cache reference as it is volatile
		List<IValidator> validators = this.validators;
		if (validators != null) {
			try (ProjectBuilder pb = model.getBuilder(null)) {
				for (Builder builder : pb.getSubBuilders()) {
					for (IValidator v : validators) {
						try {
							IStatus status = v.validate(builder);
							report(builder, status);
							model.getInfo(builder);
						} catch (Exception e) {
							logger.logError("Validator error for validator " + v + " on project " + model.getName(), e);
						}
					}
				}
			}
		}
		List<IProjectValidator> projectValidators = this.projectValidators;
		if (projectValidators != null) {
			for (IProjectValidator pv : projectValidators) {
				try {
					pv.validateProject(model);
				} catch (Exception e) {
					logger.logError("Project validator error for validator " + pv + " on project " + model.getName(),
						e);
				}
			}
		}
	}

	private static void report(Processor reporter, IStatus status) {
		if (status == null || status.isOK())
			return;

		if (status.isMultiStatus()) {
			for (IStatus s : status.getChildren())
				report(reporter, s);
		} else {
			SetLocation location;
			Throwable exception = status.getException();
			if (exception != null)
				if (status.getSeverity() == IStatus.ERROR)
					location = reporter.exception(exception, status.getMessage());
				else
					location = reporter.warning(status.getMessage() + ": " + exception);
			else {
				if (status.getSeverity() == IStatus.ERROR) {
					location = reporter.error(status.getMessage());
				} else {
					location = reporter.warning(status.getMessage());
				}
			}
			location.file(reporter.getPropertiesFile()
				.getAbsolutePath());
		}
	}

	/**
	 * Called from Eclipse when it thinks this project should be build. We're
	 * supposed to figure out if we've changed and then build as quickly as
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

		buildLog = new BuildLogger(prefs.getBuildLogging(), myProject.getName(), kind);

		BuildListeners listeners = new BuildListeners();

		try {

			listeners.fireBuildStarting(myProject);

			MarkerSupport markers = new MarkerSupport(myProject);

			Project ourModel = null;
			try {
				ourModel = Central.getProject(myProject);

			} catch (Exception e) {
				markers.deleteMarkers("*");
				logger.logError("Exception while trying to fetch bnd project for " + myProject.getName(), e);
				markers.createMarker(null, IMarker.SEVERITY_ERROR, "Exception while trying to fetch bnd project: " + e,
					BndtoolsConstants.MARKER_BND_PATH_PROBLEM);
				return null;
			}
			if (ourModel == null) {
				markers.deleteMarkers("*");
				// Do a bit of digging to see if we can find the reason
				Workspace ws = Central.getWorkspaceIfPresent();
				String msg = "Cannot find bnd project: check that it is actually a bnd project and is part of the bnd workspace";
				if (ws == null) {
					msg = "No Bnd workspace is available";
				} else if (ws.isDefaultWorkspace()) {
					msg = "The Bnd cnf directory is not part of the Eclipse workspace";
				} else {
					IPath projPath = myProject.getLocation();

					if (projPath == null) {
						msg = "The project does not exist on the local file system";
					} else {
						Path javaPath = projPath.toFile()
							.toPath();
						Path wsPath = ws.getBase()
							.toPath();
						if (!Files.isSameFile(javaPath.getParent(), wsPath)) {
							msg = "The project directory: " + javaPath + " is not a subdirectory of the bnd workspace "
								+ wsPath;
						} else {
							Path bndPath = javaPath.resolve(Project.BNDFILE);

							if (!Files.exists(bndPath)) {
								msg = "The project does not have a " + Project.BNDFILE + " file";
							} else if (!Files.isReadable(bndPath)) {
								msg = Project.BNDFILE + " is not readable";
							} else if (Files.isDirectory(bndPath)) {
								msg = Project.BNDFILE + " is a directory";
							} else if (!Files.isRegularFile(bndPath)) {
								// If it exists but it's not a directory, it
								// still might not be a file - eg, could be a
								// pipe or a device if the user has done
								// something silly.
								msg = Project.BNDFILE + " is not a regular file";
							}
						}
					}
				}
				markers.createMarker(null, IMarker.SEVERITY_ERROR, msg, BndtoolsConstants.MARKER_BND_PATH_PROBLEM);
				return null;
			}

			final Project model = ourModel;

			try {
				markers.deleteMarkers(BndtoolsConstants.MARKER_BND_BLOCKER);
				Workspace ws = model.getWorkspace();
				Central.bndCall(ws::readLocked, after -> {
					if (!model.isValid()) {
						after.accept("Not a valid project" + model, () -> {
							markers.createMarker(null, IMarker.SEVERITY_ERROR, "Not a valid bnd project",
								BndtoolsConstants.MARKER_BND_PATH_PROBLEM);
						});
						return null;
					}

					boolean force = kind == IncrementalProjectBuilder.FULL_BUILD;
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

						Processor processor = new Processor();
						validate(model);
						processor.getInfo(model);
						after.accept("Decorating " + myProject, () -> {
							markers.setMarkers(processor, BndtoolsConstants.MARKER_BND_PATH_PROBLEM);
						});
						model.clear();

						boolean changedClasspath = setupChanged && requestClasspathContainerUpdate(myProject);

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

					if (!force && delta.hasOutOfDateTarget(model, lastModifiedClasspathContainer(myProject))) {
						buildLog.basic("project has no or out-of-date target files");
						force = true;
					}

					//
					// If we're not forced to build at this point
					// then we have an incremental build and
					// no reason to rebuild.
					//

					if (!force) {
						buildLog.full("Auto/Incr. build, no changes detected");
						return null;
					}

					WorkingSetTracker.doWorkingSets(model, myProject);

					if (model.isNoBundles()) {
						buildLog.basic("-nobundles was set, so no build");
						buildLog.setFiles(0);
						return null;
					}

					if (markers.hasBlockingErrors(delta)) {
						CompileErrorAction actionOnCompileError = getActionOnCompileError();
						if (actionOnCompileError != CompileErrorAction.build) {
							after.accept("Decorating " + myProject, () -> {
								if (actionOnCompileError == CompileErrorAction.delete) {
									buildLog.basic("Blocking errors, delete build files, quit");
									deleteBuildFiles(model);
									markers.createMarker(model, IMarker.SEVERITY_ERROR, "Build errors, deleted files",
										BndtoolsConstants.MARKER_BND_BLOCKER);
								} else {
									buildLog.basic("Blocking errors, leave old build files, quit");
									markers.createMarker(model, IMarker.SEVERITY_ERROR, "Build errors, leaving files",
										BndtoolsConstants.MARKER_BND_BLOCKER);
								}
							});
							return null;
						}

						buildLog.basic("Blocking errors, continuing anyway");
						after.accept("Decorating " + myProject, () -> {
							markers.createMarker(model, IMarker.SEVERITY_WARNING,
								"Project " + myProject + " has blocking errors but requested to continue anyway",
								BndtoolsConstants.MARKER_BND_BLOCKER);
						});
					}

					File buildFiles[] = model.build();
					// We can now decorate based on the build we just did.
					BndProjectInfoAdapter adapter = new BndProjectInfoAdapter(model);
					File target = model.getTarget();
					Processor processor = new Processor();
					processor.getInfo(model);

					after.accept("Decorating " + myProject, () -> {
						IResource r = Central.toResource(target);
						r.refreshLocal(IResource.DEPTH_INFINITE, monitor);

						Central.invalidateIndex();
						if (buildFiles != null) {
							listeners.updateListeners(buildFiles, myProject);
							buildLog.setFiles(buildFiles.length);
						}
						PackageDecorator.updateDecoration(myProject, adapter);
						ComponentMarker.updateComponentMarkers(myProject, adapter);
						markers.setMarkers(processor, BndtoolsConstants.MARKER_BND_PROBLEM);
						processor.close();
					});
					if (model.isCnf()) {
						Job job = Job.create("refresh workspace", (m) -> {
							model.getWorkspace()
							.refresh(); // this is for bnd plugins built in
											// cnf
						});
						job.schedule(10);
					}
					return null;
				}, monitor);
				return null;
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

	private IProject[] postpone() {
		postponed = true;
		rememberLastBuiltState();
		return null;
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
				Workspace ws = model.getWorkspace();
				ws.readLocked(() -> {
					model.clean();
					return null;
				}, monitor::isCanceled);
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

	private boolean requestClasspathContainerUpdate(IProject myProject) throws CoreException {
		IJavaProject javaProject = JavaCore.create(myProject);
		return (javaProject == null) ? false : BndContainerInitializer.requestClasspathContainerUpdate(javaProject);
	}

	private long lastModifiedClasspathContainer(IProject myProject) {
		IJavaProject javaProject = JavaCore.create(myProject);
		if (javaProject != null) {
			IClasspathContainer container = BndContainerInitializer.getClasspathContainer(javaProject);
			if (container instanceof BndContainer) {
				BndContainer bndContainer = (BndContainer) container;
				return bndContainer.lastModified();
			}
		}
		return 0L;
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

	private CompileErrorAction getActionOnCompileError() {
		ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(getProject()),
			BndtoolsConstants.CORE_PLUGIN_ID);
		return CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY));
	}

	/**
	 * Override the scheduling rule, should make the UI more responsive
	 */
	@Override
	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		if (prefs.isParallel())
			return getProject();
		return super.getRule(kind, args);
	}
}
