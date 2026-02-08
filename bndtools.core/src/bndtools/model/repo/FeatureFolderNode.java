package bndtools.model.repo;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;

import aQute.p2.provider.Feature;

/**
 * Virtual folder node for grouping feature children (included features,
 * required features, included bundles). This provides a hierarchical structure
 * under each RepositoryFeature.
 */
public class FeatureFolderNode {

	private static final ILogger logger = Logger.getLogger(FeatureFolderNode.class);

	public enum FolderType {
		INCLUDED_FEATURES("Included Features"),
		REQUIRED_FEATURES("Required Features"),
		INCLUDED_BUNDLES("Included Bundles");

		private final String label;

		FolderType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	private final RepositoryFeature	parent;
	private final FolderType		type;
	private final List<Object>		children;
	private String					version;

	public FeatureFolderNode(RepositoryFeature parent, FolderType type) {
		this.parent = parent;
		this.type = type;
		this.children = new ArrayList<>();

		// Populate children based on type
		Feature feature = parent.getFeature();
		this.version = feature.version;
		try {
			// Ensure feature is parsed before accessing children
			feature.parse();

			switch (type) {
				case INCLUDED_FEATURES :
					for (Feature.Includes include : feature.getIncludes()) {
						children.add(new IncludedFeatureItem(this, include));
					}
					break;
				case REQUIRED_FEATURES :
					for (Feature.Requires requires : feature.getRequires()) {
						if (requires.feature != null) {
							children.add(new RequiredFeatureItem(this, requires));
						}
					}
					break;
				case INCLUDED_BUNDLES :
					for (Feature.Plugin plugin : feature.getPlugins()) {
						children.add(new IncludedBundleItem(this, plugin));
					}
					break;
			}
		} catch (Exception e) {
			// Log parsing errors for debugging
			logger.logError("Failed to parse feature " + feature.getId() + " for folder type " + type, e);
		}
	}

	public RepositoryFeature getParent() {
		return parent;
	}

	public FolderType getType() {
		return type;
	}

	public String getLabel() {
		return type.getLabel();
	}

	public List<Object> getChildren() {
		return children;
	}

	public boolean hasChildren() {
		return !children.isEmpty();
	}

	@Override
	public String toString() {
		return "FeatureFolderNode [type=" + type + ", children=" + children.size() + "]";
	}
}
