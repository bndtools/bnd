package aQute.bnd.concurrent;

import java.io.*;
import java.util.*;

import aQute.bnd.build.*;
import aQute.lib.osgi.*;
import aQute.libg.forker.*;

/**
 * This class implements a concurrent builder. It manages the build process in
 * an environment where many threads can initiate builds. Users should call
 * changed(Project,boolean)
 * 
 */
public class MultiBuilder {
	Workspace		workspace;
	Forker<Project>	forker;
	boolean			building		= false;
	final Set<File>	filesChanged	= Collections.synchronizedSet(new HashSet<File>());

	/**
	 * Constructor
	 * 
	 * @param workspace
	 *            the workspace this MultiBuilder works for.
	 * 
	 */
	public MultiBuilder(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Return the build result of a project.
	 * 
	 * @param p
	 *            the project
	 * @return the files build by the project
	 * 
	 * @throws Exception
	 */
	public File[] build(Project p) throws Exception {
		if (p.isStale()) {
			startBuild();
		}
		syncBuild();
		return p.build();
	}

	/**
	 * Indicate that the project has changed. This will start a build.
	 * 
	 * @param p
	 *            the project that is changed
	 * @throws Exception
	 */
	public void changed(Project p) throws Exception {
		p.setChanged();
		cancel();
		startBuild();
	}

	/**
	 * Cancel the current build or do nothing if no build is active.
	 * 
	 * @throws InterruptedException
	 */
	public synchronized void cancel() throws InterruptedException {
		if (building) {
			forker.cancel();
		}
	}

	/**
	 * Synchronize with a current build or return immediately.
	 * 
	 * @throws InterruptedException
	 */
	public synchronized void syncBuild() throws InterruptedException {
		if (building) {
			forker.join();
		}
	}

	/**
	 * Schedule a new build if no build is running otherwise return.
	 * 
	 * @throws Exception
	 */
	public void startBuild() throws Exception {
		synchronized (this) {
			if (building)
				return;

			forker = new Forker<Project>(Processor.getExecutor());
			building = true;
		}

		Processor.getExecutor().execute(new Runnable() {
			public void run() {
				try {
					build();
					synchronized (MultiBuilder.this) {
						building = false;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Do the whole build using a forker.
	 * 
	 * @throws Exception
	 */
	private void build() throws Exception {
		// handle multiple requests
		Thread.sleep(100);
		workspace.bracket(true);
		try {
			for (final Project p : workspace.getAllProjects()) {
				forker.doWhen(p.getDependson(), p, new Runnable() {

					public void run() {
						try {
							p.build();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
			forker.join();
		} finally {
			workspace.bracket(false);
		}
	}

}
