package aQute.lib.bundles;

import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.dto.BundleDTO;

public class BundleIdentity {
	final String	bsn;
	final Version	version;

	public BundleIdentity(String bsn, Version version) {
		Objects.requireNonNull(bsn, "bsn must be specified");
		this.bsn = bsn;
		this.version = version == null ? Version.emptyVersion : version;
	}

	public BundleIdentity(Bundle bundle) {
		this(bundle.getSymbolicName(), bundle.getVersion());
	}

	public BundleIdentity(BundleDTO bundle) {
		this(bundle.symbolicName, bundle.version);
	}

	public BundleIdentity(String bsn, String version) {
		this(bsn, version == null ? null : Version.parseVersion(version));
	}

	public BundleIdentity(Map.Entry<String, Version> entry) {
		this(entry.getKey(), entry.getValue());
	}

	public String getBundleSymbolicName() {
		return bsn;
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bsn, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BundleIdentity other = (BundleIdentity) obj;
		return Objects.equals(bsn, other.bsn) && Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return bsn + "-" + version;
	}
}
