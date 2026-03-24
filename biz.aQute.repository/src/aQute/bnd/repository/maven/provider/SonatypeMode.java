package aQute.bnd.repository.maven.provider;

@Deprecated(forRemoval = true, since = "7.3.0")
public enum SonatypeMode {

	/*
	 * No special Sonatype Portal handling
	 */
	NONE("none"),

	/*
	 * Manual mode where artifacts are staged but not published
	 */
	MANUAL("manual"),

	/*
	 * Automatic publishing after upload and validatíon
	 */
	AUTOPUBLISH("autopublish");

	private final String value;

	SonatypeMode(String value) {
		this.value = value;
	}

	@Deprecated
	@Override
	public String toString() {
		return value;
	}
}
