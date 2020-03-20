package aQute.bnd.osgi;

import java.util.Objects;

import aQute.bnd.version.Version;

/**
 * Holds the bundle bsn + version pair
 */
public class BundleId implements Comparable<BundleId> {
	final String	bsn;
	final String	version;

	public BundleId(String bsn, String version) {
		this.bsn = bsn.trim();
		this.version = version == null ? "0" : version.trim();
	}

	public BundleId(String bsn, Version version) {
		this.bsn = bsn;
		this.version = version.toString();
	}

	public String getVersion() {
		return version;
	}

	public String getBsn() {
		return bsn;
	}

	public boolean isValid() {
		return Verifier.isVersion(version) && Verifier.isBsn(bsn);
	}

	@Override
	public boolean equals(Object o) {
		return this == o || ((o instanceof BundleId) && compareTo((BundleId) o) == 0);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bsn, version);
	}

	@Override
	public int compareTo(BundleId other) {
		int result = bsn.compareTo(other.bsn);
		if (result != 0)
			return result;

		return version.compareTo(other.version);
	}

	@Override
	public String toString() {
		return bsn + ";version=" + version;
	}

	public String getShortVersion() {
		try {
			Version v = new Version(version);
			return v.toStringWithoutQualifier();
		} catch (Exception e) {
			return version;
		}
	}
}
