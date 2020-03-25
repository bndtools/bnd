package aQute.bnd.gradle

import aQute.bnd.version.MavenVersion

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner

class TestHelper {

  private TestHelper() { }

  public static GradleRunner getGradleRunner() {
    return GradleRunner.create()
            .withGradleVersion(gradleVersion())
  }

  public static GradleRunner getGradleRunner(String version) {
    String defaultversion = gradleVersion()
    if (MavenVersion.parseMavenString(defaultversion).compareTo(MavenVersion.parseMavenString(version)) > 0) {
      version = defaultversion
    }
    return GradleRunner.create()
            .withGradleVersion(version)
  }

  private static String gradleVersion() {
    JavaVersion current = JavaVersion.current()
    JavaVersion[] versions = JavaVersion.values()
    if ((versions.length > 14) && (current.compareTo(versions[14 - 1]) >= 0)) {
      return '6.3'
    }
    if ((versions.length > 13) && (current.compareTo(versions[13 - 1]) >= 0)) {
      return '6.0'
    }
    return '5.1'
  }
}
