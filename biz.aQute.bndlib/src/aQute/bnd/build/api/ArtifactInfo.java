package aQute.bnd.build.api;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Packages;

/**
 * A snapshot of the build information of an artifact.
 */
@ProviderType
public interface ArtifactInfo {
	/**
	 * The bundle ID of the build bundle
	 *
	 * @return the bundle id
	 */
	BundleId getBundleId();

	/**
	 * Exported packages
	 *
	 * @return exported packages.
	 */
	Packages getExports();

	/**
	 * Imported packages
	 */
	Packages getImports();

	/**
	 * Contained packages
	 */
	Packages getContained();

}
