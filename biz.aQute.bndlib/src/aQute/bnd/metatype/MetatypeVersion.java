package aQute.bnd.metatype;

import aQute.bnd.version.Version;

public enum MetatypeVersion {

	VERSION_1_2(new Version("1.2.0")),
	VERSION_1_3(new Version("1.3.0")),
	VERSION_1_4(new Version("1.4.0"));

	private final static String	NAMESPACE_STEM	= "http://www.osgi.org/xmlns/metatype/v";
	private final Version		value;

	MetatypeVersion(Version value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	public String getNamespace() {
		return NAMESPACE_STEM + value.toString();
	}

	static MetatypeVersion valueFor(String s) {
		Version v = new Version(s);
		for (MetatypeVersion mv : values()) {
			if (mv.value.equals(v)) {
				return mv;
			}
		}
		throw new IllegalArgumentException("No MetatypeVersion for " + v);
	}

}
