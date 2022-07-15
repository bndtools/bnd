package aQute.bnd.gradle

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner

import aQute.bnd.version.MavenVersion

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
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_18)) {
			return "7.5"
		}
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
			return "7.3.2"
		}
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
			return "7.0"
		}
		return "6.7"
	}

	public static String formatTime(ZipEntry entry) {
		long entryTime = entry.getTime()
		long offsetTime = entryTime + DEFAULT_TIME_ZONE.getOffset(entryTime)
		ZonedDateTime timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(offsetTime), UTC_ZONE_ID)
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp)
	}
}
