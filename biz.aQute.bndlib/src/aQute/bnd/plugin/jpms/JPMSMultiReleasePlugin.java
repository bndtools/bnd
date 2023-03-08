package aQute.bnd.plugin.jpms;

import java.util.SortedSet;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.JPMSModule;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.ManifestResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.ManifestPlugin;

/**
 * Handles multi release jars.
 */
public class JPMSMultiReleasePlugin implements ManifestPlugin {

	/**
	 * We need to calculate alternative files for versioned JARS
	 */
	@Override
	public void mainSet(Analyzer analyzer, Manifest manifest) throws Exception {
		if (!analyzer.is(Constants.JPMS_MULTI_RELEASE))
			return;

		Jar target = analyzer.getJar();
		JPMSModule jpms = new JPMSModule(target);
		SortedSet<Integer> releases = jpms.getVersions();

		if (releases.isEmpty())
			return;

		boolean generateModuleInfo = analyzer.getProperty(Constants.JPMS_MODULE_INFO) != null;

		Domain domain = Domain.domain(manifest);
		domain.setMultiRelease(true);

		for (int release : releases) {
			Jar releaseContent = jpms.getRelease(release);

			try (Analyzer releaseAnalyzer = new Analyzer(analyzer)) {
				releaseAnalyzer.setJar(releaseContent);
				releaseAnalyzer.removeClose(releaseContent);

				for (Jar cpEntry : analyzer.getClasspath()) {
					releaseAnalyzer.addClasspath(cpEntry);
					releaseAnalyzer.removeClose(cpEntry);
				}

				if (generateModuleInfo)
					releaseAnalyzer.addBasicPlugin(new JPMSModuleInfoPlugin());

				Manifest releaseManifest = releaseAnalyzer.calcManifest();

				Manifest newer = relevant(releaseManifest);
				jpms.putResource(release, JPMSModule.OSGI_VERSIONED_MANIFEST_PATH, new ManifestResource(newer));

				if (generateModuleInfo && jpms.getResource(release, Constants.MODULE_INFO_CLASS) == null) {
					Resource resource = releaseAnalyzer.getJar()
						.getResource(Constants.MODULE_INFO_CLASS);
					if (resource != null) {
						jpms.putResource(release, Constants.MODULE_INFO_CLASS, resource);
					}
				}
			}
		}
	}

	private Manifest relevant(Manifest m) {
		return JPMSModule.copy(null, m, "Manifest-Version", Constants.IMPORT_PACKAGE, Constants.REQUIRE_CAPABILITY);
	}
}