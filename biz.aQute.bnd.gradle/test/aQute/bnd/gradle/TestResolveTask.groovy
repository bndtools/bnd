package aQute.bnd.gradle

import java.util.jar.*
import java.util.regex.Pattern

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

import aQute.libg.reporter.slf4j.Slf4jReporter
import aQute.lib.utf8properties.UTF8Properties

class TestResolveTask extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')
    List<File> pluginClasspath

    def setup() {
      File plugin = new File(buildDir, 'biz.aQute.bnd.gradle.jar').getCanonicalFile()
      assert plugin.isFile()
      pluginClasspath = Collections.singletonList(plugin)
    }

    def "Simple Bnd Resolve Task Generate -runbundles Test"() {
        given:
          String testProject = 'resolvetask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          def reporter = new Slf4jReporter(TestResolveTask.class)
          String taskname = 'create'

        when:
          File bndrun = new File(testProjectDir, "${taskname}.bndrun")
          UTF8Properties props = new UTF8Properties()

        then:
          bndrun.isFile()
          props.load(bndrun, reporter)
          !props.getProperty('-runbundles')

        when:
          props = new UTF8Properties()
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', '--debug', taskname)
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .build()

        then:
          result.task(":${taskname}").outcome == SUCCESS
          bndrun.isFile()
          props.load(bndrun, reporter)
          props.getProperty('-runbundles') =~ /org\.apache\.felix\.eventadmin\s*;\s*version\s*=\s*'\[1\.4\.6,1\.4\.7\)'/
    }

    def "Simple Bnd Resolve Task Same -runbundles Test"() {
        given:
          String testProject = 'resolvetask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          def reporter = new Slf4jReporter(TestResolveTask.class)
          String taskname = 'same'

        when:
          File bndrun = new File(testProjectDir, "${taskname}.bndrun")
          UTF8Properties props = new UTF8Properties()

        then:
          bndrun.isFile()
          props.load(bndrun, reporter)
          props.getProperty('-runbundles') =~ /org\.apache\.felix\.eventadmin\s*;\s*version\s*=\s*'\[1\.4\.6,1\.4\.7\)'/

        when:
          props = new UTF8Properties()
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', '--debug', taskname)
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .build()

        then:
          result.task(":${taskname}").outcome == SUCCESS
          bndrun.isFile()
          props.load(bndrun, reporter)
          props.getProperty('-runbundles') =~ /org\.apache\.felix\.eventadmin\s*;\s*version\s*=\s*'\[1\.4\.6,1\.4\.7\)'/
    }

    def "Simple Bnd Resolve Task Fail On Change Test"() {
        given:
          String testProject = 'resolvetask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          def reporter = new Slf4jReporter(TestResolveTask.class)
          String taskname = 'changefail'

        when:
          File bndrun = new File(testProjectDir, "${taskname}.bndrun")
          UTF8Properties props = new UTF8Properties()

        then:
          bndrun.isFile()
          props.load(bndrun, reporter)
          props.getProperty('-runbundles') =~ /foo/

        when:
          props = new UTF8Properties()
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', '--debug', taskname)
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .buildAndFail()

        then:
          result.task(":${taskname}").outcome == FAILED
          result.output =~ /${taskname}\.bndrun resolution failure/
          bndrun.isFile()
          props.load(bndrun, reporter)
          props.getProperty('-runbundles') =~ /foo/
    }

    def "Simple Bnd Resolve Task Resolve Fail Test"() {
        given:
          String testProject = 'resolvetask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          def reporter = new Slf4jReporter(TestResolveTask.class)
          String taskname = 'resolvefail'

        when:
          File bndrun = new File(testProjectDir, "${taskname}.bndrun")
          UTF8Properties props = new UTF8Properties()

        then:
          bndrun.isFile()
          props.load(bndrun, reporter)
          !props.getProperty('-runbundles')

        when:
          props = new UTF8Properties()
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', '--debug', taskname)
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .buildAndFail()

        then:
          result.task(":${taskname}").outcome == FAILED
          result.output =~ /${taskname}\.bndrun resolution exception/
          bndrun.isFile()
          props.load(bndrun, reporter)
          !props.getProperty('-runbundles')
    }
}
