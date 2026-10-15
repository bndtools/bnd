package bndtools.core.test.launch.rendered;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.service.runnable.ParameterizedRunnable;
import org.osgi.framework.BundleContext;

/**
 * Variant of the headless launcher of {@code bndtools.core.test.launch} that
 * renders the workbench with the default SWT presentation engine. This is
 * required for SWTBot tests which drive real widgets. It only bridges the bnd
 * launcher main thread to the Eclipse application; it does not register a
 * {@code NullContextPresentationEngine}.
 */
class RenderedLauncher implements ApplicationLauncher {

	private final Logger		log	= Logger.getLogger(RenderedLauncher.class.getPackage()
		.getName());
	private final BundleContext	bc;

	public RenderedLauncher(BundleContext bc) {
		this.bc = bc;
	}

	@Override
	public void launch(final ParameterizedRunnable runnable, final Object context) {
		log.log(Level.FINE,
			"Received launch request from Eclipse application service, registering java.lang.Runnable{main.thread=true}");
		Runnable service = () -> {
			try {
				log.log(Level.FINE, "Executing application on thread {0} ({1}).", new Object[] {
					Thread.currentThread()
						.getName(),
					Thread.currentThread()
						.getId()
				});
				runnable.run(context);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error executing application", e);
			}
		};

		Dictionary<String, String> svcProps = new Hashtable<>();
		svcProps.put("main.thread", "true");
		bc.registerService(Runnable.class, service, svcProps);
	}

	@Override
	public void shutdown() {
		log.fine("Received shutdown request");
	}
}
