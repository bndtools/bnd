package org.bndtools.elph.importer;

import static java.util.stream.Collectors.toSet;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.runtime.Status.CANCEL_STATUS;
import static org.eclipse.core.runtime.Status.OK_STATUS;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

enum EclipseWorkspace {
	;
	/**
	 * Importing cnf causes the bndtools plugin in eclipse to analyze the entire workspace.
	 * This is in addition to our analysis using our version of bndtools.
	 * Both analyses can happen in parallel.
	 * Both should be kicked off early.
	 * Every other project depends on cnf, so this class will allow (and require) cnf to be imported first.
	 * This latch is to let other project imports wait for this initial import to happen.
	 */
	private static CountDownLatch bndWorkspaceInit = new CountDownLatch(1);
	
	private static void importProject(final Path projectRoot) throws CoreException {
		IProjectDescription description = getWorkspace().loadProjectDescription(
				new org.eclipse.core.runtime.Path(projectRoot.resolve(".project").toString()));
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
		project.create(description, null);
		project.open(null); // this step takes a long time for project "cnf" because the bndtools plugin is doing its own analysis 
	}

	static Set<String> listProjects() {
		return Stream.of(getWorkspace().getRoot().getProjects())
			.map(p-> p.getName())
			// filter out any that aren't in the bnd workspace
			.collect(toSet());
	}
	
	static void importCnf(Path bndWorkspace) {
		// If cnf is already imported, we can skip this bit
		// but synch on the latch first to make sure the import isn't still underway
		synchronized (bndWorkspaceInit) {
			if (listProjects().contains("cnf")) {
				// cnf is already imported - nothing more to do
				// TODO: check that the bndtools plugin is working and has parsed any updates
				bndWorkspaceInit.countDown();
				return;
			}
		}
		Job.create("Initializing bnd workspace with cnf project", monitor -> {  
			synchronized (bndWorkspaceInit) {
				try {
					importProject(bndWorkspace.resolve("cnf"));
					System.out.println("Successfully imported cnf");
				} catch (RuntimeException | CoreException  err) {
					System.err.println(err.getMessage());
					return CANCEL_STATUS;
				}
				bndWorkspaceInit.countDown();
			}
			return OK_STATUS;
		}).schedule(); 
	}
	
	static void importProjects(Collection<Path> paths) { 
		try {
			bndWorkspaceInit.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new Error(e);
		}
		Set<String> existingProjects = listProjects();
		Queue<Path> queue = paths.stream()
				.filter(p -> !existingProjects.contains(p.getFileName().toString()))
				.collect(Collectors.toCollection(LinkedList::new));
		Job.create("Import Liberty projects", monitor -> {  
			// use a submonitor to specify the number of subtasks
			SubMonitor subMonitor = SubMonitor.convert(monitor, queue.size());
			for (Path p = queue.poll(); null != p; p = queue.poll()) {
				try {
					subMonitor.setTaskName("Importing " + p.getFileName());
					subMonitor.split(1);
					importProject(p);
					System.out.println("Successfully Imported " + p.getFileName());
				} catch (RuntimeException | CoreException  err) {
					System.err.println(err.getMessage());
					return CANCEL_STATUS;
				}
			}		
			return OK_STATUS;
		}).schedule(); 
	}
}