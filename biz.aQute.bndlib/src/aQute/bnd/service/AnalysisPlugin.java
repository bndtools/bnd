package aQute.bnd.service;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.PackageRef;

/**
 * A plugin that is called during the analysis phase to collect information
 * about analysis decisions. This allows implementations to track why certain
 * decisions were made, such as why a particular version range was chosen for an
 * import.
 * <p>
 * This plugin is called during the {@link Analyzer#analyze()} phase, before the
 * manifest is generated. It provides callbacks for various analysis events,
 * allowing implementations to build a detailed log of analysis decisions.
 */
public interface AnalysisPlugin extends OrderedPlugin {

	/**
	 * Called when the analyzer determines a version range for an import package.
	 * This provides insight into why a particular version range was chosen.
	 *
	 * @param analyzer the analyzer performing the analysis
	 * @param packageRef the package being analyzed
	 * @param version the version or version range determined
	 * @param reason a human-readable explanation for why this version was chosen
	 *            (e.g., "provider type", "consumer type", "explicit version
	 *            policy")
	 * @throws Exception if an error occurs during processing
	 */
	void reportImportVersion(Analyzer analyzer, PackageRef packageRef, String version, String reason)
		throws Exception;

	/**
	 * Called when the analyzer makes other analysis decisions that may be of
	 * interest.
	 *
	 * @param analyzer the analyzer performing the analysis
	 * @param category the category of the analysis decision (e.g., "uses",
	 *            "export", "capability")
	 * @param details detailed information about the decision
	 * @throws Exception if an error occurs during processing
	 */
	default void reportAnalysis(Analyzer analyzer, String category, String details) throws Exception {
		// Default implementation does nothing
	}
}
