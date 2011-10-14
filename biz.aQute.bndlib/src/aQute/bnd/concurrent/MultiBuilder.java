package aQute.bnd.concurrent;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

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
	Workspace						workspace;
	volatile FutureTask<Throwable>	future;
	final Set<File>					filesChanged	= Collections
															.synchronizedSet(new HashSet<File>());

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
			schedule(true);
		}
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
		schedule(false);
	}

	/**
	 * Schedule a new build if no build is running otherwise return.
	 * 
	 * @throws Exception
	 */
	public void schedule(boolean sync) throws Exception {
		synchronized (this) {
			if (future != null) {
				future.cancel(true);
				future.get();
				future = null;
			}

			future = new FutureTask<Throwable>(new Callable<Throwable>() {

				public Throwable call() {
					Forker<Project> forker = new Forker<Project>(Processor.getExecutor());

					try {
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
							forker.start(0);
						} catch (InterruptedException e) {
							forker.cancel(10000);
						} finally {
							workspace.bracket(false);
						}
					} catch (Exception e) {
						return e;
					}
					return null;
				}

			});
			Processor.getExecutor().execute(future);
			if ( sync )
				future.get();
		}
	}

}
