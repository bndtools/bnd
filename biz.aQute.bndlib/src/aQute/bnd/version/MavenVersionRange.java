package aQute.bnd.version;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;

/**
 * Provides a representation of a maven version range. The implementation is a
 * small wrapper around
 * {@link org.apache.maven.artifact.versioning.VersionRange}.
 */
public class MavenVersionRange {

	private org.apache.maven.artifact.versioning.VersionRange range;

	public MavenVersionRange(String range) {
		try {
			this.range = org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec(range);
		} catch (InvalidVersionSpecificationException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public boolean includes(MavenVersion mvr) {
		return range.containsVersion(mvr);
	}

	public static MavenVersionRange parseRange(String version) {
		try {
			return new MavenVersionRange(version);
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public boolean wasSingle() {
		if (range.getRestrictions()
			.size() != 1) {
			return false;
		}
		Restriction r = range.getRestrictions()
			.get(0);
		return r.getLowerBound() == null && r.getUpperBound() == null;
	}

	public static boolean isRange(String version) {
		if (version == null)
			return false;

		version = version.trim();
		return version.startsWith("[") || version.startsWith("(");
	}

	@Override
	public String toString() {
		return range.toString();
	}
}
