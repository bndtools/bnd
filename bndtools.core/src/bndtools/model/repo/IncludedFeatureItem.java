package bndtools.model.repo;

import java.util.ArrayList;
import java.util.List;

import aQute.p2.provider.Feature;

/**
 * Wrapper for Feature.Includes representing an included feature reference.
 * Displays id, version, optional flag, and platform filters.
 */
public class IncludedFeatureItem {

	private final FeatureFolderNode	parent;
	private final Feature.Includes	includes;

	public IncludedFeatureItem(FeatureFolderNode parent, Feature.Includes includes) {
		this.parent = parent;
		this.includes = includes;
	}

	public FeatureFolderNode getParent() {
		return parent;
	}

	public Feature.Includes getIncludes() {
		return includes;
	}

	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append(includes.id);

		if (includes.version != null && !includes.version.equals("0.0.0")) {
			sb.append(" ")
				.append(includes.version);
		}

		// Add platform filters if present
		List<String> filters = new ArrayList<>();
		if (includes.os != null)
			filters.add(includes.os);
		if (includes.ws != null)
			filters.add(includes.ws);
		if (includes.arch != null)
			filters.add(includes.arch);

		if (!filters.isEmpty()) {
			sb.append(" [")
				.append(String.join("/", filters))
				.append("]");
		}

		if (includes.optional) {
			sb.append(" (optional)");
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return "IncludedFeatureItem [" + includes.toString() + "]";
	}
}
