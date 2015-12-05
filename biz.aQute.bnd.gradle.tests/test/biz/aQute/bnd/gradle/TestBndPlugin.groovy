package aQute.bnd.gradle

import java.util.jar.*;

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

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
            .withArguments('build', 'release')
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

          File release_jar = new File(testProjectDir, 'cnf/repo/test.simple/test.simple-0.0.0.jar')
          release_jar.isFile()
    }
}
