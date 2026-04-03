package bndtools.views.bundlegraph.model;

import java.util.Objects;

/**
 * Represents a bundle (OSGi project) node in the Bundle Graph.
 * <p>
 * Identity is based solely on {@code bsn} and {@code version}; {@code projectName} is informational metadata and is
 * intentionally excluded from {@link #equals(Object)} and {@link #hashCode()}. This ensures that a node loaded from a
 * repository and the same bundle loaded from a workspace project are treated as the same node so that edges can be
 * shared across providers.
 */
public record BundleNode(String bsn, String version, String projectName) {

	public BundleNode(String bsn, String version, String projectName) {
		this.bsn = Objects.requireNonNull(bsn, "bsn");
		this.version = version != null ? version : "";
		this.projectName = projectName != null ? projectName : "";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof BundleNode other))
			return false;
		return Objects.equals(bsn, other.bsn) && Objects.equals(version, other.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bsn, version);
	}

	public String getBsn() {
		return bsn;
	}

	public String getVersion() {
		return version;
	}

	public String getProjectName() {
		return projectName;
	}

	@Override
	public String toString() {
		if (version.isEmpty()) {
			return bsn;
		}
		return bsn + " " + version;
	}
}
