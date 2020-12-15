package aQute.bnd.gradle

import aQute.bnd.version.MavenVersion

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner

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
    if (System.getProperty('org.gradle.warning.mode') == 'fail') { // if 'fail' we use the build gradle version
      return runner
    }
    return runner.withGradleVersion(version)
  }

  private static String gradleVersion() {
    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_15)) {
      return '6.7'
    }
    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_14)) {
      return '6.3'
    }
    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_13)) {
      return '6.0'
    }
    return '5.3'
  }
}
