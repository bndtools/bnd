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
    if (JavaVersion.current().compareTo(JavaVersion.VERSION_12) > 0) {
      return '6.0'
    }
    return '5.1'
  }
}
