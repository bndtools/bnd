package aQute.bnd.service;

import java.util.List;

/**
 * An interface for repository plugins that can provide Eclipse P2 features.
 * This allows repositories to expose features alongside bundles. The actual
 * feature objects are implementation-specific (e.g., aQute.p2.provider.Feature
 * for P2 repositories).
 */
public interface FeatureProvider {

	/**
	 * Get all features available in this repository.
	 *
	 * @return a list of features, or empty list if none available
	 * @throws Exception if an error occurs while fetching features
	 */
	List<?> getFeatures() throws Exception;

	/**
	 * Get a specific feature by ID and version.
	 *
	 * @param id the feature ID
	 * @param version the feature version
	 * @return the feature, or null if not found
	 * @throws Exception if an error occurs while fetching the feature
	 */
	Object getFeature(String id, String version) throws Exception;
}
