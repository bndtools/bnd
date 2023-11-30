package bndtools.central;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;

public class RepositoriesViewRefresher implements RepositoryListenerPlugin {

	public interface RefreshModel {
		List<RepositoryPlugin> getRepositories();
	}

	// private static final ILogger logger =
	// Logger.getLogger(RepositoriesViewRefresher.class);

	enum State {
		IDLE,
		BUSY,
		REDO;
	}

	private final AtomicReference<State>						state	= new AtomicReference<>(State.IDLE);
	private final ServiceRegistration<RepositoryListenerPlugin>	registration;
	private final Map<TreeViewer, RefreshModel>					viewers	= new ConcurrentHashMap<>();
	private final BundleContext									context;

	RepositoriesViewRefresher() {
		ServiceRegistration<RepositoryListenerPlugin> reg = null;
		Bundle bundle = FrameworkUtil.getBundle(RepositoriesViewRefresher.class);
		if (bundle != null) {
			context = bundle.getBundleContext();
			if (context != null)
				reg = context.registerService(RepositoryListenerPlugin.class, this, null);
		} else
			context = null;
		this.registration = reg;
	}

	public void refreshRepositories(final RepositoryPlugin target) {
		if (state.updateAndGet(current -> (current == State.IDLE) ? State.BUSY : State.REDO) == State.REDO) {
			return;
		}

		//
		// Since this can delay, we move this to the background
		//
		Central.onAnyWorkspace(ws -> {
			new WorkspaceJob("Updating repositories content") {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					if (monitor == null)
						monitor = new NullProgressMonitor();

					Set<RepositoryPlugin> repos = new HashSet<>();
					if (target != null)
						repos.add(target);
					else {
						for (RefreshModel m : viewers.values()) {
							repos.addAll(m.getRepositories());
						}
					}

					ensureLoaded(monitor, repos, ws);

					final Map<Entry<TreeViewer, RefreshModel>, List<RepositoryPlugin>> entryRepos = new HashMap<>();

					for (Map.Entry<TreeViewer, RefreshModel> entry : viewers.entrySet()) {
						entryRepos.put(entry, entry.getValue()
							.getRepositories());
					}

					state.set(State.BUSY);

					for (Map.Entry<TreeViewer, RefreshModel> entry : viewers.entrySet()) {

						TreeViewer viewer = entry.getKey();
						viewer.getControl()
							.getDisplay()
							.asyncExec(() -> {
								TreePath[] expandedTreePaths = viewer.getExpandedTreePaths();

								viewer.setInput(entryRepos.get(entry));
							if (expandedTreePaths != null && expandedTreePaths.length > 0)
									viewer.setExpandedTreePaths(expandedTreePaths);
							});

					}
					if (state.getAndSet(State.IDLE) == State.REDO) {
						refreshRepositories(null);
					}
					return Status.OK_STATUS;
				}
			}.schedule(1000);
		});
	}

	private IStatus ensureLoaded(IProgressMonitor monitor, Collection<RepositoryPlugin> repos, Workspace ws) {
		int n = 0;
		try {
			final RepositoryPlugin workspaceRepo = ws.getWorkspaceRepository();
			for (RepositoryPlugin repo : repos) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				monitor.beginTask(repo.getName(), n++);
				if (repo != workspaceRepo) {
					repo.list(null); // looks silly but is here to incur any
										// download time
					continue;
				}
				// We must safely call bnd to list workspace repo
				try {
					ws.readLocked(() -> workspaceRepo.list(null), monitor::isCanceled);
				} catch (TimeoutException | InterruptedException e) {
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID,
						"Unable to acquire lock to refresh repository " + repo.getName(), e);
				}
			}
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "Exception refreshing repositories", e);
		}
		return Status.OK_STATUS;
	}

	public void addViewer(TreeViewer viewer, RefreshModel model) {
		this.viewers.put(viewer, model);
		Central.onAnyWorkspace(workspace -> new Job("Updating repositories") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<RepositoryPlugin> repositories = model.getRepositories();
				Display.getDefault()
					.asyncExec(() -> viewer.setInput(repositories));
				return Status.OK_STATUS;
			}
		}.schedule());
	}

	public void removeViewer(TreeViewer viewer) {
		this.viewers.remove(viewer);
	}

	@Override
	public void bundleAdded(final RepositoryPlugin repository, Jar jar, File file) {
		refreshRepositories(repository);
	}

	@Override
	public void bundleRemoved(final RepositoryPlugin repository, Jar jar, File file) {
		refreshRepositories(repository);
	}

	@Override
	public void repositoryRefreshed(final RepositoryPlugin repository) {
		refreshRepositories(repository);
	}

	@Override
	public void repositoriesRefreshed() {
		refreshRepositories(null);
	}

	public void close() {
		if (registration != null)
			registration.unregister();
	}

	public void setRepositories(final TreeViewer viewer, final RefreshModel refresh) {
		if (state.updateAndGet(current -> (current == State.IDLE) ? State.BUSY : State.REDO) == State.REDO) {
			return;
		}
		Central.onAnyWorkspace(ws -> {
			new WorkspaceJob("Setting repositories") {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					if (monitor == null)
						monitor = new NullProgressMonitor();

					ensureLoaded(monitor, refresh.getRepositories(), ws);

					SWTConcurrencyUtil.execForControl(viewer.getControl(), true, () -> {
						viewer.setInput(refresh.getRepositories());

						if (state.getAndSet(State.IDLE) == State.REDO) {
							refreshRepositories(null);
						}
					});
					return Status.OK_STATUS;
				}
			}.schedule();
		});
	}
}
