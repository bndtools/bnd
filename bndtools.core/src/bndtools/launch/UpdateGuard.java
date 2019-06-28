package bndtools.launch;

import java.io.Closeable;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.build.api.BuildListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;

/**
 * This class watches the build and allows you to schedule an update some time
 * after the last build operation ended.
 */
public abstract class UpdateGuard implements Closeable {

	private static final int								GRACE_PERIOD	= 500;
	private final static Timer								timer			= new Timer(true);
	private final BundleContext								context;
	private final AtomicBoolean								closed			= new AtomicBoolean(true);
	private ServiceRegistration<BuildListener>				buildListener;
	private ServiceRegistration<RepositoryListenerPlugin>	repositoryListener;
	private TimerTask										trigger;

	public UpdateGuard(BundleContext context) {
		this.context = context;
	}

	public void open() {
		if (!closed.getAndSet(false))
			throw new IllegalStateException("Already opened");

		//
		// We wait for build changes. We never update during a build
		// and we will wait a bit after a build ends.
		//

		buildListener = context.registerService(BuildListener.class, new BuildListener() {

			@Override
			public void buildStarting(IProject project) {
				off();
			}

			@Override
			public void builtBundles(IProject project, IPath[] paths) {}

			@Override
			public void released(IProject project) {
				on();
			}
		}, null);

		//
		// We also wait for repository changes, though they will generally cause
		// a rebuild as well.
		//
		repositoryListener = context.registerService(RepositoryListenerPlugin.class, new RepositoryListenerPlugin() {

			@Override
			public void repositoryRefreshed(RepositoryPlugin repository) {
				on();
			}

			@Override
			public void repositoriesRefreshed() {
				on();
			}

			@Override
			public void bundleRemoved(RepositoryPlugin repository, Jar jar, File file) {
				on();
			}

			@Override
			public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {
				on();
			}
		}, null);

	}

	private void off() {
		if (closed.get())
			return;

		synchronized (timer) {
			if (trigger != null)
				trigger.cancel();
			trigger = null;
		}
	}

	private void on() {
		if (closed.get())
			return;

		synchronized (timer) {
			if (trigger != null)
				trigger.cancel();
			trigger = new TimerTask() {

				@Override
				public void run() {
					if (closed.get())
						return;
					update();
				}
			};
			timer.schedule(trigger, GRACE_PERIOD);
		}
	}

	protected abstract void update();

	public void kick() {
		on();
	}

	@Override
	public void close() {
		if (closed.getAndSet(true))
			return;

		try {
			buildListener.unregister();
		} catch (Exception e) {
			// ignore
		}
		try {
			repositoryListener.unregister();
		} catch (Exception e) {
			// ignore
		}
	}
}
