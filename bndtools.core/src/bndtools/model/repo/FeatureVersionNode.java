package bndtools.model.repo;

import aQute.p2.provider.Feature;
import aQute.bnd.service.RepositoryPlugin;

/**
 * Virtual node representing a specific Eclipse feature version. Folder nodes
 * (included features, required features/bundles, included bundles) are shown
 * as children of this node.
 */
public class FeatureVersionNode {

	private final RepositoryFeature	parent;
	private boolean					parsed	= false;

	public FeatureVersionNode(RepositoryFeature parent) {
		this.parent = parent;
	}

	public RepositoryFeature getParent() {
		return parent;
	}

	public RepositoryPlugin getRepo() {
		return parent.getRepo();
	}

	public Feature getFeature() {
		return parent.getFeature();
	}

	/**
	 * Ensures the feature is parsed exactly once, caching the parsed state so
	 * that subsequent calls are no-ops. This avoids repeated parsing when
	 * multiple {@link FeatureFolderNode}s are created for the same feature.
	 *
	 * @throws Exception if parsing fails
	 */
	public synchronized void ensureParsed() throws Exception {
		if (!parsed) {
			getFeature().parse();
			parsed = true;
		}
	}

	public String getVersion() {
		Feature feature = getFeature();
		return feature.getVersion();
	}

	public String getText() {
		String version = getVersion();
		return (version == null || version.isBlank()) ? "(no version)" : version;
	}

	@Override
	public String toString() {
		return "FeatureVersionNode [id=" + getFeature().getId() + ", version=" + getVersion() + "]";
	}
}