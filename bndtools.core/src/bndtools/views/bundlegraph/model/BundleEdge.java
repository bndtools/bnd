package bndtools.views.bundlegraph.model;

import java.util.Objects;

/**
 * Represents a directed dependency edge between two bundle nodes.
 * <p>
 * An edge {@code (from, to)} means bundle {@code from} depends on bundle
 * {@code to} (i.e., {@code from} imports at least one package that {@code to}
 * exports). The edge is considered <em>optional</em> when every
 * {@code Import-Package} entry that gave rise to this edge carries
 * {@code resolution:=optional}; if at least one contributing import is
 * mandatory, the edge is mandatory.
 *
 * @param from the importing bundle
 * @param to the exporting bundle (the dependency)
 * @param optional {@code true} when every import that created this edge is
 *            {@code resolution:=optional}
 * @param contributingPackage the first package name that gave rise to this edge
 *            (may be empty, never {@code null})
 */
public record BundleEdge(BundleNode from, BundleNode to, boolean optional, String contributingPackage) {

	public BundleEdge(BundleNode from, BundleNode to, boolean optional, String contributingPackage) {
		this.from = Objects.requireNonNull(from, "from");
		this.to = Objects.requireNonNull(to, "to");
		this.optional = optional;
		this.contributingPackage = contributingPackage != null ? contributingPackage : "";
	}

	/**
	 * Convenience constructor for callers that do not track a contributing
	 * package.
	 */
	public BundleEdge(BundleNode from, BundleNode to, boolean optional) {
		this(from, to, optional, "");
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, optional, to);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BundleEdge other = (BundleEdge) obj;
		return Objects.equals(from, other.from) && optional == other.optional && Objects.equals(to, other.to);
	}
}
