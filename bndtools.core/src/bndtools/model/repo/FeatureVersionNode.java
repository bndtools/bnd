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