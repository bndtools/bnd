package bndtools.central.sync;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
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
@Component
public class SynchronizeWorkspaceWithEclipse {
	final static IWorkspaceRoot	root			= ResourcesPlugin.getWorkspace()
		.getRoot();
	final AtomicBoolean			lock			= new AtomicBoolean();
	final ScheduledFuture<?>	schedule;

	long						lastModified	= -1;
	FileWatcher					watcher;
	File						dir;
	final boolean				macos;

	@Reference
	Workspace					workspace;

	public SynchronizeWorkspaceWithEclipse() throws IOException {
		schedule = Processor.getScheduledExecutor()
			.scheduleAtFixedRate(this::check, 1000, 1000, TimeUnit.MILLISECONDS);

		macos = "MacOSX".equalsIgnoreCase(osname());
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

	@Deactivate
	void deactivate() throws IOException {
		schedule.cancel(true);
		if (watcher != null)
			IO.close(watcher);
	}

	private void sync() {
		if (lock.getAndSet(true) == true) {
			// already being handled
			return;
		}

		Job job = Job.create("bnd sync", (m) -> {
			System.out.println("Syncing");

			while (System.currentTimeMillis() - dir.lastModified() < 1000) {
				sleep(1000);
			}
			if (m.isCanceled())
				return;
			lock.set(false);
			WorkspaceSynchronizer ws = new WorkspaceSynchronizer();
			ws.synchronize(true, m, () -> {});
		});
		job.setRule(root);
		job.schedule(1000);
	}

	private void sleep(long sleep) {
		try {
			Thread.sleep(sleep);
		} catch (InterruptedException e) {
			Thread.currentThread()
				.interrupt();
			return;
		}
	}

	private void changed(File file1, String kind) {
		System.out.println("changed " + file1 + " " + kind);
		sync();
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
			System.out.println("workspace time changed " + diff);
			sync();
		}
	}

}
