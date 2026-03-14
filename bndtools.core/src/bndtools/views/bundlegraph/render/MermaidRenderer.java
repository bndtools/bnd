package bndtools.views.bundlegraph.render;

import java.util.Set;

import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleGraphModel;
import bndtools.views.bundlegraph.model.BundleNode;

/**
 * Renders a subset of a {@link BundleGraphModel} as a Mermaid graph definition.
 */
public final class MermaidRenderer {

	private MermaidRenderer() {}



	/**
	 * Produces a Mermaid {@code graph LR} definition for the given node subset,
	 * visually distinguishing "primary" (user-selected seed) nodes from
	 * "secondary" (closure) nodes, and filtering edges according to
	 * {@code edgeFilter}.
	 * <p>
	 * Primary nodes get a solid, highlighted border; secondary nodes get a
	 * dashed, muted border. Mandatory dependency edges are rendered as solid
	 * arrows ({@code -->}); optional-only edges are rendered as dotted arrows
	 * ({@code -.->}). The {@code edgeFilter} controls which edges are included:
	 * <ul>
	 * <li>{@link EdgeFilter#ALL} – all edges (mandatory and optional)</li>
	 * <li>{@link EdgeFilter#ONLY_MANDATORY} – only edges that are not
	 * all-optional</li>
	 * <li>{@link EdgeFilter#ONLY_OPTIONAL} – only edges that are
	 * all-optional</li>
	 * </ul>
	 * Only edges where both endpoints are in the subset are included.
	 *
	 * @param model the full graph model
	 * @param subset all nodes to include in the diagram (primary ∪ secondary)
	 * @param primaryNodes the user-selected seed nodes (subset of
	 *            {@code subset})
	 * @param edgeFilter controls which dependency edges to include
	 * @param showFirstPackage if <code>true</code> the first contributing
	 *            package on an edge is shown
	 * @return Mermaid graph definition string
	 */
	public static String toMermaid(BundleGraphModel model, Set<BundleNode> subset, Set<BundleNode> primaryNodes,
		EdgeFilter edgeFilter, boolean showFirstPackage) {

		// Collect active edges (filtered by edgeFilter, both endpoints in subset)
		java.util.List<BundleEdge> activeEdges = new java.util.ArrayList<>();
		for (BundleEdge edge : model.edges()) {
			if (edgeFilter == EdgeFilter.ONLY_MANDATORY && edge.optional()) {
				continue;
			}
			if (edgeFilter == EdgeFilter.ONLY_OPTIONAL && !edge.optional()) {
				continue;
			}
			if (!subset.contains(edge.from()) || !subset.contains(edge.to())) {
				continue;
			}
			// When a non-trivial primary set is given and the filter is non-ALL,
			// restrict to edges that touch at least one primary node so that
			// secondary-to-secondary edges (e.g. transitive dependencies between
			// closure nodes) are not shown.
			if (edgeFilter != EdgeFilter.ALL && !primaryNodes.isEmpty()
				&& !primaryNodes.contains(edge.from()) && !primaryNodes.contains(edge.to())) {
				continue;
			}
			activeEdges.add(edge);
		}

		// When filtering, only show nodes that participate in at least one active edge
		Set<BundleNode> visibleNodes;
		if (edgeFilter == EdgeFilter.ALL) {
			visibleNodes = subset;
		} else {
			visibleNodes = new java.util.LinkedHashSet<>();
			for (BundleEdge edge : activeEdges) {
				visibleNodes.add(edge.from());
				visibleNodes.add(edge.to());
			}
		}

		boolean hasSecondary = visibleNodes.stream()
			.anyMatch(n -> !primaryNodes.contains(n));
		boolean hasPrimary = !primaryNodes.isEmpty();

		StringBuilder sb = new StringBuilder("graph LR\n");

		// Emit classDef declarations when there is a visual distinction to make
		if (hasPrimary && hasSecondary) {
			sb.append("    classDef primary fill:#dae8fc,stroke:#1a6496,stroke-width:2px\n");
			sb.append("    classDef secondary fill:#f5f5f5,stroke:#888888,stroke-width:1px,stroke-dasharray:4 4\n");
		}

		for (BundleNode node : visibleNodes) {
			sb.append("    ")
				.append(nodeId(node))
				.append("[\"")
				.append(escape(node.toString()))
				.append("\"]");
			if (hasPrimary && hasSecondary) {
				sb.append(":::").append(primaryNodes.contains(node) ? "primary" : "secondary");
			}
			sb.append("\n");
		}

		for (BundleEdge edge : activeEdges) {
			// Arrow direction: 'to' exports something that 'from' imports →
			// arrow points to --> from
			String arrow = edge.optional() ? ".->" : "-->";
			String firstPkg = edge.contributingPackage();
			sb.append("    ")
				.append(nodeId(edge.to()))
				.append(" ")
				.append(arrow);
			if (showFirstPackage && firstPkg != null && !firstPkg.isEmpty()) {
				sb.append("|")
					.append(escape(firstPkg))
					.append("|");
			}
			sb.append(" ")
				.append(nodeId(edge.from()))
				.append("\n");
		}
		return sb.toString();
	}

	private static String nodeId(BundleNode node) {
		// Replace non-alphanumeric characters to produce a valid Mermaid node id
		return node.getBsn()
			.replace('.', '_')
			.replace('-', '_');
	}

	private static String escape(String text) {
		return text.replace("\"", "&quot;");
	}
}
