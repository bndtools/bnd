package bndtools.central;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.IStartupParticipant;
import org.bndtools.api.Logger;
import org.bndtools.api.ModelListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.function.Consumer;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.exceptions.Exceptions;
import bndtools.central.RepositoriesViewRefresher.RefreshModel;
import bndtools.preferences.BndPreferences;

public class Central implements IStartupParticipant {

	private static final ILogger						logger						= Logger.getLogger(Central.class);
	private static volatile Central						instance					= null;
	private static volatile Workspace					workspace					= null;
	private static final Deferred<Workspace>			workspaceQueue				= Processor.getPromiseFactory()
		.deferred();

	static WorkspaceR5Repository						r5Repository				= null;

	private static Auxiliary							auxiliary;

	static final AtomicBoolean							indexValid					= new AtomicBoolean(false);

	private final BundleContext							bundleContext;
	private final Map<IJavaProject, Project>			javaProjectToModel			= new HashMap<>();
	private final List<ModelListener>					listeners					= new CopyOnWriteArrayList<>();

	private RepositoryListenerPluginTracker				repoListenerTracker;

	@SuppressWarnings("unused")
	private static WorkspaceRepositoryChangeDetector	workspaceRepositoryChangeDetector;

	private static RepositoriesViewRefresher			repositoriesViewRefresher	= new RepositoriesViewRefresher();

	static {
		try {
			BundleContext context = FrameworkUtil.getBundle(Central.class)
				.getBundleContext();
			Bundle bndlib = FrameworkUtil.getBundle(Workspace.class);
			auxiliary = new Auxiliary(context, bndlib);
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * WARNING: Do not instantiate this class. It must be public to allow
	 * instantiation by the Eclipse registry, but it is not intended for direct
	 * creation by clients. Instead call Central.getInstance().
	 */
	@Deprecated
	public Central() {
		bundleContext = FrameworkUtil.getBundle(Central.class)
			.getBundleContext();
	}

	@Override
	public void start() {
		instance = this;

		repoListenerTracker = new RepositoryListenerPluginTracker(bundleContext);
		repoListenerTracker.open();

	}

	@Override
	public void stop() {
		repoListenerTracker.close();

		instance = null;

		Workspace ws = workspace;
		if (ws != null) {
			ws.close();
		}

		if (auxiliary != null)
			try {
				auxiliary.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	public static Central getInstance() {
		return instance;
	}

	public Project getModel(IJavaProject project) {
		try {
			Project model = javaProjectToModel.get(project);
			if (model == null) {
				try {
					model = getProject(project.getProject());
				} catch (IllegalArgumentException e) {
					// initialiseWorkspace();
					// model = Central.getProject(projectDir);
					return null;
				}
				if (workspace == null) {
					model.getWorkspace();
				}
				if (model != null) {
					javaProjectToModel.put(project, model);
				}
			}
			return model;
		} catch (Exception e) {
			// TODO do something more useful here
			throw new RuntimeException(e);
		}
	}

	public static IFile getWorkspaceBuildFile() throws Exception {
		File file = Central.getWorkspace()
			.getPropertiesFile();
		IFile[] matches = ResourcesPlugin.getWorkspace()
			.getRoot()
			.findFilesForLocationURI(file.toURI());

		if (matches == null || matches.length != 1) {
			logger.logError("Cannot find workspace location for bnd configuration file " + file, null);
			return null;
		}

		return matches[0];
	}

	public synchronized static WorkspaceR5Repository getWorkspaceR5Repository() throws Exception {
		if (r5Repository != null)
			return r5Repository;

		r5Repository = new WorkspaceR5Repository();
		r5Repository.init();

		return r5Repository;
	}

	public synchronized static RepositoryPlugin getWorkspaceRepository() throws Exception {
		return getWorkspace().getWorkspaceRepository();
	}

	public static Workspace getWorkspaceIfPresent() {
		try {
			return getWorkspace();
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			return null;
		}
	}

	public static Workspace getWorkspace() throws Exception {
		if (getInstance() == null) {
			throw new IllegalStateException("Central is not initialised");
		}
		Workspace ws;
		boolean resolve;
		synchronized (workspaceQueue) {
			ws = workspace;
			File workspaceDirectory = getWorkspaceDirectory();
			if (ws != null) {
				if (workspaceDirectory != null && ws.isDefaultWorkspace()) {
					ws.setFileSystem(workspaceDirectory, Workspace.CNFDIR);
					ws.refresh();
					resolve = !workspaceQueue.getPromise()
						.isDone();
				} else if (workspaceDirectory == null && !ws.isDefaultWorkspace()) {
					ws.setFileSystem(Workspace.BND_DEFAULT_WS, Workspace.CNFDIR);
					ws.refresh();
					resolve = false;
				} else {
					resolve = false;
				}
			} else {
				try {
					Workspace.setDriver(Constants.BNDDRIVER_ECLIPSE);
					Workspace.addGestalt(Constants.GESTALT_INTERACTIVE, new Attrs());

					if (workspaceDirectory == null) {
						// there is no cnf project. So
						// we create a temp workspace
						ws = Workspace.createDefaultWorkspace();
						resolve = false;
					} else {
						ws = Workspace.getWorkspace(workspaceDirectory);
						resolve = true;
					}

					ws.setOffline(new BndPreferences().isWorkspaceOffline());

					ws.addBasicPlugin(new WorkspaceListener(ws));
					ws.addBasicPlugin(getInstance().repoListenerTracker);
					ws.addBasicPlugin(getWorkspaceR5Repository());
					ws.addBasicPlugin(new JobProgress());

					// Initialize projects in synchronized block
					ws.getBuildOrder();

					// Monitor changes in cnf so we can refresh the workspace
					addCnfChangeListener(ws);

					workspaceRepositoryChangeDetector = new WorkspaceRepositoryChangeDetector(ws);

					// The workspace has been initialized fully, set the field
					// now
					workspace = ws;
				} catch (final Exception e) {
					if (ws != null) {
						ws.close();
					}
					logger.logError("Init of workspace", e);
					throw e;
				}
			}
		}
		if (resolve)
			workspaceQueue.resolve(ws); // notify onWorkspaceInit callbacks
		return ws;
	}

	public static Promise<Workspace> onWorkspace(Consumer<Workspace> callback) {
		Promise<Workspace> p = workspaceQueue.getPromise();
		return p.thenAccept(workspace -> callback.accept(workspace))
			.onFailure(failure -> logger.logError("onWorkspace callback failed", failure));
	}

	public static Promise<Workspace> onWorkspaceAsync(Consumer<Workspace> callback) {
		Promise<Workspace> p = workspaceQueue.getPromise();
		return p.then(resolved -> {
			Workspace workspace = resolved.getValue();
			Deferred<Workspace> completion = promiseFactory().deferred();
			Display.getDefault()
				.asyncExec(() -> {
					try {
						callback.accept(workspace);
						completion.resolve(workspace);
					} catch (Throwable e) {
						completion.fail(e);
					}
				});
			return completion.getPromise();
		})
			.onFailure(failure -> logger.logError("onWorkspaceAsync callback failed", failure));
	}

	public static PromiseFactory promiseFactory() {
		return Processor.getPromiseFactory();
	}

	public static boolean isWorkspaceInited() {
		return workspace != null;
	}

	private static File getWorkspaceDirectory() throws CoreException {
		IWorkspaceRoot eclipseWorkspace = ResourcesPlugin.getWorkspace()
			.getRoot();

		IProject cnfProject = eclipseWorkspace.getProject(Workspace.BNDDIR);
		if (!cnfProject.exists())
			cnfProject = eclipseWorkspace.getProject(Workspace.CNFDIR);

		if (cnfProject.exists()) {
			if (!cnfProject.isOpen())
				cnfProject.open(null);
			return cnfProject.getLocation()
				.toFile()
				.getParentFile();
		}

		return null;
	}

	public static boolean hasWorkspaceDirectory() {
		try {
			return getWorkspaceDirectory() != null;
		} catch (CoreException e) {
			return false;
		}
	}

	private static void addCnfChangeListener(final Workspace workspace) {
		ResourcesPlugin.getWorkspace()
			.addResourceChangeListener(event -> {
				if (Central.getInstance() == null) { // plugin is not active
					return;
				}
				if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
					return;
				}
				IResourceDelta rootDelta = event.getDelta();
				if (isCnfChanged(workspace, rootDelta)) {
					logger.logInfo("cnf changed; refreshing workspace", null);
					workspace.refresh();
				}
			});
	}

	private static boolean isCnfChanged(Workspace workspace, IResourceDelta rootDelta) {
		try {
			IPath path = toPath(workspace.getPropertiesFile());
			if (path != null && rootDelta.findMember(path) != null) {
				return true;
			}
			List<File> includedFiles = workspace.getIncluded();
			if (includedFiles != null) {
				for (File includedFile : includedFiles) {
					path = toPath(includedFile);
					if (path != null && rootDelta.findMember(path) != null) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			logger.logError("Central.isCnfChanged() failed", e);
		}
		return false;
	}

	public static boolean isChangeDelta(IResourceDelta delta) {
		if (IResourceDelta.MARKERS == delta.getFlags())
			return false;
		if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) == 0)
			return false;
		return true;
	}

	public void changed(Project model) {
		model.setChanged();
		for (ModelListener m : listeners)
			try {
				m.modelChanged(model);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public void addModelListener(ModelListener m) {
		if (!listeners.contains(m)) {
			listeners.add(m);
		}
	}

	public void removeModelListener(ModelListener m) {
		listeners.remove(m);
	}

	public static IJavaProject getJavaProject(Project model) {
		for (IProject iproj : ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects()) {
			if (iproj.getName()
				.equals(model.getName())) {
				IJavaProject ij = JavaCore.create(iproj);
				if (ij != null && ij.exists()) {
					return ij;
				}
				break; // current project is not a Java project
			}
		}
		return null;
	}

	public static IPath toPath(File file) throws Exception {
		File absolute = file.getCanonicalFile();
		return toFullPath(absolute).orElseGet(() -> {
			try {
				String workspacePath = getWorkspace().getBase()
					.getAbsolutePath();
				String absolutePath = absolute.getPath();
				if (absolutePath.startsWith(workspacePath))
					return new Path(absolutePath.substring(workspacePath.length()));
				return null;
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		});
	}

	public static IPath toPathMustBeInEclipseWorkspace(File file) throws Exception {
		File absolute = file.getCanonicalFile();
		return toFullPath(absolute).orElse(null);
	}

	private static Optional<IPath> toFullPath(File file) {
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace()
			.getRoot();
		IFile[] candidates = wsroot.findFilesForLocationURI(file.toURI());
		return Stream.of(candidates)
			.map(IFile::getFullPath)
			.min((a, b) -> Integer.compare(a.segmentCount(), b.segmentCount()));
	}

	public static void refresh(IPath path) {
		try {
			IResource r = ResourcesPlugin.getWorkspace()
				.getRoot()
				.findMember(path);
			if (r != null)
				return;

			IPath p = (IPath) path.clone();
			while (p.segmentCount() > 0) {
				p = p.removeLastSegments(1);
				IResource resource = ResourcesPlugin.getWorkspace()
					.getRoot()
					.findMember(p);
				if (resource != null) {
					resource.refreshLocal(IResource.DEPTH_INFINITE, null);
					return;
				}
			}
		} catch (Exception e) {
			logger.logError("While refreshing path " + path, e);
		}
	}

	public static void refreshPlugins() throws Exception {
		List<File> refreshedFiles = new ArrayList<>();
		List<Refreshable> rps = getWorkspace().getPlugins(Refreshable.class);
		boolean changed = false;
		boolean repoChanged = false;
		for (Refreshable rp : rps) {
			if (rp.refresh()) {
				changed = true;
				File root = rp.getRoot();
				if (root != null)
					refreshedFiles.add(root);
				if (rp instanceof RepositoryPlugin) {
					repoChanged = true;
				}
			}
		}

		//
		// If repos were refreshed then
		// we should also update the classpath
		// containers. We can force this by setting the "bndtools.refresh"
		// property.
		//

		if (changed) {
			try {

				for (File file : refreshedFiles) {
					refreshFile(file);
				}

				for (Project p : Central.getWorkspace()
					.getAllProjects()) {
					p.setChanged();
					for (ModelListener l : getInstance().listeners)
						l.modelChanged(p);
				}

				if (repoChanged) {
					repositoriesViewRefresher.repositoriesRefreshed();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

	}

	public static void refreshPlugin(Refreshable plugin) throws Exception {
		if (plugin.refresh()) {
			refreshFile(plugin.getRoot());
			for (Project p : Central.getWorkspace()
				.getAllProjects()) {
				p.setChanged();
				for (ModelListener l : getInstance().listeners)
					l.modelChanged(p);
			}
			if (plugin instanceof RepositoryPlugin) {
				repositoriesViewRefresher.repositoryRefreshed((RepositoryPlugin) plugin);
			}
		}
	}

	public static void refreshFile(File f) throws CoreException {
		refreshFile(f, null, false);
	}

	public static void refreshFile(File file, IProgressMonitor monitor, boolean derived) throws CoreException {
		IResource target = toResource(file);
		if (target == null) {
			return;
		}
		int depth = target.getType() == IResource.FILE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
		if (!target.isSynchronized(depth)) {
			target.refreshLocal(depth, monitor);
			if (target.exists() && (target.isDerived() != derived)) {
				target.setDerived(derived, monitor);
			}
		}
	}

	public static void refresh(Project p) throws Exception {
		IJavaProject jp = getJavaProject(p);
		if (jp != null)
			jp.getProject()
				.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	public void close() {
		repositoriesViewRefresher.close();
	}

	public static void invalidateIndex() {
		indexValid.set(false);
	}

	public static boolean needsIndexing() {
		return indexValid.compareAndSet(false, true);
	}

	public static Project getProject(File projectDir) throws Exception {
		return getWorkspace().getProjectFromFile(projectDir);
	}

	public static Project getProject(IProject p) throws Exception {
		return getProject(p.getLocation()
			.toFile());
	}

	/**
	 * Return the IResource associated with a file
	 *
	 * @param file
	 */

	public static IResource toResource(File file) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
			.getRoot();
		return toFullPath(file).map(p -> file.isDirectory() ? root.getFolder(p) : root.getFile(p))
			.orElse(null);
	}

	/**
	 * Reentrant lock for serializing access to bnd code.
	 */
	private static final class BndLock extends ReentrantLock {
		private static final long	serialVersionUID	= 1L;
		private final AtomicLong	progress			= new AtomicLong();

		long progress() {
			return progress.get();
		}

		@Override
		public void unlock() {
			progress.incrementAndGet();
			super.unlock();
		}

		Throwable getOwnerCause() {
			final Thread owner = getOwner();
			if (owner == null) {
				return null;
			}
			final Throwable cause = new Throwable(
				owner + " owns the bndLock\n\nFull thread dump:\n" + dumpAllThreads() + "Owner stacktrace:");
			cause.setStackTrace(owner.getStackTrace());
			return cause;
		}

		private String dumpAllThreads() {
			ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
			return Arrays
				.stream(threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(),
					threadMXBean.isSynchronizerUsageSupported()))
				.map(Object::toString)
				.collect(Collectors.joining());
		}
	}

	private static final BndLock bndLock = new BndLock();

	/**
	 * Used to serialize access to bnd code which is not thread safe.
	 *
	 * @param callable The code to execute while holding the central lock.
	 * @return The result of the specified callable.
	 * @throws InterruptedException If the thread is interrupted while waiting
	 *             for the lock.
	 * @throws TimeoutException If the lock was not obtained within the timeout
	 *             period.
	 * @throws Exception If the callable throws an exception.
	 */
	public static <V> V bndCall(Callable<V> callable) throws Exception {
		return bndCall(callable, null);
	}

	/**
	 * Used to serialize access to bnd code which is not thread safe.
	 *
	 * @param callable The code to execute while holding the central lock.
	 * @param monitor If the monitor is cancelled, a TimeoutException will be
	 *            thrown.
	 * @return The result of the specified callable.
	 * @throws InterruptedException If the thread is interrupted while waiting
	 *             for the lock.
	 * @throws TimeoutException If the lock was not obtained within the timeout
	 *             period or the specified monitor is cancelled while waiting to
	 *             obtain the lock.
	 * @throws Exception If the callable throws an exception.
	 */
	public static <V> V bndCall(Callable<V> callable, IProgressMonitor monitor) throws Exception {
		boolean interrupted = Thread.interrupted();
		try {
			long progress = bndLock.progress();
			for (int i = 0; i < 120; i++) {
				if ((monitor != null) && monitor.isCanceled()) {
					throw (CancellationException) new CancellationException("Cancelled waiting to acquire bndLock["
						+ bndLock + "]; has waiters: " + bndLock.getQueueLength()).initCause(bndLock.getOwnerCause());
				}
				boolean locked = false;
				try {
					locked = bndLock.tryLock(1, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					interrupted = true;
					throw e;
				}
				if (locked) {
					try {
						return callable.call();
					} finally {
						bndLock.unlock();
					}
				}
				long currentProgress = bndLock.progress();
				if (progress != currentProgress) {
					progress = currentProgress;
					i = 0;
				}
			}
			throw (TimeoutException) new TimeoutException(
				"Unable to acquire bndLock[" + bndLock + "]; has waiters: " + bndLock.getQueueLength())
					.initCause(bndLock.getOwnerCause());
		} finally {
			if (interrupted) {
				Thread.currentThread()
					.interrupt();
			}
		}
	}

	/**
	 * Convert a processor to a status object
	 */

	public static IStatus toStatus(Processor processor, String message) {
		int severity = IStatus.INFO;
		List<IStatus> statuses = new ArrayList<>();
		for (String error : processor.getErrors()) {
			Status status = new Status(IStatus.ERROR, BndtoolsConstants.CORE_PLUGIN_ID, error);
			statuses.add(status);
			severity = IStatus.ERROR;
		}
		for (String warning : processor.getWarnings()) {
			Status status = new Status(IStatus.WARNING, BndtoolsConstants.CORE_PLUGIN_ID, warning);
			statuses.add(status);
			severity = IStatus.WARNING;
		}

		IStatus[] array = statuses.toArray(new IStatus[0]);
		return new MultiStatus(//
			BndtoolsConstants.CORE_PLUGIN_ID, //
			severity, //
			array, message, null);
	}

	/**
	 * Register a viewer with repositories
	 */

	public static void addRepositoriesViewer(TreeViewer viewer, RepositoriesViewRefresher.RefreshModel model) {
		repositoriesViewRefresher.addViewer(viewer, model);
	}

	/**
	 * Unregister a viewer with repositories
	 */
	public static void removeRepositoriesViewer(TreeViewer viewer) {
		repositoriesViewRefresher.removeViewer(viewer);
	}

	public static void setRepositories(TreeViewer viewer, RefreshModel model) {
		repositoriesViewRefresher.setRepositories(viewer, model);
	}
}
