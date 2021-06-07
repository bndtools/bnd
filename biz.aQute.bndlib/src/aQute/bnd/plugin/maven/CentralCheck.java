package aQute.bnd.plugin.maven;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.verifier.VerifierPlugin;

/**
 * Sets a warning when a header is missing to publish at Central. The Sonatype
 * Nexus does some verifications and it is pretty annoying if they fail.
 *
 * @see <a href=
 *      "https://central.sonatype.org/pages/requirements.html#sufficient-metadata">Sufficient
 *      Metadata</a>
 */

@BndPlugin(name = "CentralCheck")
public class CentralCheck implements VerifierPlugin {

	@Override
	public void verify(Analyzer analyzer) throws Exception {
		String pom = analyzer.getProperty(Constants.POM);
		if (!Processor.isTrue(pom))
			return;
		Attrs pomProperties = OSGiHeader.parseProperties(pom);
		String groupId = pomProperties.get("groupid");
		if (groupId == null) {
			check(analyzer, Constants.GROUPID);
		}
		String version = pomProperties.get("version");
		if (version == null) {
			check(analyzer, Constants.BUNDLE_VERSION);
		}

		// name defaults to groupId:artifactId

		// description defaults to name

		check(analyzer, Constants.BUNDLE_DOCURL);

		check(analyzer, Constants.BUNDLE_LICENSE);

		check(analyzer, Constants.BUNDLE_DEVELOPERS);

		check(analyzer, Constants.BUNDLE_SCM);
	}

	private void check(Analyzer analyzer, String key) throws Exception {
		String value = analyzer.getProperty(key);
		if (value == null) {
			analyzer.warning("Maven Central Check: %s not set", key);
		}
	}

}
