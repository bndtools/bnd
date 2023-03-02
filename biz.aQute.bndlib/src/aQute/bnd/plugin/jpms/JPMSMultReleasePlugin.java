package aQute.bnd.plugin.jpms;

import static aQute.bnd.osgi.Constants.JPMS_MULTI_RELEASE;
import static aQute.bnd.osgi.JPMSModule.MANIFEST_PATH;
import static aQute.bnd.osgi.JPMSModule.MODULE_INFO_CLASS;
import static aQute.bnd.osgi.JPMSModule.MULTI_RELEASE_HEADER;

import java.util.SortedSet;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.JPMSModule;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.ManifestResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.ManifestPlugin;

/**
 * Handles multi release jars. If the manif
 */
public class JPMSMultReleasePlugin implements ManifestPlugin {

	/**
	 * We need to calculate alternative files for versioned JARS
	 */
	@Override
	public void mainSet(Analyzer analyzer, Manifest manifest) throws Exception {
		if (!analyzer.is(JPMS_MULTI_RELEASE))
			return;

		JPMSModule jpms = new JPMSModule(analyzer.getJar());
		SortedSet<Integer> releases = jpms.getVersions();

		if (releases.isEmpty())
			return;

		manifest.getMainAttributes()
			.putValue(MULTI_RELEASE_HEADER, "true");
		Analyzer copy = Analyzer.copy(analyzer);

		copy.addBasicPlugin(new JPMSModuleInfoPlugin());
		if (copy.getProperty(Constants.JPMS_MODULE_INFO) == null)
			copy.setProperty(Constants.JPMS_MODULE_INFO, "");

		Jar baseline = copy.getJar();
		baseline.removePrefix(JPMSModule.VERSIONS_PATH);

		baseline.setManifest(manifest);
		baseline.putResource(MANIFEST_PATH, new ManifestResource(manifest));

		Manifest older = relevant(manifest);

		for (int release : releases) {

			Jar delta = jpms.getReleaseOnly(release);

			copy.addDelta(delta);
			Manifest m = copy.calcManifest();

			Manifest newer = relevant(m);

			if (!newer.equals(older)) {
				jpms.putResource(release, MANIFEST_PATH, new ManifestResource(newer));
				older = newer;
			}

			Resource resource = delta.getResource(MODULE_INFO_CLASS);
			if (resource == null) {
				resource = baseline.getResource(MODULE_INFO_CLASS);
			}
			if (resource != null) {
				jpms.putResource(release, MODULE_INFO_CLASS, resource);
			}
		}
	}

	private Manifest relevant(Manifest m) {
		return JPMSModule.copy(null, m, "Manifest-Version", Constants.IMPORT_PACKAGE, Constants.REQUIRE_CAPABILITY);
	}
}
