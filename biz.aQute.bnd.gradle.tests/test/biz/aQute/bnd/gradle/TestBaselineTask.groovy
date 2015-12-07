package aQute.bnd.gradle

import java.util.jar.*
import java.util.regex.Pattern

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

class TestBaselineTask extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')
    List<File> pluginClasspath

    def setup() {
      File plugin = new File('../biz.aQute.bnd.gradle/generated/biz.aQute.bnd.gradle.jar').getCanonicalFile()
      assert plugin.isFile()
      pluginClasspath = Collections.singletonList(plugin)
    }

    def "Simple Bnd Baseline Task Test"() {
        given:
          String testProject = 'baselinetask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectReportsDir = new File(testProjectDir, 'build/reports').canonicalFile

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('baseline', 'baselineSelf')
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .build()

        then:
          result.task(":jar").outcome == SUCCESS
          result.task(":baseline").outcome == SUCCESS
          result.task(":baselineSelf").outcome == SUCCESS

          testProjectReportsDir.isDirectory()
          File baseline = new File(testProjectReportsDir, 'baseline/baseline.txt')
          baseline.isFile()
          File baselineSelf = new File(testProjectReportsDir, 'baselineSelf/baselineSelf.txt')
          baselineSelf.isFile()

          String baselineFile = Pattern.quote(baseline.absolutePath)
          result.getOutput() =~ /Baseline problems detected\. See the report in ${baselineFile}/
    }
}
