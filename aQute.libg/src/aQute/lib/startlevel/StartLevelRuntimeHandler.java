package aQute.lib.startlevel;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

import aQute.lib.bundles.BundleIdentity;
import aQute.libg.parameters.ParameterMap;

/**
 * Support to handle start levels in a launcher. This code is related to code in
 * the Project Launcher. It is in aQute.lib so it can be included easily in the
 * Launcher, the Remote launcher, and Launchpad.
 * <p>
 * This class is not threadsafe!
 */
public class StartLevelRuntimeHandler implements Closeable {

	/**
	 * If this property is set we take on start levels, if this property is not
	 * set we ignore the startlevels completely. This is defined in
	 * aQute.bnd.osgi.Constants
	 */
	public static String	LAUNCH_STARTLEVEL_DEFAULT	= "launch.startlevel.default";
	public static String	LAUNCH_RUNBUNDLES_ATTRS		= "launch.runbundles.attrs";

	/**
	 * Indicate if this class supports start levels or not.
	 *
	 * @return true if this class supports startlevels
	 */
	public boolean hasStartLevels() {
		return false;
	}

	/**
	 * Set the start level of a bundle
	 *
	 * @param b the bundle
	 */
	public void setStartLevel(Bundle b) {}

	/**
	 * Answer the current framework start level
	 *
	 * @param framework the framework
	 * @return the current start level of the framework
	 */
	public int getFrameworkStartLevel(Framework framework) {
		return framework.adapt(FrameworkStartLevel.class)
			.getStartLevel();
	}

	/**
	 * Set the default start level of newly installed bundles
	 *
	 * @param framework the framework
	 * @param level the default start level
	 */
	public void setDefaultStartlevel(Framework framework, int level) {
		framework.adapt(FrameworkStartLevel.class)
			.setInitialBundleStartLevel(level);
	}

	/**
	 * Set the framework start level and return previous
	 *
	 * @param framework the framework
	 * @param startlevel the start level to set
	 * @param ls listeners
	 * @return the previous start level of the framework
	 */
	public int setFrameworkStartLevel(Framework framework, int startlevel, FrameworkListener... ls) {
		int previous = getFrameworkStartLevel(framework);
		framework.adapt(FrameworkStartLevel.class)
			.setStartLevel(startlevel, ls);
		return previous;
	}

	/**
	 * Get a bundle's start level
	 *
	 * @param bundle the bundle to query
	 * @return the start level > 0
	 */
	public int getBundleStartLevel(Bundle bundle) {
		return bundle.adapt(BundleStartLevel.class)
			.getStartLevel();
	}

	/**
	 * Set a bundle's start level
	 *
	 * @param bundle the bundle to query
	 * @param startlevel start level to set, > 0
	 */
	public void setBundleStartLevel(Bundle bundle, int startlevel) {
		bundle.adapt(BundleStartLevel.class)
			.setStartLevel(startlevel);
	}

	/**
	 * Must be called before the framework is started.
	 * <p>
	 * ensure systemBundle.getState() == INIT and startlevel systemBundle == 0
	 *
	 * @param systemBundle the framework
	 */
	public void beforeStart(Framework systemBundle) {}

	/**
	 * When the configuration properties have been updated
	 *
	 * @param configuration the configuration properties
	 */
	public void updateConfiguration(Map<String, ?> configuration) {}

	/**
	 * Called after the framework is started and the launcher is ready
	 */
	public void afterStart() {}

	/**
	 * Wait for the framework to reach its start level. Must be called after the
	 * {@link #afterStart()} method. Will return when the framework has
	 * traversed all start levels.
	 */
	public void sync() {}

	/**
	 * Close this object
	 */

	@Override
	public void close() {}

	/**
	 * Create a start level handler. If the {@link #LAUNCH_STARTLEVEL_DEFAULT}
	 * property is set we create an active handler that will direct the
	 * framework properly according to the settings in Project Launcher. If not
	 * set, a dummy is returned that does not do anything
	 *
	 * @param outerConfiguration the properties as set by the Project Launcher
	 * @return an active or dummy {@link StartLevelRuntimeHandler}
	 */
	static public StartLevelRuntimeHandler create(Trace logger, Map<String, String> outerConfiguration) {

		String defaultStartlevelString = outerConfiguration.get(LAUNCH_STARTLEVEL_DEFAULT);
		if (defaultStartlevelString == null) {
			logger.trace("startlevel: not handled");
			return absent();
		}

		int defaultStartlevel = toInt(defaultStartlevelString, 1);
		int beginningStartlevel = toInt(outerConfiguration.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL), 1);
		outerConfiguration.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "1");

		logger.trace("startlevel: handled begin=%s default=%s", beginningStartlevel, defaultStartlevel);

		//
		// We need to remove it otherwise the framework reacts to it
		//

		return new StartLevelRuntimeHandler() {
			CountDownLatch							latch		= new CountDownLatch(1);
			private Framework						systemBundle;
			private Map<BundleIdentity, Integer>	startlevels	= new HashMap<>();
			private Map<Bundle, BundleIdentity>		installed	= new HashMap<>();

			@Override
			public void beforeStart(Framework systemBundle) {
				assert getFrameworkStartLevel(
					systemBundle) == 0 : "Expects the framework to be in init mode, not yet started";

				this.systemBundle = systemBundle;

				updateConfiguration(outerConfiguration);

				setDefaultStartlevel(this.systemBundle, defaultStartlevel);

				systemBundle.getBundleContext()
					.addBundleListener(new SynchronousBundleListener() {

						@Override
						public void bundleChanged(BundleEvent event) {
							Bundle bundle = event.getBundle();
							if (bundle.getBundleId() == 0)
								return;

							if (bundle.getSymbolicName() == null) {
								logger.trace("Found bundle without a bsn %s, ignoring", bundle);
								return;
							}

							BundleIdentity id = installed.computeIfAbsent(bundle, BundleIdentity::new);
							if (event.getType() == BundleEvent.INSTALLED || event.getType() == BundleEvent.UPDATED) {
								setStartlevel(bundle, id);
							} else if (event.getType() == BundleEvent.UNINSTALLED) {
								installed.remove(bundle);
							}
						}

					});
				logger.trace("startlevel: default=%s, beginning=%s", defaultStartlevel, beginningStartlevel);

			}

			@Override
			public void afterStart() {
				setFrameworkStartLevel(systemBundle, beginningStartlevel, (event) -> {
					logger.trace("startlevel: notified reached final level %s : %s", beginningStartlevel, event);
					latch.countDown();
				});
				logger.trace("startlevel change begin: beginning level %s", beginningStartlevel);
			}

			@Override
			public void sync() {
				try {
					latch.await();
				} catch (InterruptedException ie) {
					Thread.interrupted();
					throw new RuntimeException(ie);
				}
			}

			@Override
			public boolean hasStartLevels() {
				return true;
			}

			@Override
			public void updateConfiguration(Map<String, ?> configuration) {
				new ParameterMap((String) configuration.get(LAUNCH_RUNBUNDLES_ATTRS)).entrySet()
					.forEach(entry -> {
						String bsn = ParameterMap.removeDuplicateMarker(entry.getKey());
						String version = entry.getValue()
							.getVersion();
						BundleIdentity id = new BundleIdentity(bsn, version);

						int startlevel = toInt(entry.getValue()
							.get("startlevel"), -1);
						if (startlevel > 0) {
							startlevels.put(id, startlevel);
						}
					});

				installed.forEach(this::setStartlevel);
			}

			private void setStartlevel(Bundle bundle, BundleIdentity id) {
				if (bundle.getState() != Bundle.UNINSTALLED) {
					int level = startlevels.getOrDefault(id, -1);
					if (level == -1)
						level = defaultStartlevel;

					setBundleStartLevel(bundle, level);
					logger.trace("startlevel: %s <- %s", bundle, level);
				}
			}

		};
	}

	static int toInt(Object object, int defltValue) {
		if (object == null)
			return defltValue;

		String s = object.toString()
			.trim();
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return defltValue;
		}
	}

	public static StartLevelRuntimeHandler absent() {
		return new StartLevelRuntimeHandler() {};
	}

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	public static StartLevelRuntimeHandler create(Trace reporter, Properties properties) {
		return create(reporter, (Map) properties);
	}
}
