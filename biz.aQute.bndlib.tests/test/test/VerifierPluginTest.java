package test;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.verifier.VerifierPlugin;
import junit.framework.TestCase;

public class VerifierPluginTest extends TestCase {

	public static void testNoVerifierPluginExecution() throws Exception {
		final AtomicBoolean executedCheck = new AtomicBoolean(false);

		AnalyzerPlugin analyzerPlugin = analyzer -> {
			// The following is null when AnalyzerPlugins execute
			Packages exports = analyzer.getExports();

			if (exports == null) {
				executedCheck.set(true);
			}

			return false;
		};

		try (Builder b = new Builder()) {
			b.setProperty("Bundle-SymbolicName", "p1");
			b.setProperty("Bundle-Version", "1.2.3");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(analyzerPlugin);

			Jar jar = b.build();
		}
		assertTrue(executedCheck.get());
	}

	public static void testVerifierPluginExecution() throws Exception {
		final AtomicBoolean executedCheck = new AtomicBoolean(false);

		VerifierPlugin verifier = analyzer -> {
			// the following is true only after the jar and manifest have
			// been analyzed
			Packages exports = analyzer.getExports();

			PackageRef packageRef = analyzer.getPackageRef("test/activator");

			if (exports.containsKey(packageRef)) {
				executedCheck.set(true);
			}
		};

		try (Builder b = new Builder()) {
			b.setProperty("Bundle-SymbolicName", "p1");
			b.setProperty("Bundle-Version", "1.2.3");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(verifier);

			Jar jar = b.build();
		}
		assertTrue(executedCheck.get());
	}

}
