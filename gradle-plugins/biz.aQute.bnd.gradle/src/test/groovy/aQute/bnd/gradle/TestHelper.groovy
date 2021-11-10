package aQute.bnd.gradle

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner

import aQute.bnd.version.MavenVersion

class TestHelper {

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
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
			return "7.3"
		}
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
			return "7.0"
		}
		return "6.7"
	}
}
