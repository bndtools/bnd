package aQute.bnd.gradle

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner

class TestHelper {

  private TestHelper() { }

  public static GradleRunner getGradleRunner() {
    return GradleRunner.create()
            .withGradleVersion(JavaVersion.current().isJava9Compatible() ? "4.2.1" : "4.0")
  }
}
