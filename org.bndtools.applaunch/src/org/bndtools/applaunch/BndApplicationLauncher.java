package org.bndtools.applaunch;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.service.runnable.ParameterizedRunnable;
import org.osgi.framework.BundleContext;

class BndApplicationLauncher implements ApplicationLauncher {

	private final Logger		log	= Logger.getLogger(BndApplicationLauncher.class.getPackage()
		.getName());
	private final BundleContext	bc;
	private final boolean		initialize;

	/**
	 * @param initialize {@code true} if {@code -initialize} was given: the
	 *            main thread returns without running the application, which
	 *            lets the launcher shut the framework down like Eclipse does
	 */
	BndApplicationLauncher(BundleContext context, boolean initialize) {
		this.bc = context;
		this.initialize = initialize;
	}

	@Override
	public void launch(final ParameterizedRunnable runnable, final Object context) {
		log.log(Level.FINE,
			"Received launch request from Eclipse application service, registering java.lang.Runnable{main.thread=true}");
		Runnable service = () -> {
			if (initialize) {
				log.info("-initialize specified: the configuration is initialized and the Eclipse application is not run.");
				return;
			}
			try {
				log.log(Level.FINE, "Executing appplication on thread {0} ({1}).", new Object[] {
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
		log.warning("Ignoring shutdown call");
	}
}
