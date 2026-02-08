package bndtools.model.repo;

import aQute.p2.provider.Feature;

/**
 * Wrapper for Feature.Requires representing a required feature reference.
 * Displays id, version, match rule, and optional flag.
 */
public class RequiredFeatureItem {

	private final FeatureFolderNode	parent;
	private final Feature.Requires	requires;

	public RequiredFeatureItem(FeatureFolderNode parent, Feature.Requires requires) {
		this.parent = parent;
		this.requires = requires;
	}

	public FeatureFolderNode getParent() {
		return parent;
	}

	public Feature.Requires getRequires() {
		return requires;
	}

	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append(requires.feature);

		if (requires.version != null && !requires.version.equals("0.0.0")) {
			sb.append(" ")
				.append(requires.version);
		}

		// Add match rule if present
		if (requires.match != null && !requires.match.isEmpty()) {
			sb.append(" [")
				.append(requires.match)
				.append("]");
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return "RequiredFeatureItem [" + requires.toString() + "]";
	}
}
