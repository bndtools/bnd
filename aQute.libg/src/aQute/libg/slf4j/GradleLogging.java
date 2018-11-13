package aQute.libg.slf4j;

import org.slf4j.Marker;

/**
 * SLF4J Markers for special Gradle log levels. These markers are to be used
 * with the info logging methods.
 */
public class GradleLogging {
	/**
	 * The Gradle LIFECYCLE marker.
	 */
	public final static Marker	LIFECYCLE;
	/**
	 * The Gradle QUIET marker.
	 */
	public final static Marker	QUIET;

	static {
		String gradleLogging = "org.gradle.api.logging.Logging";
		Marker lifecycle = null;
		Marker quiet = null;
		try {
			Class<?> logging = Class.forName(gradleLogging);
			lifecycle = (Marker) logging.getField("LIFECYCLE")
				.get(null);
			quiet = (Marker) logging.getField("QUIET")
				.get(null);
		} catch (Exception e) {}
		LIFECYCLE = lifecycle;
		QUIET = quiet;
	}

	private GradleLogging() {}
}
