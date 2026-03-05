package bndtools.model.repo;

import java.util.ArrayList;
import java.util.Comparator;
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
		REQUIRED_FEATURES("Required Features & Bundles"),
		INCLUDED_BUNDLES("Included Bundles");

		private final String label;

		FolderType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	private final FeatureVersionNode	parent;
	private final FolderType		type;
	private final List<Object>		children;

	public FeatureFolderNode(FeatureVersionNode parent, FolderType type) {
		this.parent = parent;
		this.type = type;
		this.children = new ArrayList<>();

		// Populate children based on type
		Feature feature = parent.getFeature();
		try {
			// Ensure feature is parsed before accessing children
			feature.parse();

			switch (type) {
				case INCLUDED_FEATURES :
					for (Feature.Includes include : feature.getIncludes()) {
						children.add(new IncludedFeatureItem(this, include));
					}
					children.sort(Comparator
						.comparing((Object o) -> ((IncludedFeatureItem) o).getIncludes().id,
							Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
						.thenComparing(o -> ((IncludedFeatureItem) o).getIncludes().version,
							Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
					break;
				case REQUIRED_FEATURES :
					for (Feature.Requires requires : feature.getRequires()) {
						// Include both feature and plugin requirements
						children.add(new RequiredFeatureItem(this, requires));
					}
					children.sort(Comparator
						.comparing((Object o) -> {
							Feature.Requires requires = ((RequiredFeatureItem) o).getRequires();
							return requires.feature != null ? requires.feature : requires.plugin;
						}, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
						.thenComparing(o -> ((RequiredFeatureItem) o).getRequires().version,
							Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
					break;
				case INCLUDED_BUNDLES :
					for (Feature.Plugin plugin : feature.getPlugins()) {
						children.add(new IncludedBundleItem(this, plugin));
					}
					children.sort(Comparator
						.comparing((Object o) -> ((IncludedBundleItem) o).getPlugin().id,
							Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
						.thenComparing(o -> ((IncludedBundleItem) o).getPlugin().version,
							Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
					break;
			}
		} catch (Exception e) {
			// Log parsing errors for debugging
			logger.logError("Failed to parse feature " + feature.getId() + " for folder type " + type, e);
		}
	}

	public FeatureVersionNode getParent() {
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
