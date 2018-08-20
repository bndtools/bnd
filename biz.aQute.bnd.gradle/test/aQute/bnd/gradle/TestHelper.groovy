package aQute.bnd.gradle

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner

class TestHelper {

  private TestHelper() { }

  public static GradleRunner getGradleRunner() {
    return GradleRunner.create()
            .withGradleVersion(gradleVersion())
  }

  public static GradleRunner getGradleRunner(String gradleVersion) {
    return GradleRunner.create()
            .withGradleVersion(gradleVersion)
  }

  private static String gradleVersion() {
    if (JavaVersion.current().isJava10Compatible()) {
      return '4.7'
    }
    if (JavaVersion.current().isJava9Compatible()) {
      return '4.2.1'
    }
    return '4.0'
  }
}
