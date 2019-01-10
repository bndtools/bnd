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
    if (MavenVersion.parseString(defaultversion).compareTo(MavenVersion.parseString(version)) > 0) {
      version = defaultversion
    }
    return GradleRunner.create()
            .withGradleVersion(version)
  }

  private static String gradleVersion() {
    if (JavaVersion.current().isJava11Compatible()) {
      return '5.0'
    }
    if (JavaVersion.current().isJava10Compatible()) {
      return '4.7'
    }
    if (JavaVersion.current().isJava9Compatible()) {
      return '4.2.1'
    }
    return '4.0'
  }
}
