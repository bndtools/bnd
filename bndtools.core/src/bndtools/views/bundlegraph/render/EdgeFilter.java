package bndtools.views.bundlegraph.render;

/**
 * Controls which dependency edges are rendered in the Mermaid graph.
 */
public enum EdgeFilter {

	/** Show all edges – both mandatory and optional-only. */
	ALL("All (mandatory + optional)"),

	/** Show only mandatory edges (edges where at least one contributing import has no {@code resolution:=optional}). */
	ONLY_MANDATORY("Only mandatory"),

	/** Show only optional-only edges (edges where every contributing import carries {@code resolution:=optional}). */
	ONLY_OPTIONAL("Only optional");

	private final String label;

	EdgeFilter(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
