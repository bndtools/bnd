package aQute.bnd.gradle

import aQute.bnd.version.MavenVersion
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry

class TestHelper {
	private final static TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();
	private final static ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

	private TestHelper() { }

	public static GradleRunner getGradleRunner() {
		return runner(gradleVersion())
	}

	public static GradleRunner getGradleRunner(String version) {
		String defaultversion = gradleVersion()
		if (MavenVersion.parseMavenString(defaultversion).compareTo(MavenVersion.parseMavenString(version)) > 0) {
			return runner(defaultversion)
		}
		return runner(version)
	}

	private static GradleRunner runner(String version) {
		GradleRunner runner = GradleRunner.create()
		if (System.getProperty("org.gradle.warning.mode") == "fail") {
			// if "fail" we use the build gradle version
			return runner
		}
		return runner.withGradleVersion(version)
	}

	private static String gradleVersion() {
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21) || 
			JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_22) || 
			JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_23)) {
			return "8.10"
		}
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_20)) {
			return "8.1.1"
		}
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_19)) {
			return "7.6"
		}
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_18)) {
			return "7.5"
		}
		return "7.3.2"
	}

	public static String formatTime(ZipEntry entry) {
		long entryTime = entry.getTime()
		long offsetTime = entryTime + DEFAULT_TIME_ZONE.getOffset(entryTime)
		ZonedDateTime timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(offsetTime), UTC_ZONE_ID)
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp)
	}
}
