package bndtools;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.facade.ExtensionFacade;
import org.bndtools.headless.build.manager.api.HeadlessBuildManager;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.osgi.Processor;
import aQute.bnd.version.Version;
import bndtools.services.WorkspaceURLStreamHandlerService;

public class Plugin extends AbstractUIPlugin {

	static final Class<?>											EXTENSION_FACADE	= ExtensionFacade.class;

	public static final String										PLUGIN_ID			= "bndtools.core";
	public static final String										BND_EDITOR_ID		= PLUGIN_ID + ".bndEditor";
	public static final String										IMG_OK				= "OK";

	public static final Version										DEFAULT_VERSION		= new Version(0, 0, 0);

	public static final String										BNDTOOLS_NATURE		= "bndtools.core.bndnature";

	private static volatile Plugin									plugin;

	private BundleContext											bundleContext;
	private Activator												bndActivator;

	private volatile ServiceTracker<IWorkspace, IWorkspace>			workspaceTracker;
	private volatile ServiceRegistration<URLStreamHandlerService>	urlHandlerReg;
	private volatile HeadlessBuildManagerTracker					headlessBuildManager;
	private volatile VersionControlIgnoresManagerTracker			versionControlIgnoresManager;

	private volatile ScheduledExecutorService						scheduler;

	@Override
	public void start(BundleContext context) throws Exception {
		registerWorkspaceURLHandler(context);
		super.start(context);
		plugin = this;
		this.bundleContext = context;

		scheduler = Executors.newScheduledThreadPool(4);

		bndActivator = new Activator();
		bndActivator.start(context);

		versionControlIgnoresManager = new VersionControlIgnoresManagerTracker(context);
		versionControlIgnoresManager.open();

		headlessBuildManager = new HeadlessBuildManagerTracker(context);
		headlessBuildManager.open();
	}

	private void registerWorkspaceURLHandler(BundleContext context) {
		workspaceTracker = new ServiceTracker<>(context, IWorkspace.class.getName(), null);
		workspaceTracker.open();

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] {
			WorkspaceURLStreamHandlerService.PROTOCOL
		});
		urlHandlerReg = context.registerService(URLStreamHandlerService.class,
			new WorkspaceURLStreamHandlerService(workspaceTracker), props);
	}

	private void unregisterWorkspaceURLHandler() {
		urlHandlerReg.unregister();
		workspaceTracker.close();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		bndActivator.stop(context);
		headlessBuildManager.close();
		versionControlIgnoresManager.close();
		this.bundleContext = null;
		plugin = null;
		super.stop(context);
		unregisterWorkspaceURLHandler();
		scheduler.shutdown();
		Icons.clear();
	}

	public static Plugin getDefault() {
		return plugin;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public static void report(boolean warnings, @SuppressWarnings("unused") boolean acknowledge, Processor reporter,
		final String title, final String extra) {
		if (reporter.getErrors()
			.size() > 0
			|| (warnings && reporter.getWarnings()
				.size() > 0)) {
			final StringBuffer sb = new StringBuffer();
			sb.append("\n");
			if (reporter.getErrors()
				.size() > 0) {
				sb.append("[Errors]\n");
				for (String msg : reporter.getErrors()) {
					sb.append(msg);
					sb.append("\n");
				}
			}
			sb.append("\n");
			if (reporter.getWarnings()
				.size() > 0) {
				sb.append("[Warnings]\n");
				for (String msg : reporter.getWarnings()) {
					sb.append(msg);
					sb.append("\n");
				}
			}
			final Status s = new Status(IStatus.ERROR, PLUGIN_ID, 0, sb.toString(), null);
			reporter.clear();

			async(() -> ErrorDialog.openError(null, title, title + "\n" + extra, s));

		} else {
			message(title + " : ok");
		}
	}

	public static void message(final String msg) {
		async(() -> MessageDialog.openInformation(null, "Bnd", msg));
	}

	static void async(Runnable run) {
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		display.asyncExec(run);
	}

	public static void error(List<String> errors) {
		final StringBuffer sb = new StringBuffer();
		for (String msg : errors) {
			sb.append(msg);
			sb.append("\n");
		}

		async(() -> {
			Status s = new Status(IStatus.ERROR, PLUGIN_ID, 0, "", null);
			ErrorDialog.openError(null, "Errors during bundle generation", sb.toString(), s);
		});
	}

	static final AtomicBoolean busy = new AtomicBoolean(false);

	public void error(final String msg, final Throwable t) {
		Status s = new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, t);
		getLog().log(s);
		async(() -> {
			if (!busy.compareAndSet(false, true)) {
				Status s1 = new Status(IStatus.ERROR, PLUGIN_ID, 0, "", null);
				ErrorDialog.openError(null, "Errors during bundle generation", msg + " " + t.getMessage(), s1);

				busy.set(false);
			}
		});
	}

	public static void warning(List<String> errors) {
		final StringBuffer sb = new StringBuffer();
		for (String msg : errors) {
			sb.append(msg);
			sb.append("\n");
		}
		async(() -> {
			Status s = new Status(IStatus.WARNING, PLUGIN_ID, 0, "", null);
			ErrorDialog.openError(null, "Warnings during bundle generation", sb.toString(), s);
		});
	}

	public static ImageDescriptor imageDescriptorFromPlugin(String imageFilePath) {
		return Icons.desc(imageFilePath);
	}

	public HeadlessBuildManager getHeadlessBuildManager() {
		return headlessBuildManager;
	}

	public VersionControlIgnoresManager getVersionControlIgnoresManager() {
		return versionControlIgnoresManager;
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		registry.put(IMG_OK, imageDescriptorFromPlugin("icons/testok.png"));
	}
}
