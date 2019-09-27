package aQute.bnd.gradle

import java.util.jar.*
import java.util.regex.Pattern

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

import aQute.libg.reporter.slf4j.Slf4jReporter
import aQute.lib.utf8properties.UTF8Properties

class TestBndPlugin extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')
    String pluginClasspath

    def setup() {
      URL propertiesFile = getClass().getResource('/plugin-under-test-metadata.properties')
      assert propertiesFile != null
      def properties = new Properties()
      propertiesFile.withInputStream {
        properties.load(it)
      }
      pluginClasspath = properties.'implementation-classpath'
      assert pluginClasspath != null
    }

    def "Bnd Workspace Plugin"() {
        given:
          String testProject = 'workspaceplugin1'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', '--debug', 'build', 'release')
            .forwardOutput()
            .build()

        then:
          result.task(':test.simple:jar').outcome == SUCCESS
          result.task(':test.simple:test').outcome == SUCCESS
          result.task(':test.simple:testOSGi').outcome == SUCCESS
          result.task(':test.simple:check').outcome == SUCCESS
          result.task(':test.simple:build').outcome == SUCCESS
          result.task(':test.simple:release').outcome == SUCCESS

          File simple_bundle = new File(testProjectDir, 'test.simple/generated/test.simple.jar')
          simple_bundle.isFile()

          JarFile simple_jar = new JarFile(simple_bundle)
          Attributes simple_manifest = simple_jar.getManifest().getMainAttributes()
          simple_manifest.getValue('Bundle-SymbolicName') == 'test.simple'
          simple_manifest.getValue('Bundle-Version') =~ /0\.0\.0\./
          simple_manifest.getValue('Foo') == 'foo'
          simple_manifest.getValue('Bar') == 'bar'
          simple_manifest.getValue('Import-Package') =~ /junit\.framework/
          simple_jar.getEntry('test/simple/Simple.class')
          simple_jar.getEntry('test/simple/Test.class')
          simple_jar.getEntry('OSGI-OPT/src/')
          simple_jar.getEntry('test.txt')
          simple_jar.getInputStream(simple_jar.getEntry('test.txt')).text =~ /This is a project resource/
          simple_jar.getEntry('test/simple/test.txt')
          simple_jar.getInputStream(simple_jar.getEntry('test/simple/test.txt')).text =~ /This is a package resource/
          simple_jar.close()

          File release_jar = new File(testProjectDir, 'cnf/releaserepo/test.simple/test.simple-0.0.0.jar')
          release_jar.isFile()

          File testReports = new File(testProjectDir, 'test.simple/generated/test-reports')
          new File(testReports, 'test/TEST-test.simple.JUnitTest.xml').isFile()
          new File(testReports, 'testOSGi/TEST-test.simple-0.0.0.xml').isFile()

          result.output =~ Pattern.quote("### Project ${testProject} has BndWorkspacePlugin applied")
          result.output =~ Pattern.quote('### Project test.simple has BndPlugin applied')
    }

    def "Bnd Workspace Plugin echo/bndproperties Test"() {
        given:
          String testProject = 'workspaceplugin5'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', 'echo', 'bndproperties', ':tasks')
            .forwardOutput()
            .build()

        then:
          result.task(':test.simple:echo').outcome == SUCCESS
          result.task(':test.simple:bndproperties').outcome == SUCCESS
          result.task(':tasks').outcome == SUCCESS
    }

    def "Bnd Workspace Plugin resolve Test"() {
        given:
          String testProject = 'workspaceplugin6'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', '--continue', ':test.simple:resolve', ':test.simple:resolve2')
            .forwardOutput()
            .buildAndFail()

        then:
          result.task(':test.simple:jar').outcome == SUCCESS
          result.task(':test.simple:resolve.resolve').outcome == SUCCESS
          result.task(':test.simple:resolve.resolvenochange').outcome == SUCCESS
          result.task(':test.simple:resolve.resolveerror').outcome == FAILED
          result.task(':test.simple:resolve.resolvechange').outcome == FAILED
          result.task(':test.simple:resolve2').outcome == SUCCESS

        when:
          File bndrun = new File(testProjectDir, 'test.simple/resolve.bndrun')
          UTF8Properties props = new UTF8Properties()
        then:
          bndrun.isFile()
          props.load(bndrun, new Slf4jReporter(TestBndPlugin.class))
          props.getProperty('-runbundles') =~ /osgi\.enroute\.junit\.wrapper\s*;\s*version='\[4\.12\.0,4\.12\.1\)'\s*,\s*test\.simple\s*;\s*version=snapshot/

        when:
          bndrun = new File(testProjectDir, 'test.simple/resolvenochange.bndrun')
          props = new UTF8Properties()
        then:
          bndrun.isFile()
          props.load(bndrun, new Slf4jReporter(TestBndPlugin.class))
          props.getProperty('-runbundles') =~ /osgi\.enroute\.junit\.wrapper\s*;\s*version='\[4\.12\.0,4\.12\.1\)'\s*,\s*test\.simple\s*;\s*version=snapshot/

        when:
          bndrun = new File(testProjectDir, 'test.simple/resolveerror.bndrun')
          props = new UTF8Properties()
        then:
          bndrun.isFile()
          props.load(bndrun, new Slf4jReporter(TestBndPlugin.class))
          props.getProperty('-runbundles') =~ /osgi\.enroute\.junit\.wrapper/
          result.output =~ /Unable to resolve <<INITIAL>>: missing requirement osgi\.identity;filter:='\(osgi\.identity=test\.simple\)'/

        when:
          bndrun = new File(testProjectDir, 'test.simple/resolvechange.bndrun')
          props = new UTF8Properties()
        then:
          bndrun.isFile()
          props.load(bndrun, new Slf4jReporter(TestBndPlugin.class))
          props.getProperty('-runbundles') =~ /osgi\.enroute\.junit\.wrapper/

        when:
          bndrun = new File(testProjectDir, 'test.simple/resolve2.bndrun')
          props = new UTF8Properties()
        then:
          bndrun.isFile()
          props.load(bndrun, new Slf4jReporter(TestBndPlugin.class))
          props.getProperty('-runbundles') =~ /osgi\.enroute\.junit\.wrapper\s*;\s*version='\[4\.12\.0,4\.12\.1\)'\s*,\s*test\.simple\s*;\s*version=snapshot/
    }

    def "Bnd Workspace Plugin export Test"() {
        given:
          String testProject = 'workspaceplugin7'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', '--continue', ':test.simple:export', ':test.simple:runbundles', ':test.simple:export2', ':test.simple:export3')
            .forwardOutput()
            .build()

        then:
          result.task(':test.simple:jar').outcome == SUCCESS
          result.task(':test.simple:export').outcome == SUCCESS
          result.task(':test.simple:export.export').outcome == SUCCESS
          result.task(':test.simple:runbundles').outcome == SUCCESS
          result.task(':test.simple:runbundles.export').outcome == SUCCESS
          result.task(':test.simple:export2').outcome == SUCCESS
          result.task(':test.simple:export3').outcome == SUCCESS

          File distributions = new File(testProjectDir, 'test.simple/generated/distributions')
          new File(distributions, 'runbundles/export/test.simple.jar').isFile()
          new File(distributions, 'runbundles/export/osgi.enroute.junit.wrapper-4.12.0.201507311000.jar').isFile()

          File executable = new File(distributions, 'executable/export.jar')
          executable.isFile()
          JarFile executable_jar = new JarFile(executable)
          Attributes executable_manifest = executable_jar.getManifest().getMainAttributes()
          def runpath = executable_manifest.getValue('Embedded-Runpath')
          runpath =~ /jar\/org\.eclipse\.osgi-3\.13\.0\.v20180409-1500\.jar/
          def launcher = runpath =~ /jar\/biz\.aQute\.launcher.*?\.jar/
          launcher.find()
          executable_jar.getEntry(launcher.group(0))
          executable_jar.getEntry('jar/test.simple.jar')
          executable_jar.getEntry('jar/osgi.enroute.junit.wrapper-4.12.0.201507311000.jar')
          executable_jar.getEntry('jar/org.eclipse.osgi-3.13.0.v20180409-1500.jar')
          executable_jar.getEntry('launcher.properties')
          UTF8Properties props = new UTF8Properties()
          props.load(executable_jar.getInputStream(executable_jar.getEntry('launcher.properties')), null, new Slf4jReporter(TestBndPlugin.class))
          props.getProperty('launch.bundles') =~ /jar\/osgi\.enroute\.junit\.wrapper-4\.12\.0\.201507311000\.jar\s*,\s*jar\/test\.simple\.jar/
          executable_jar.close()

        when:
          executable = new File(distributions, 'executable/export2.jar')
        then:
          executable.isFile()

        when:
          executable = new File(distributions, 'executable/test.simple.jar')
        then:
          executable.isFile()
    }

    def "Bnd Workspace Plugin extra properties/extensions Test"() {
        given:
          String testProject = 'workspaceplugin2'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', ':tasks')
            .forwardOutput()
            .build()

        then:
          result.task(':tasks').outcome == SUCCESS
    }

    def "Bnd Workspace Plugin Old-style settings.gradle"() {
        given:
          String testProject = 'workspaceplugin3'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', '--debug', 'build', 'release')
            .forwardOutput()
            .build()

        then:
          result.task(':test.simple:jar').outcome == SUCCESS
          result.task(':test.simple:test').outcome == SUCCESS
          result.task(':test.simple:testOSGi').outcome == SUCCESS
          result.task(':test.simple:check').outcome == SUCCESS
          result.task(':test.simple:build').outcome == SUCCESS
          result.task(':test.simple:release').outcome == SUCCESS

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
          simple_jar.close()

          File release_jar = new File(testProjectDir, 'cnf/releaserepo/test.simple/test.simple-0.0.0.jar')
          release_jar.isFile()

          File testReports = new File(testProjectDir, 'test.simple/generated/test-reports')
          new File(testReports, 'test/TEST-test.simple.Test.xml').isFile()
          new File(testReports, 'testOSGi/TEST-test.simple-0.0.0.xml').isFile()
    }

    def "Bnd Workspace Plugin Simple settings.gradle"() {
        given:
          String testProject = 'workspaceplugin4'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', '--debug', 'build', 'release')
            .forwardOutput()
            .build()

        then:
          result.task(':test.simple:jar').outcome == SUCCESS
          result.task(':test.simple:test').outcome == SUCCESS
          result.task(':test.simple:testOSGi').outcome == SUCCESS
          result.task(':test.simple:check').outcome == SUCCESS
          result.task(':test.simple:build').outcome == SUCCESS
          result.task(':test.simple:release').outcome == SUCCESS

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
          simple_jar.close()

          File release_jar = new File(testProjectDir, 'cnf/releaserepo/test.simple/test.simple-0.0.0.jar')
          release_jar.isFile()

          File testReports = new File(testProjectDir, 'test.simple/generated/test-reports')
          new File(testReports, 'test/TEST-test.simple.Test.xml').isFile()
          new File(testReports, 'testOSGi/TEST-test.simple-0.0.0.xml').isFile()
    }

    def "Bnd Plugin workspace not rootProject"() {
        given:
          String testProject = 'workspaceplugin8'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', '--debug', 'build', 'release')
            .forwardOutput()
            .build()

        then:
          result.task(':build').outcome == SUCCESS
          result.task(':workspace:build').outcome == SUCCESS
          result.task(':workspace:test.simple:jar').outcome == SUCCESS
          result.task(':workspace:test.simple:test').outcome == SUCCESS
          result.task(':workspace:test.simple:testOSGi').outcome == SUCCESS
          result.task(':workspace:test.simple:check').outcome == SUCCESS
          result.task(':workspace:test.simple:build').outcome == SUCCESS
          result.task(':workspace:test.simple:release').outcome == SUCCESS

          File simple_bundle = new File(testProjectDir, 'workspace/test.simple/generated/test.simple.jar')
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
          simple_jar.close()

          File release_jar = new File(testProjectDir, 'workspace/cnf/releaserepo/test.simple/test.simple-0.0.0.jar')
          release_jar.isFile()

          File testReports = new File(testProjectDir, 'workspace/test.simple/generated/test-reports')
          new File(testReports, 'test/TEST-test.simple.Test.xml').isFile()
          new File(testReports, 'testOSGi/TEST-test.simple-0.0.0.xml').isFile()

          result.output =~ Pattern.quote("### Project ${testProject} : is rootProject")
          result.output =~ Pattern.quote('### Project workspace :workspace Workspace [workspace]')

    }

    def "Bnd Workspace Plugin TestOSGi tests option"() {
        given:
          String testProject = 'workspaceplugin9'
          File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', '--debug', 'check', '--exclude-task', 'testrun.testOSGi2')
            .forwardOutput()
            .build()

        then:
          result.task(':test.simple:jar').outcome == SUCCESS
          result.task(':test.simple:test').outcome == SUCCESS
          result.task(':test.simple:testOSGi').outcome == SUCCESS
          result.task(':test.simple:check').outcome == SUCCESS

          File simple_bundle = new File(testProjectDir, 'test.simple/generated/test.simple.jar')
          simple_bundle.isFile()

          JarFile simple_jar = new JarFile(simple_bundle)
          Attributes simple_manifest = simple_jar.getManifest().getMainAttributes()
          simple_manifest.getValue('Bundle-SymbolicName') == 'test.simple'
          simple_manifest.getValue('Bundle-Version') =~ /0\.0\.0\./
          simple_manifest.getValue('Foo') == 'foo'
          simple_manifest.getValue('Bar') == 'bar'
          simple_manifest.getValue('Import-Package') =~ /junit\.framework/
          simple_manifest.getValue('Test-Cases') =~ /test\.simple\.Test/
          simple_manifest.getValue('Test-Cases') =~ /test\.simple\.FailingTest/
          simple_jar.getEntry('test/simple/Simple.class')
          simple_jar.getEntry('test/simple/Test.class')
          simple_jar.getEntry('test/simple/FailingTest.class')
          simple_jar.getEntry('OSGI-OPT/src/')
          simple_jar.getEntry('test.txt')
          simple_jar.getInputStream(simple_jar.getEntry('test.txt')).text =~ /This is a project resource/
          simple_jar.getEntry('test/simple/test.txt')
          simple_jar.getInputStream(simple_jar.getEntry('test/simple/test.txt')).text =~ /This is a package resource/
          simple_jar.close()

          File testReports = new File(testProjectDir, 'test.simple/generated/test-reports')
          new File(testReports, 'test/TEST-test.simple.JUnitTest.xml').isFile()
          new File(testReports, 'testOSGi/TEST-testOSGi.xml').isFile()

        when:
          result = TestHelper.getGradleRunner('4.6')
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', '--debug', 'testrun.testOSGi2')
            .forwardOutput()
            .buildAndFail()

        then:
          result.task(':test.simple:testrun.testOSGi2').outcome == FAILED

        when:
          result = TestHelper.getGradleRunner('4.6')
            .withProjectDir(testProjectDir)
            .withArguments("-Pbnd_plugin=${pluginClasspath}", '--parallel', '--stacktrace', '--debug', 'testrun.testOSGi2', '--tests=test.simple.Test')
            .forwardOutput()
            .build()

        then:
          result.task(':test.simple:testrun.testOSGi2').outcome == SUCCESS

          new File(testReports, 'testrun.testOSGi2/TEST-testrun.testOSGi2.xml').isFile()
    }

}
