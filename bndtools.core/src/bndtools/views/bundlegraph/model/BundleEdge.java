package bndtools.views.bundlegraph.model;

/**
 * Represents a directed dependency edge between two bundle nodes.
 * <p>
 * An edge {@code (from, to)} means bundle {@code from} depends on bundle {@code to} (i.e., {@code from} imports at
 * least one package that {@code to} exports). The edge is considered <em>optional</em> when every
 * {@code Import-Package} entry that gave rise to this edge carries {@code resolution:=optional}; if at least one
 * contributing import is mandatory, the edge is mandatory.
 *
 * @param from the importing bundle
 * @param to the exporting bundle (the dependency)
 * @param optional {@code true} when every import that created this edge is {@code resolution:=optional}
 */
public record BundleEdge(BundleNode from, BundleNode to, boolean optional) {}
