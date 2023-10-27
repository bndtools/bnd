package bndtools.central.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.api.OnWorkspace;
import aQute.bnd.osgi.Processor;
import aQute.lib.concurrent.serial.TriggerRepeat;
import aQute.lib.io.IO;
import aQute.lib.watcher.FileWatcher;
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
@Component(enabled = false)
public class SynchronizeWorkspaceWithEclipse {
	final IWorkspace		eclipse;
	final IWorkspaceRoot	root;
	final Workspace			workspace;
	// This restricted API warning can be ignored because the constant
	// is resolved at compile time.
	@SuppressWarnings("restriction")
	final static String		BNDTOOLS_NATURE	= bndtools.Plugin.BNDTOOLS_NATURE;
	final TriggerRepeat		lock			= new TriggerRepeat();
	ScheduledFuture<?>		schedule;
	long					lastModified	= -1;
	FileWatcher				watcher;
	File					dir;
	boolean					macos;
	OnWorkspace				event;

	@Activate
	public SynchronizeWorkspaceWithEclipse(@Reference
	IWorkspace eclipse, @Reference
	Workspace workspace) throws IOException {
		this.eclipse = eclipse;
		this.workspace = workspace;
		this.root = eclipse.getRoot();
		schedule = Processor.getScheduledExecutor()
			.scheduleAtFixedRate(this::check, 1000, 300, TimeUnit.MILLISECONDS);

		macos = "MacOSX".equalsIgnoreCase(osname());
		event = workspace.on("workspace sync")
			.projects(this::sync);
	}

	@Deactivate
	void deactivate() throws IOException {
		schedule.cancel(true);
		if (watcher != null)
			IO.close(watcher);
		IO.close(event);
	}

	private void sync(Collection<Project> doNotUse) {

		if (!lock.trigger()) {
			// already being handled
			return;
		}

		// No need to turn off autobuild as the lock will take care of it

		Job sync = Job.create("sync workspace", (IProgressMonitor monitor) -> {

			Map<String, IProject> projects = Stream.of(root.getProjects())
				.collect(Collectors.toMap(IProject::getName, p -> p));

			SubMonitor subMonitor = SubMonitor.convert(monitor, 5);

			do {
				try {
					// Reset this on every iteration (divides the remaining
					// time in the monitor into 5, however much is remaining).
					subMonitor.setWorkRemaining(5);
					subMonitor.setTaskName("Waiting for changes to finish");
					// Step 1: wait for catch-up
					subMonitor.split(1);
					// catch up with some changes
					Thread.sleep(500);

					List<Project> createProjects = new ArrayList<>(100);
					List<IProject> removeProjects = new ArrayList<>(100);

					workspace.writeLocked(() -> {
						Collection<Project> allProjects = workspace.getAllProjects();
						// Step 2: look for projects to add/update
						SubMonitor loopMonitor = subMonitor.split(1)
							.setWorkRemaining(allProjects.size());
						subMonitor.setTaskName("Scanning bnd workspace for projects to add/refresh");
						for (Project model : allProjects) {
							loopMonitor.split(1);
							loopMonitor.subTask("Checking project " + model.getName());
							IProject project = projects.remove(model.getName());
							if (project == null) {
								createProjects.add(model);
							}
						}

						// Step 3: look for projects to remove.
						loopMonitor = subMonitor.split(1)
							.setWorkRemaining(projects.size());
						subMonitor.setTaskName("Scanning Eclipse workspace for projects to remove");

						for (IProject project : projects.values()) {
							loopMonitor.split(1);
							loopMonitor.subTask("Checking project " + project.getName());
							if (!project.isAccessible())
								continue;

							if (!project.hasNature(BNDTOOLS_NATURE))
								continue;

							removeProjects.add(project);
						}
						return null;
					}, subMonitor::isCanceled);

					// Step 4: add/refresh projects that were found
					SubMonitor loopMonitor = subMonitor.split(1)
						.setWorkRemaining(createProjects.size());
					subMonitor.setTaskName("Adding/creating bnd projects in Eclipse workspace");
					for (Project create : createProjects) {
						loopMonitor.subTask("Adding/creating project " + create.getName());
						IProject p = WorkspaceSynchronizer.createProject(create.getBase(), create,
							loopMonitor.split(1));
					}

					// Step 5: remove projects that were not found
					loopMonitor = subMonitor.split(1)
						.setWorkRemaining(removeProjects.size());
					subMonitor.setTaskName("Removing projects that are not part of the bnd workspace");
					for (IProject remove : removeProjects) {
						loopMonitor.subTask("Removing project " + remove.getName());
						WorkspaceSynchronizer.removeProject(remove, loopMonitor.split(1));
					}
				} catch (OperationCanceledException e) {
					throw e;
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
}
