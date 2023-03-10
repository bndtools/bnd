package aQute.xlaunchpad;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;

/**
 * Collect tests for running bndlib in an OSGi framework without Eclipse. This
 * does not seem a common use case but there are some class loading issues to
 * consider for the plugins.
 */
public class WorkspaceTest {
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("workspace.bndrun");

	/**
	 * Check if the ActivelyClosingClassLoader will load from other bundles.
	 */

	@Test
	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	public void test() throws Exception {
		try (Launchpad lp = builder.create()) {
			Bundle bndlib = lp.getBundle("biz.aQute.bndlib")
				.get();
			Class accl = bndlib.loadClass("aQute.bnd.osgi.ActivelyClosingClassLoader");

			Constructor constructor = accl.getDeclaredConstructor();
			constructor.setAccessible(true);
			ClassLoader cl = (ClassLoader) constructor.newInstance();
			Class<?> resolver = cl.loadClass("biz.aQute.resolve.BndResolver");
			assertThat(resolver).isNotNull();
		}
	}
}
