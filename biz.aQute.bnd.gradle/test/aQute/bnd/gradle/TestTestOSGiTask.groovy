package aQute.bnd.gradle

import java.util.jar.*
import java.util.regex.Pattern

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

class TestTestOSGiTask extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')
    List<File> pluginClasspath

    def setup() {
      File plugin = new File(buildDir, 'biz.aQute.bnd.gradle.jar').getCanonicalFile()
      assert plugin.isFile()
      pluginClasspath = Collections.singletonList(plugin)
    }

    def "Bnd TestOSGi Task Basic Test"() {
        given:
          String testProject = 'testosgitask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectBuildDir = new File(testProjectDir, 'build').canonicalFile
          String taskname = 'testosgi'

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', '--debug', 'build')
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .build()

        then:
          result.task(':test').outcome == SUCCESS
          result.task(':jar').outcome == SUCCESS
          result.task(":${taskname}").outcome == SUCCESS

          testProjectBuildDir.isDirectory()

          File jartask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.0.0.jar")
          jartask_bundle.isFile()
          JarFile jartask_jar = new JarFile(jartask_bundle)
          Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()

          jartask_manifest.getValue('Bundle-SymbolicName') == "${testProject}"
          jartask_manifest.getValue('Bundle-Version') == '1.0.0'
          jartask_manifest.getValue('Export-Package') =~ /doubler/
          jartask_manifest.getValue('Test-Cases') == 'doubler.impl.DoublerImplOSGiTest'
          jartask_jar.getEntry('doubler/Doubler.class')
          jartask_jar.getEntry('doubler/packageinfo')
          jartask_jar.getEntry('doubler/impl/DoublerImpl.class')
          jartask_jar.getEntry('doubler/impl/packageinfo')
          jartask_jar.getEntry('doubler/impl/DoublerImplOSGiTest.class')
          !jartask_jar.getEntry('doubler/impl/DoublerImplUnitTest.class')
          jartask_jar.getEntry('OSGI-OPT/src/')
          jartask_jar.getEntry('OSGI-OPT/src/doubler/packageinfo')
          jartask_jar.getEntry('OSGI-OPT/src/doubler/impl/packageinfo')
          jartask_jar.getInputStream(jartask_jar.getEntry('OSGI-OPT/src/doubler/packageinfo')).text =~ /version 1\.0/
          jartask_jar.getInputStream(jartask_jar.getEntry('OSGI-OPT/src/doubler/impl/packageinfo')).text =~ /version 1\.2/
          jartask_jar.getEntry('foo.txt')
          jartask_jar.getInputStream(jartask_jar.getEntry('foo.txt')).text =~ /Hi!/
          !jartask_jar.getEntry('test.txt')
          jartask_jar.close()

        when:
          File testxml = new File(testProjectBuildDir, 'test-results/test/TEST-doubler.impl.DoublerImplUnitTest.xml')
        then:
          testxml.isFile()
          testxml.text =~ /<testcase name=['"]testIt["'].*classname=['"]doubler.impl.DoublerImplUnitTest["']/

        when:
          testxml = new File(testProjectBuildDir, "test-results/${taskname}/TEST-${testProject}-1.0.0.xml")
        then:
          testxml.isFile()
          testxml.text =~ /<testcase name=['"]testIt["'].*classname=['"]doubler.impl.DoublerImplOSGiTest["']/
    }

    def "Bnd TestOSGi Task Working Dir Test"() {
        given:
          String testProject = 'testosgitask2'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectBuildDir = new File(testProjectDir, 'build').canonicalFile
          String taskname = 'testosgi'

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', '--debug', 'build')
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .build()

        then:
          result.task(':test').outcome == SUCCESS
          result.task(':jar').outcome == SUCCESS
          result.task(":${taskname}").outcome == SUCCESS

          testProjectBuildDir.isDirectory()
          new File(testProjectBuildDir, "tmp/${taskname}/temptemp" ).isDirectory()

        when:
          File testxml = new File(testProjectBuildDir, 'test-results/test/TEST-doubler.impl.DoublerImplUnitTest.xml')
        then:
          testxml.isFile()
          testxml.text =~ /<testcase name=['"]testIt["'].*classname=['"]doubler.impl.DoublerImplUnitTest["']/

        when:
          testxml = new File(testProjectBuildDir, "test-results/${taskname}/TEST-${testProject}-1.0.0.xml")
        then:
          testxml.isFile()
          testxml.text =~ /<testcase name=['"]testIt["'].*classname=['"]doubler.impl.DoublerImplOSGiTest["']/
    }

    def "Bnd TestOSGi Task Replace bundles Test"() {
        given:
          String testProject = 'testosgitask3'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectBuildDir = new File(testProjectDir, 'build').canonicalFile
          String taskname = 'testosgi'

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', '--debug', 'build')
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .build()

        then:
          result.task(':test').outcome == SUCCESS
          result.task(':jar').outcome == SUCCESS
          result.task(":${taskname}").outcome == SUCCESS

          testProjectBuildDir.isDirectory()

        when:
          File testxml = new File(testProjectBuildDir, 'test-results/test/TEST-doubler.impl.DoublerImplUnitTest.xml')
        then:
          testxml.isFile()
          testxml.text =~ /<testcase name=['"]testIt["'].*classname=['"]doubler.impl.DoublerImplUnitTest["']/

        when:
          testxml = new File(testProjectBuildDir, "test-results/${taskname}/TEST-${testProject}-1.0.0.xml")
        then:
          testxml.isFile()
          testxml.text =~ /<testcase name=['"]testIt["'].*classname=['"]doubler.impl.DoublerImplOSGiTest["']/
    }

    def "Bnd TestOSGi Task ignore failure Test"() {
        given:
          String testProject = 'testosgitask4'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectBuildDir = new File(testProjectDir, 'build').canonicalFile

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', '--debug', '--continue', 'build')
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .buildAndFail()

        then:
          result.task(':jar').outcome == SUCCESS
          testProjectBuildDir.isDirectory()

        when:
          String taskname = 'testosgiIgnoreFail'
          File testxml = new File(testProjectBuildDir, "test-results/${taskname}/TEST-${testProject}-1.0.0.xml")
        then:
          result.task(":${taskname}").outcome == SUCCESS
          testxml.isFile()
          testxml.text =~ /<testcase name=['"]testIt["'].*classname=['"]doubler.impl.DoublerImplOSGiTest["']/

        when:
          taskname = 'testosgiFail'
          testxml = new File(testProjectBuildDir, "test-results/${taskname}/TEST-${testProject}-1.0.0.xml")
        then:
          result.task(":${taskname}").outcome == FAILED
          testxml.isFile()
          testxml.text =~ /<testcase name=['"]testIt["'].*classname=['"]doubler.impl.DoublerImplOSGiTest["']/
    }
}
