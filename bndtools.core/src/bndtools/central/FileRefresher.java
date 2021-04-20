package bndtools.central;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileRefresher {
	final static Logger		logger		= LoggerFactory.getLogger(FileRefresher.class);
	final IWorkspace		iworkspace	= ResourcesPlugin.getWorkspace();
	final IWorkspaceRoot	iroot		= iworkspace.getRoot();
	final Set<Item>			toRefresh	= new HashSet<>();
	boolean					active		= false;

	static class Item {
		File	f;
		boolean	derived;
	}

	void changed(Collection<File> files, boolean derived) {

		if (files == null || files.isEmpty())
			return;

		synchronized (toRefresh) {

			files.stream()
				.map(f -> {
					Item item = new Item();
					item.f = f;
					item.derived = derived;
					return item;
				})
				.forEach(toRefresh::add);

			if (active || toRefresh.isEmpty())
				return;
			active = true;
		}

		Job job = Job.create("Refresh file", m -> {

			assert !Central.inBndLock() : "We may never compete with the bnd lock";

			Set<Item> refresh = new HashSet<>();
			while (true) {
				synchronized (toRefresh) {
					refresh.addAll(toRefresh);
					toRefresh.clear();
					if (refresh.isEmpty()) {
						active = false;
						return;
					}
				}

				for (Item resource : refresh) {
					if (m.isCanceled()) {
						synchronized (toRefresh) {
							logger.info("interrupted refresh with {}", toRefresh);
							toRefresh.clear();
							active = false;
						}
						return;
					}

					IResource r = Central.toResource(resource.f);
					if (r != null) {
						r.setDerived(resource.derived, m);
						m.subTask("Refresh " + r);
						r.refreshLocal(IResource.DEPTH_INFINITE, m);
					}
				}

				refresh.clear();
			}
		});
		job.setRule(iroot);
		job.schedule();
	}

	void changed(File file, boolean derived) {
		changed(Collections.singleton(file), derived);
	}

}
