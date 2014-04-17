package aQute.bnd.indexer.analyzers;

import org.osgi.resource.*;

import aQute.bnd.indexer.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.version.*;


/**
 * Detects JARs that are OSGi Frameworks, using the presence of
 * META-INF/services/org.osgi.framework.launch.FrameworkFactory
 */
public class OSGiFrameworkAnalyzer implements ResourceAnalyzer {

	private static final String SERVICE_FRAMEWORK_FACTORY = "META-INF/services/org.osgi.framework.launch.FrameworkFactory";
	private static final String FRAMEWORK_PACKAGE = "org.osgi.framework";

	public OSGiFrameworkAnalyzer() {
	}

	public void analyzeResource(Jar resource, ResourceBuilder rb) throws Exception {
		Resource fwkFactorySvc = resource.getResource(SERVICE_FRAMEWORK_FACTORY);
		if (fwkFactorySvc != null) {
			CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_CONTRACT).addAttribute(Namespaces.NS_CONTRACT, Namespaces.CONTRACT_OSGI_FRAMEWORK);

			Version specVersion = null;
			StringBuilder uses = new StringBuilder();
			boolean firstPkg = true;

			for (Capability cap : rb.getCapabilities()) {
				if (Namespaces.NS_WIRING_PACKAGE.equals(cap.getNamespace())) {
					// Add to the uses directive
					if (!firstPkg)
						uses.append(',');
					String pkgName = (String) cap.getAttributes().get(Namespaces.NS_WIRING_PACKAGE);
					uses.append(pkgName);
					firstPkg = false;

					// If it's org.osgi.framework, get the package version and
					// map to OSGi spec version
					if (FRAMEWORK_PACKAGE.equals(pkgName)) {
						Version frameworkPkgVersion = (Version) cap.getAttributes().get(Namespaces.ATTR_VERSION);
						specVersion = mapFrameworkPackageVersion(frameworkPkgVersion);
					}
				}
			}

			if (specVersion != null)
				builder.addAttribute(Namespaces.ATTR_VERSION, specVersion);

			builder.addDirective(Namespaces.DIRECTIVE_USES, uses.toString());
			rb.addCapability(builder);
		}
	}

	/**
	 * Map the version of package {@code org.osgi.framework} to an OSGi
	 * specification release version
	 * 
	 * @param pv
	 *            Version of the {@code org.osgi.framework} packge
	 * @return The OSGi specification release version, or {@code null} if not
	 *         known.
	 */
	private Version mapFrameworkPackageVersion(Version pv) {
		if (pv.getMajor() != 1)
			return null;

		Version version;
		switch (pv.getMinor()) {
		case 7:
			version = new Version(5, 0, 0);
			break;
		case 6:
			version = new Version(4, 3, 0);
			break;
		case 5:
			version = new Version(4, 2, 0);
			break;
		case 4:
			version = new Version(4, 1, 0);
			break;
		case 3:
			version = new Version(4, 0, 0);
			break;
		case 2:
			version = new Version(3, 0, 0);
			break;
		case 1:
			version = new Version(2, 0, 0);
			break;
		case 0:
			version = new Version(1, 0, 0);
			break;
		default:
			version = null;
			break;
		}

		return version;
	}

}
