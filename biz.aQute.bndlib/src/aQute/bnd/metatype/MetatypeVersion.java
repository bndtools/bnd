package aQute.bnd.metatype;

public enum MetatypeVersion {
	
	VERSION_1_2("1.2.0"),
	VERSION_1_3("1.3.0");
	
	private final static String NAMESPACE_STEM = "http://www.osgi.org/xmlns/metatype/v";
	private final String	value;

	MetatypeVersion(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
	
	public String getNamespace() {
		return NAMESPACE_STEM + value;
	}

}
