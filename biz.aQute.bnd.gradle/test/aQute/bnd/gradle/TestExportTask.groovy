package aQute.bnd.gradle

import java.util.jar.*
import java.util.regex.Pattern

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

import aQute.libg.reporter.slf4j.Slf4jReporter
import aQute.lib.utf8properties.UTF8Properties

class TestExportTask extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')

    def "Simple Bnd Export Task Executable Jar Test"() {
        given:
          String testProject = 'exporttask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          def reporter = new Slf4jReporter(TestResolveTask.class)
          String taskname = 'export'

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
            .withArguments('--parallel', '--stacktrace', '--debug', taskname)
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(":${taskname}").outcome == SUCCESS
          bndrun.isFile()
          props.load(bndrun, reporter)
          props.getProperty('-runbundles') =~ /org\.apache\.felix\.eventadmin\s*;\s*version\s*=\s*'\[1\.4\.6,1\.4\.7\)'/

          File distributions = new File(testProjectDir, 'build/distributions')

          File executable = new File(distributions, "executable/${taskname}.jar")
          executable.isFile()
          JarFile executable_jar = new JarFile(executable)
          Attributes executable_manifest = executable_jar.getManifest().getMainAttributes()
          def runpath = executable_manifest.getValue('Embedded-Runpath')
          runpath =~ /jar\/biz\.aQute\.launcher/
          runpath =~ /jar\/org\.apache\.felix\.framework/
          executable_jar.getEntry('jar/org.apache.felix.eventadmin-1.4.6.jar')
          executable_jar.getEntry('launcher.properties')
          UTF8Properties launchprops = new UTF8Properties()
          launchprops.load(executable_jar.getInputStream(executable_jar.getEntry('launcher.properties')), null, reporter)
          launchprops.getProperty('launch.bundles') =~ /jar\/org\.apache\.felix\.eventadmin-1\.4\.6\.jar/
          launchprops.getProperty('launch.keep') == 'true'
          executable_jar.close()
    }

    def "Simple Bnd Export Task Runbundles Jar Test"() {
        given:
          String testProject = 'exporttask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          def reporter = new Slf4jReporter(TestResolveTask.class)
          String taskname = 'runbundles'

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
            .withArguments('--parallel', '--stacktrace', '--debug', taskname)
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(":${taskname}").outcome == SUCCESS
          bndrun.isFile()
          props.load(bndrun, reporter)
          props.getProperty('-runbundles') =~ /org\.apache\.felix\.eventadmin\s*;\s*version\s*=\s*'\[1\.4\.6,1\.4\.7\)'/

          File distributions = new File(testProjectDir, 'build/distributions')

          File folder = new File(distributions, "runbundles/${taskname}")
          folder.isDirectory()
          new File(folder, 'org.apache.felix.eventadmin-1.4.6.jar').isFile()
    }

    def "Bnd Export Task Named Exporter Test"() {
        given:
          String testProject = 'exporttask1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          def reporter = new Slf4jReporter(TestResolveTask.class)
          String taskname = 'exporter'

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
            .withArguments('--parallel', '--stacktrace', '--debug', taskname)
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(":${taskname}").outcome == SUCCESS
          bndrun.isFile()
          props.load(bndrun, reporter)
          props.getProperty('-runbundles') =~ /org\.apache\.felix\.eventadmin\s*;\s*version\s*=\s*'\[1\.4\.6,1\.4\.7\)'/

          File distributions = new File(testProjectDir, 'build/distributions')

          File folder = new File(distributions, "runbundles/${taskname}")
          folder.isDirectory()
          new File(folder, 'org.apache.felix.eventadmin-1.4.6.jar').isFile()
    }
}
