package aQute.bnd.jpm;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Semaphore;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.version.Version;

public class Crawler extends Thread {
	boolean				quit;
	private Repository	repository;
	private Set<String>	dls			= new HashSet<>();
	private Set<String>	fails		= new HashSet<>();
	private Semaphore	throttle	= new Semaphore(2);

	public Crawler(Repository repository) {
		this.repository = repository;
	}

	public void run() {
		outer: while (!quit)
			try {

				if (!repository.offline) {
					for (String bsn : repository.list(null)) {
						SortedSet<Version> versions = repository.versions(bsn);
						for (Version version : versions) {

							final String key = bsn + "-" + version;

							if (dls.contains(key))
								continue;

							throttle.acquire();

							DownloadListener dl = new RepositoryPlugin.DownloadListener() {

								@Override
								public void success(File file) throws Exception {
									throttle.release();
									dls.remove(key);
								}

								@Override
								public void failure(File file, String reason) throws Exception {
									throttle.release();
									fails.add(key);
									dls.remove(key);
								}

								@Override
								public boolean progress(File file, int percentage) throws Exception {
									return false;
								}

							};

							if (!dls.contains(key)) {
								dls.add(key);
								repository.get(bsn, version, null, dl);
							}

							if (interrupted())
								continue outer;

							if (repository.offline)
								continue outer;

						}

					}
				}
				Thread.sleep(500000);
			}
			catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(5000);
				}
				catch (InterruptedException e1) {}
			}
	}

	public void close() {
		quit = true;
		interrupt();
	}

	public void refresh() {
		interrupt();
	}

}
