package aQute.bnd.gradle

import java.util.jar.*;

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

import aQute.libg.reporter.slf4j.Slf4jReporter
import aQute.lib.utf8properties.UTF8Properties

class TestBndPlugin extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')

    def "Simple Bnd Workspace Plugin Test"() {
        given:
          String testProject = 'workspaceplugin1'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', '--debug', 'build', 'release')
            .forwardOutput()
            .build()

        then:
          result.task(":test.simple:test").outcome == SUCCESS
          result.task(":test.simple:check").outcome == SUCCESS
          result.task(":test.simple:build").outcome == SUCCESS
          result.task(":test.simple:release").outcome == SUCCESS

          File simple_bundle = new File(testProjectDir, 'test.simple/generated/test.simple.jar')
          simple_bundle.isFile()

          JarFile simple_jar = new JarFile(simple_bundle)
          Attributes simple_manifest = simple_jar.getManifest().getMainAttributes()
          simple_manifest.getValue('Bundle-SymbolicName') == 'test.simple'
          simple_manifest.getValue('Bundle-Version') =~ /0\.0\.0\./
          simple_manifest.getValue('Foo') == 'foo'
          simple_manifest.getValue('Bar') == 'bar'
          simple_manifest.getValue('Import-Package') =~ /junit\.framework/
          simple_jar.getEntry('test/simple/Test.class')
          simple_jar.getEntry('OSGI-OPT/src/')
          simple_jar.getEntry('test.txt')
          simple_jar.getInputStream(simple_jar.getEntry('test.txt')).text =~ /This is a test resource/
          simple_jar.getEntry('test/simple/test.txt')
          simple_jar.getInputStream(simple_jar.getEntry('test/simple/test.txt')).text =~ /This is a test resource/

          File release_jar = new File(testProjectDir, 'cnf/releaserepo/test.simple/test.simple-0.0.0.jar')
          release_jar.isFile()
    }

    def "Bnd Workspace Plugin echo/bndproperties Test"() {
        given:
          String testProject = 'workspaceplugin1'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', 'echo', 'bndproperties', ':tasks')
            .forwardOutput()
            .build()

        then:
          result.task(":test.simple:echo").outcome == SUCCESS
          result.task(":test.simple:bndproperties").outcome == SUCCESS
          result.task(":tasks").outcome == SUCCESS
    }

    def "Bnd Workspace Plugin resolve Test"() {
        given:
          String testProject = 'workspaceplugin1'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', ':test.simple:resolve.resolve')
            .forwardOutput()
            .build()

        then:
          result.task(":test.simple:resolve.resolve").outcome == SUCCESS
          File bndrun = new File(testProjectDir, 'test.simple/resolve.bndrun')
          bndrun.isFile()
          UTF8Properties props = new UTF8Properties()
          props.load(bndrun, new Slf4jReporter(TestBndPlugin.class))
          props.getProperty('-runbundles') =~ /osgi\.enroute\.junit\.wrapper\s*;\s*version='\[4\.12\.0,4\.12\.1\)'\s*,\s*test\.simple\s*;\s*version=snapshot/
    }

    def "Bnd Workspace Plugin extra properties/extentions Test"() {
        given:
          String testProject = 'workspaceplugin2'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('--stacktrace', ':tasks')
            .forwardOutput()
            .build()

        then:
          result.task(":tasks").outcome == SUCCESS
    }
}
