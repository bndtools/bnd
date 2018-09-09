package aQute.bnd.gradle

import aQute.lib.utf8properties.UTF8Properties
import aQute.libg.reporter.slf4j.Slf4jReporter
import spock.lang.Specification

import java.util.jar.Attributes
import java.util.jar.JarFile

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class TestWrap extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')
    List<File> pluginClasspath

    def setup() {
        File plugin = new File(buildDir, 'biz.aQute.bnd.gradle.jar').getCanonicalFile()
        assert plugin.isFile()
        pluginClasspath = Collections.singletonList(plugin)
    }

    def "Bnd Wrap Task"() {
        given:
        String testProject = 'wraptask1'
        File testProjectDir = new File(testResources, testProject).canonicalFile
        assert testProjectDir.isDirectory()
        String taskname = 'wrap'

        when:
        def result = TestHelper.getGradleRunner()
                .withProjectDir(testProjectDir)
                .withArguments('--stacktrace', '--debug', taskname)
                .withPluginClasspath(pluginClasspath)
                .forwardOutput()
                .withDebug(true)
                .build()

        then:
        result.task(":${taskname}").outcome == SUCCESS
        File output = new File(testProjectDir, "out.jar")
        assert output.isFile()
    }
}
