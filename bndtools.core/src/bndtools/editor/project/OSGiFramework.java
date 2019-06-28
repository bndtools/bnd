package bndtools.editor.project;

import java.net.URL;

import aQute.bnd.version.Version;

class OSGiFramework {

	private final String	name;
	private final String	bsn;
	private final Version	version;
	private final URL		icon;

	public OSGiFramework(String name, String bsn, Version version, URL icon) throws IllegalArgumentException {
		if (bsn == null)
			throw new IllegalArgumentException("At least BSN must be specified");

		this.name = name;
		this.bsn = bsn;
		this.version = version;
		this.icon = icon;
	}

	public String getName() {
		return name;
	}

	public String getBsn() {
		return bsn;
	}

	public Version getVersion() {
		return version;
	}

	public URL getIcon() {
		return icon;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bsn == null) ? 0 : bsn.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OSGiFramework other = (OSGiFramework) obj;
		if (bsn == null) {
			if (other.bsn != null)
				return false;
		} else if (!bsn.equals(other.bsn))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(bsn);
		if (version != null) {
			b.append(";version='[")
				.append(version.toString())
				.append(',')
				.append(version)
				.append("]'");
		}
		return b.toString();
	}

}
