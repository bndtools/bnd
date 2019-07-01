package aQute.launcher;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * This start level handler looks at the parms.startlevels parameter of the
 * launcher. If it is set it will handle startlevels, otherwise it is not doing
 * anything.
 * <p>
 * Startlevels can be assigned with the {@code startlevel} attribute on the
 * {@code -runbundles} instruction. If not set, the default will be the highest
 * set startlevel number + 1.
 * <p>
 * If there are startlevels, the user can still override the beginning start
 * level. This can be useful if you want to do something before bundles are
 * started in startlevel > 1. You can set the beginning startlevel using the
 * property {@linkplain Constants#FRAMEWORK_BEGINNING_STARTLEVEL}.
 * <p>
 * If this property is not set, we'll set the framework startlevel to the
 * highest found startlevel in the parameters + 2.
 */
interface StartLevelHandler {

	default boolean hasStartLevels() {
		return false;
	}

	default int getStartLevelByIndex(Integer n) {
		return 1;
	}

	default boolean hasStartlevels() {
		return false;
	}

	default void setStartLevelByIndex(Bundle b, Integer index) {}

	default int getStartLevel(Framework framework) {
		return framework.adapt(FrameworkStartLevel.class)
			.getStartLevel();
	}

	default int setFrameworkStartLevel(Framework framework, int startlevel, FrameworkListener... ls) {
		int previous = getStartLevel(framework);
		framework.adapt(FrameworkStartLevel.class)
			.setStartLevel(startlevel, ls);
		return previous;
	}

	default void beforeStart(Framework systemBundle) {}

	default void afterStart(Framework systemBundle) {}

	default int getBundleStartLevel(Bundle bundle) {
		return bundle.adapt(BundleStartLevel.class)
			.getStartLevel();
	}

	default void setStartLevel(Bundle b, int startLevel) {
		b.adapt(BundleStartLevel.class)
			.setStartLevel(startLevel);
	}

	default void sync() {}

	static StartLevelHandler create(Launcher launcher, Properties properties, List<Integer> startLevels) {

		if (startLevels != null && !startLevels.isEmpty()) {

			int maxStartLevel = startLevels.stream()
				.max(Integer::compare)
				.orElse(-1);

			if (maxStartLevel > 0) {

				//
				// Ok, we do use start levels.
				//
				launcher.trace("startlevel: handled %s", startLevels);

				String requestedBeginningStartLevel = properties.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);

				int beginningStartLevel;

				if (requestedBeginningStartLevel == null) {
					beginningStartLevel = maxStartLevel + 2;
				} else if (requestedBeginningStartLevel.matches("\\s*\\d+\\s*")) {
					beginningStartLevel = Integer.parseInt(requestedBeginningStartLevel.trim());
				} else {
					launcher.trace(
						"############# Invalid format of run property %s for setting beginning startlevel : %s. Will move to ",
						Constants.FRAMEWORK_BEGINNING_STARTLEVEL, requestedBeginningStartLevel);
					beginningStartLevel = maxStartLevel + 2;
				}

				//
				// We need to remove it otherwise the framework reacts to it
				//

				properties.remove(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);

				return new StartLevelHandler() {
					final CountDownLatch latch = new CountDownLatch(1);

					@Override
					public void beforeStart(Framework systemBundle) {
						launcher.trace("startlevel: default=%s, beginning=%s, now moving to 1", maxStartLevel + 1,
							beginningStartLevel);
						assert getStartLevel(systemBundle) == 0;
						setFrameworkStartLevel(systemBundle, 1);
					}

					@Override
					public void setStartLevelByIndex(Bundle b, Integer index) {
						int level = index == null ? maxStartLevel + 1 : getStartLevelByIndex(index);
						setStartLevel(b, level);
						launcher.trace("startlevel: bundle %s <- %s", b, level);
					}

					@Override
					public void afterStart(Framework systemBundle) {
						setFrameworkStartLevel(systemBundle, beginningStartLevel, (event) -> {
							launcher.trace("startlevel: notified reached final level %s : %s", beginningStartLevel,
								event);
							latch.countDown();
						});
						launcher.trace("startlevel: beginning level %s", beginningStartLevel);
					}

					@Override
					public void sync() {
						try {
							latch.await();
						} catch (InterruptedException ie) {
							throw new RuntimeException(ie);
						}
					}

					@Override
					public int getStartLevelByIndex(Integer n) {
						if (n == null)
							return maxStartLevel + 1;

						int level = startLevels.size() > n ? startLevels.get(n) : -1;

						if (level == -1)
							return maxStartLevel + 1;

						return level;
					}

					@Override
					public boolean hasStartlevels() {
						return true;
					}

				};
			}
		}
		launcher.trace("startlevel: not handled");
		return new StartLevelHandler() {};
	}

}
