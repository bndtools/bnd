package aQute.bnd.gradle

import java.util.jar.*
import java.util.regex.Pattern

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

class TestBaselineTask extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')

    def "Simple Bnd Baseline Task Test"() {
        given:
          String testProject = 'baselinetask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectReportsDir = new File(testProjectDir, 'build/reports').canonicalFile

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--parallel', '--stacktrace', '--debug', 'baseline', 'baselineSelf', 'baselineDiffpackages', 'baselineDiffignore')
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(':jar').outcome == SUCCESS
          result.task(':baseline').outcome == SUCCESS
          result.task(':baselineSelf').outcome == SUCCESS
          result.task(':baselineDiffpackages').outcome == SUCCESS
          result.task(':baselineDiffignore').outcome == SUCCESS

          testProjectReportsDir.isDirectory()
          File baseline = new File(testProjectReportsDir, "baseline/baseline/${testProject}-1.1.0.txt")
          baseline.isFile()
          File baselineSelf = new File(testProjectReportsDir, "foo/baselineSelf/${testProject}-1.1.0.txt")
          baselineSelf.isFile()
          File baselineDiffpackages = new File(testProjectReportsDir, "baseline/baselineDiffpackages/${testProject}-1.1.0.txt")
          baselineDiffpackages.isFile()
          File baselineDiffignore = new File(testProjectReportsDir, "baseline/baselineDiffignore/${testProject}-1.1.0.txt")
          baselineDiffignore.isFile()

          //result.getOutput() =~ Pattern.quote("Baseline problems detected. See the report in ${baseline.absolutePath}")
    }

    def "Bnd Baseline Configuration Test"() {
        given:
          String testProject = 'baselinetask2'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--parallel', '--stacktrace', 'echo')
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(':echo').outcome == SUCCESS

          result.getOutput() =~ Pattern.quote('Bundle-SymbolicName: biz.aQute.bnd')
          result.getOutput() =~ Pattern.quote('Bundle-Version: 2.4.1')
    }

    def "Bnd No Baseline Test"() {
        given:
          String testProject = 'baselinetask3'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectReportsDir = new File(testProjectDir, 'build/reports').canonicalFile

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--parallel', '--stacktrace', 'tasks', 'baseline')
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(':tasks').outcome == SUCCESS
          result.task(':baseline').outcome == SUCCESS

          testProjectReportsDir.isDirectory()
          File baseline = new File(testProjectReportsDir, "baseline/baseline/${testProject}-1.0.txt")
          baseline.isFile()
    }

    def "Bnd Baseline Configuration Task Test"() {
        given:
          String testProject = 'baselinetask4'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectReportsDir = new File(testProjectDir, 'build/reports').canonicalFile

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--parallel', '--stacktrace', '--debug', 'baseline')
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(':jar').outcome == SUCCESS
          result.task(':baseline').outcome == SUCCESS

          testProjectReportsDir.isDirectory()
          File baseline = new File(testProjectReportsDir, "baseline/baseline/${testProject}-1.1.0.txt")
          baseline.isFile()

          result.getOutput() =~ Pattern.quote("Baseline problems detected. See the report in ${baseline.absolutePath}")
    }
}
