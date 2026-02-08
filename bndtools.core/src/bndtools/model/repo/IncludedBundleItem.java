package bndtools.model.repo;

import java.util.ArrayList;
import java.util.List;

import aQute.p2.provider.Feature;

/**
 * Wrapper for Feature.Plugin representing an included bundle reference.
 * Displays id, version, platform filters (os/ws/arch), fragment flag, and
 * unpack flag.
 */
public class IncludedBundleItem {

	private final FeatureFolderNode	parent;
	private final Feature.Plugin	plugin;

	public IncludedBundleItem(FeatureFolderNode parent, Feature.Plugin plugin) {
		this.parent = parent;
		this.plugin = plugin;
	}

	public FeatureFolderNode getParent() {
		return parent;
	}

	public Feature.Plugin getPlugin() {
		return plugin;
	}

	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append(plugin.id);

		if (plugin.version != null && !plugin.version.equals("0.0.0")) {
			sb.append(" ")
				.append(plugin.version);
		}

		// Add platform filters if present
		List<String> filters = new ArrayList<>();
		if (plugin.os != null)
			filters.add(plugin.os);
		if (plugin.ws != null)
			filters.add(plugin.ws);
		if (plugin.arch != null)
			filters.add(plugin.arch);

		if (!filters.isEmpty()) {
			sb.append(" [")
				.append(String.join("/", filters))
				.append("]");
		}

		// Add flags
		if (plugin.fragment) {
			sb.append(" (fragment)");
		}
		if (plugin.unpack) {
			sb.append(" (unpack)");
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return "IncludedBundleItem [" + plugin.toString() + "]";
	}
}
