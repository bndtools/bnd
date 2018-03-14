package aQute.bnd.osgi;

import java.util.Objects;

/**
 * Holds the bundle bsn + version pair
 */
public class BundleId implements Comparable<BundleId> {
	final String	bsn;
	final String	version;

	public BundleId(String bsn, String version) {
		this.bsn = bsn.trim();
		this.version = version.trim();
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
}
