package org.osgi.service.indexer.impl;

import java.util.List;

import org.osgi.framework.Version;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;

/**
 * Detects JARs that are OSGi Frameworks, using the presence of META-INF/services/org.osgi.framework.launch.FrameworkFactory
 */
public class OSGiFrameworkAnalyzer implements ResourceAnalyzer {

	public void analyzeResource(Resource resource, List<Capability> caps, List<Requirement> reqs) throws Exception {
		Resource fwkFactorySvc = resource.getChild("META-INF/services/org.osgi.framework.launch.FrameworkFactory");
		if (fwkFactorySvc != null) {
			Builder builder = new Builder().setNamespace("osgi.framework");

			String identity = null;
			Version version = null;
			Version specVersion = null;
			StringBuilder uses = new StringBuilder();
			boolean firstPkg = true;
			
			for (Capability cap : caps) {
				if (Namespaces.NS_IDENTITY.equals(cap.getNamespace())) {
					identity = (String) cap.getAttributes().get(Namespaces.NS_IDENTITY);
					version = (Version) cap.getAttributes().get(Namespaces.ATTR_VERSION);
				} else if (Namespaces.NS_WIRING_PACKAGE.equals(cap.getNamespace())) {
					if (!firstPkg)
						uses.append(',');
					String pkgName = (String) cap.getAttributes().get(Namespaces.NS_WIRING_PACKAGE);
					uses.append(pkgName);
					firstPkg = false;
					
					if ("org.osgi.framework".equals(pkgName)) {
						Version frameworkPkgVersion = (Version) cap.getAttributes().get(Namespaces.ATTR_VERSION);
						specVersion = mapFrameworkPackageVersion(frameworkPkgVersion);
					}
				}
			}
			
			if (identity != null)
				builder.addAttribute("osgi.framework", identity);
			if (version != null)
				builder.addAttribute(Namespaces.ATTR_VERSION, version);
			if (specVersion != null)
				builder.addAttribute("specification-version", specVersion);
			
			builder.addDirective(Namespaces.DIRECTIVE_USES, uses.toString());
			caps.add(builder.buildCapability());
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
