package aQute.bnd.plugin.maven;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.AnalyzerPlugin;

/**
 * Sets a warning when a header is missing to publish at Central. The Sonatype
 * Nexus does some verifications and it is pretty annoying if they fail.
 */

@BndPlugin(name = "Central")
public class CentralCheck implements AnalyzerPlugin {

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		if (analyzer.getProperty(Constants.POM) == null)
			return false;

		check(analyzer, Constants.GROUPID);
		check(analyzer, Constants.BUNDLE_VERSION);
		check(analyzer, Constants.BUNDLE_LICENSE);
		check(analyzer, Constants.BUNDLE_SCM);
		check(analyzer, Constants.BUNDLE_DEVELOPERS);
		check(analyzer, Constants.BUNDLE_DOCURL);
		return false;
	}

	private void check(Analyzer analyzer, String key) throws Exception {
		String value = analyzer.getProperty(key);
		if (value == null) {
			analyzer.warning("Maven Central: %s not set", key);
		}
	}

}
