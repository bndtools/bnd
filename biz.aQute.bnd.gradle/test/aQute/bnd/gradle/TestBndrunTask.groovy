package aQute.bnd.gradle

import java.util.jar.*
import java.util.regex.Pattern

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

class TestBndrunTask extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')

    def "Bnd Bndrun Task Basic Test"() {
        given:
          String testProject = 'runtask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectBuildDir = new File(testProjectDir, 'build').canonicalFile
          String taskname = 'run'

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--parallel', '--stacktrace', '--debug', 'build', taskname)
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(':jar').outcome == SUCCESS
          result.task(":${taskname}").outcome == SUCCESS

          testProjectBuildDir.isDirectory()

          File jartask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.0.0.jar")
          jartask_bundle.isFile()
          JarFile jartask_jar = new JarFile(jartask_bundle)
          Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()

          jartask_manifest.getValue('Bundle-SymbolicName') == "${testProject}"
          jartask_manifest.getValue('Bundle-Version') == '1.0.0'
          jartask_jar.getEntry('run/Activator.class')
          jartask_jar.close()

          result.getOutput() =~ Pattern.quote('Run Barry, RUN!!!')

    }
}
