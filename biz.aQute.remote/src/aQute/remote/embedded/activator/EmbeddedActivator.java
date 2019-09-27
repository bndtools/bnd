package aQute.remote.embedded.activator;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.FrameworkWiring;

import aQute.lib.regex.PatternConstants;

/**
 * This activator is placed in bundles that will load embedded bundles when
 * activated. We mark installed bundles with our bsn + "@" their bsn. This makes
 * it quick and easy to update the constellation.
 */
public class EmbeddedActivator implements BundleActivator {
	final List<Bundle>				bundles			= new ArrayList<>();
	private final static String		VERSION_S		= "(?:\\d{1,9})(?:\\.(?:\\d{1,9})(?:\\.(?:\\d{1,9})(?:\\.(?:"
		+ PatternConstants.TOKEN + "))?)?)?";

	private final static Pattern	BSN_VERSION_P	= Pattern
		.compile("\\s*(" + PatternConstants.SYMBOLICNAME + ")\\s*=\\s*(" + VERSION_S + ")\\s*");

	/**
	 * The activator start will install any bundles that are not already
	 * installed and update embedded bundles that are refreshed
	 */

	@Override
	public void start(BundleContext context) throws Exception {
		try {
			Bundle ours = context.getBundle();
			String bsn = ours.getSymbolicName();
			Version version = ours.getVersion();

			String embedded = ours.getHeaders()
				.get("Bnd-Embedded");
			if (embedded == null)
				throw new IllegalArgumentException("Requires a Bnd-Embedded header");

			Map<String, Version> index = new HashMap<>();

			String[] clauses = embedded.split("\\s*,\\s*");
			for (String clause : clauses) {
				Matcher m = BSN_VERSION_P.matcher(clause);
				if (!m.matches())
					throw new IllegalArgumentException(
						"Funny clause in Bnd-Embedded header " + clause + ", expecting <bsn>=<version>");

				String tbsn = m.group(1);
				String tversion = m.group(2);

				index.put(tbsn, new Version(tversion));
			}

			List<Bundle> toStop = new ArrayList<>();
			List<Bundle> toUpdate = new ArrayList<>();

			Pattern bsn_p = Pattern.compile(Pattern.quote(bsn) + "@(.*)");
			for (Bundle b : context.getBundles()) {
				if (b == ours)
					continue;

				String location = b.getLocation();
				Matcher m = bsn_p.matcher(location);
				if (m.matches()) {
					String tbsn = b.getSymbolicName();
					Version expected = index.remove(tbsn);

					if (expected == null) {

						//
						// No longer there
						//

						b.uninstall();

					} else {

						//
						// We stop all our bundles
						//

						if (isGC(b)) {

							//
							// But the GC bundle is special. We do not
							// automatically stop it and we only
							// update it when we have a better version
							//

							if (b.getVersion()
								.compareTo(expected) > 0)
								continue;
						}

						toStop.add(b);

						//
						// And if the version differs we do
						// an update
						//

						if (!expected.equals(b.getVersion())) {
							toUpdate.add(b);
						}
					}
				}
			}

			List<Bundle> toStart = new ArrayList<>();

			for (Bundle b : toStop) {
				b.stop();
				toStart.add(b);
			}

			refresh(context, toStop);

			for (String tbsn : index.keySet()) {
				URL url = ours.getEntry("jar/" + tbsn + ".jar");
				Bundle b = context.installBundle(bsn + "@" + tbsn, url.openStream());
				toStart.add(b);
			}

			for (Bundle b : toUpdate) {
				URL url = ours.getEntry("jar/" + b.getSymbolicName() + ".jar");
				b.update(url.openStream());
				toStart.add(b);
			}

			for (Bundle b : toStart) {
				b.start();
			}
		} catch (Exception e) {
			stop(context);
		}
	}

	/*
	 * Refresh the stopped bundles
	 */
	void refresh(BundleContext context, List<Bundle> toStop) throws InterruptedException {
		Bundle bundle = context.getBundle(0);
		FrameworkWiring framework = bundle.adapt(FrameworkWiring.class);
		final Semaphore s = new Semaphore(0);

		framework.refreshBundles(toStop, event -> s.release());

		s.tryAcquire(10, TimeUnit.SECONDS);
	}

	/**
	 * Stop any bundles that our in our domain
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		Bundle ours = context.getBundle();
		String bsn = ours.getSymbolicName();
		Pattern bsn_p = Pattern.compile(Pattern.quote(bsn) + "@(.*)");
		List<Exception> exceptions = new ArrayList<>();

		for (Bundle b : context.getBundles()) {
			if (b == ours || isGC(b))
				continue;

			String location = b.getLocation();
			Matcher m = bsn_p.matcher(location);
			if (m.matches())
				try {
					b.stop();
				} catch (Exception e) {
					exceptions.add(e);
				}
		}

		if (exceptions.isEmpty())
			return;

		throw new Exception("Failed to stop all sub bundles " + exceptions);
	}

	private boolean isGC(Bundle b) {
		return "biz.aQute.remote.gc".equals(b.getSymbolicName());
	}

}
