package bndtools.central.sync;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.lib.concurrent.serial.TriggerRepeat;
import aQute.lib.io.IO;
import aQute.lib.watcher.FileWatcher;
import bndtools.Plugin;
import bndtools.central.Central;

/**
 * Synchronizes changes in the bnd workspace with Eclipse. This will use a Java
 * Watcher & checks the modification date since on MacOS the Java Watcher does
 * not work well.
 * <p>
 * If a difference is detected a 1sec job is started that compares the workspace
 * with the file system. Any deltas are processed by creating or deleting the
 * project.
 */
@Component
public class SynchronizeWorkspaceWithEclipse {
	static IWorkspace			eclipse			= ResourcesPlugin.getWorkspace();
	final static IWorkspaceRoot	root			= eclipse.getRoot();
	final TriggerRepeat			lock			= new TriggerRepeat();
	ScheduledFuture<?>			schedule;
	long						lastModified	= -1;
	FileWatcher					watcher;
	File						dir;
	boolean						macos;

	@Reference
	Workspace					workspace;

	@Activate
	void activate() throws IOException {
		schedule = Processor.getScheduledExecutor()
			.scheduleAtFixedRate(this::check, 1000, 300, TimeUnit.MILLISECONDS);

		macos = "MacOSX".equalsIgnoreCase(osname());
		workspace.on("workspace sync")
			.projects(this::sync);
	}

	@Deactivate
	void deactivate() throws IOException {
		schedule.cancel(true);
		if (watcher != null)
			IO.close(watcher);
	}

	private void sync(Collection<Project> doNotUse) {

		if (!lock.trigger()) {
			// already being handled
			return;
		}

		boolean previous = setAutobuild(false);

		Job sync = Job.create("sync workspace", (IProgressMonitor monitor) -> {
			Map<String, IProject> projects = Stream.of(root.getProjects())
				.collect(Collectors.toMap(IProject::getName, p -> p));

			do {
				try {
					// catch up with some changes
					Thread.sleep(500);

					Central.bndCall(after -> {
						boolean refresh = false;
						for (Project model : workspace.getAllProjects()) {
							IProject project = projects.remove(model.getName());
							if (project == null) {
								System.out.println("create " + model);
								refresh = true;
								after.accept("create project",
									() -> WorkspaceSynchronizer.createProject(model.getBase(), model, monitor));

							}
						}

						if (monitor.isCanceled())
							return null;

						for (IProject project : projects.values()) {
							if (!project.isAccessible())
								continue;

							if (!project.hasNature(Plugin.BNDTOOLS_NATURE))
								continue;

							System.out.println("remove " + project);
							refresh = true;
							after.accept("remove project", () -> WorkspaceSynchronizer.removeProject(project, monitor));
						}

						if (monitor.isCanceled())
							return null;

						if (refresh) {

							for (Project p : workspace.getAllProjects()) {

								if (monitor.isCanceled())
									return null;

								p.clean();
							}

							after.accept("refresh", () -> {
								root.refreshLocal(IResource.DEPTH_INFINITE, monitor);
								eclipse.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
								eclipse.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
							});
						}
						if (previous)
							after.accept("resetting autobuild " + previous, () -> setAutobuild(previous));
						return null;
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			} while (lock.doit());

		});
		sync.setRule(root);
		sync.setPriority(Job.SHORT);
		sync.schedule();
	}

	private void watcher(File directory) {
		if (watcher != null)
			IO.close(watcher);
		try {
			watcher = new FileWatcher.Builder().executor(Processor.getExecutor())
				.file(directory)
				.changed(this::changed)
				.build();
		} catch (IOException e) {
			watcher = null;
			About.logger.error("could not create watcher for directory {}", directory);
		}
	}

	private void changed(File file1, String kind) {
		workspace.forceRefreshProjects();
	}

	private String osname() {
		return System.getProperty("os.name", "?")
			.replaceAll("[\\s-_]", "");
	}

	private void check() {
		Workspace ws = Central.getWorkspaceIfPresent();
		if (ws == null)
			return;

		if (!ws.getBase()
			.equals(dir)) {
			dir = ws.getBase();
			watcher(dir);
		}

		long lastModified2 = ws.getBase()
			.lastModified();
		if (lastModified < lastModified2) {
			lastModified = lastModified2;
			long diff = System.currentTimeMillis() - lastModified2;
			workspace.forceRefreshProjects();
		}
	}

	private boolean setAutobuild(boolean on) {
		try {
			IWorkspaceDescription description = eclipse.getDescription();
			boolean original = description.isAutoBuilding();
			description.setAutoBuilding(on);
			eclipse.setDescription(description);
			return original;
		} catch (CoreException e) {
			e.printStackTrace();
			return true;
		}
	}
}
