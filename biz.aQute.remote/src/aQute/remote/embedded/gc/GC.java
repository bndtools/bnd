package aQute.remote.embedded.gc;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;

public class GC implements BundleActivator {

	private BundleContext context;

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		context.addBundleListener(event -> {
			if (event.getType() == BundleEvent.UNINSTALLED) {
				Bundle b = event.getBundle();
				String embedded = b.getHeaders()
					.get("Bnd-Embedded");
				if (embedded != null) {
					uninstalled(b);
				}
			}
		});
	}

	void uninstalled(Bundle parent) {
		String bsn = parent.getSymbolicName();

		for (Bundle b : context.getBundles()) {
			if (b.getLocation()
				.startsWith(bsn + "@"))
				;
			try {
				b.uninstall();
			} catch (BundleException e) {
				System.err.println("Oops, could not install sub bundle " + b + " " + e);
			}
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {}

}
