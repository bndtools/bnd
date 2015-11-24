package aQute.bnd.gradle

import java.util.jar.*;

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

class TestBundlePlugin extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')
    List<File> pluginClasspath

    def setup() {
      def File plugin = new File('../biz.aQute.bnd.gradle/generated/biz.aQute.bnd.gradle.jar').getCanonicalFile()
      assert plugin.isFile()
      pluginClasspath = Collections.singletonList(plugin)
    }

    def "Simple Bnd Builder Plugin Test"() {
        given:
          def String testProject = 'builderplugin1'
          def File testProjectDir = new File(testResources, testProject)
          assert testProjectDir.isDirectory()
          def File testProjectBuildDir = new File(testProjectDir, 'build/libs')

        when:
          def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('build')
            .withPluginClasspath(pluginClasspath)
            .forwardOutput()
            .build()

        then:
          result.task(":bundle").outcome == SUCCESS
          result.task(":jar").outcome == SUCCESS

          testProjectBuildDir.isDirectory()

          def File jartask_bundle = new File(testProjectBuildDir, "${testProject}-1.0.0.jar")
          jartask_bundle.isFile()
          def JarFile jartask_jar = new JarFile(jartask_bundle)
          def Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()
          def File bundletask_bundle = new File(testProjectBuildDir, "${testProject}_bundle-1.1.0.jar")
          bundletask_bundle.isFile()
          def JarFile bundletask_jar = new JarFile(bundletask_bundle)
          def Attributes bundletask_manifest = bundletask_jar.getManifest().getMainAttributes()

          jartask_manifest.getValue('Bundle-SymbolicName') == "${testProject}"
          jartask_manifest.getValue('Bundle-Version') == '1.0.0'
          jartask_manifest.getValue('Implementation-Title') == "${testProject}"
          jartask_manifest.getValue('Implementation-Version') == '1.0.0'
          jartask_manifest.getValue('My-Header') == 'my-value'
          jartask_manifest.getValue('Export-Package') =~ /doubler/
          jartask_manifest.getValue('X-SomeProperty') == 'Included via -include in jar task manifest'
          jartask_manifest.getValue('Override') == 'Override the jar task manifest'
          jartask_jar.getEntry('doubler/Doubler.class')
          jartask_jar.getEntry('doubler/packageinfo')
          jartask_jar.getEntry('doubler/impl/DoublerImpl.class')
          jartask_jar.getEntry('doubler/impl/packageinfo')
          !jartask_jar.getEntry('doubler/impl/DoublerImplTest.class')
          jartask_jar.getEntry('META-INF/services/foo.properties')
          jartask_jar.getInputStream(jartask_jar.getEntry('META-INF/services/foo.properties')).text =~ /key=value/
          jartask_jar.getEntry('OSGI-OPT/src/')
          jartask_jar.getEntry('OSGI-OPT/src/doubler/packageinfo')
          jartask_jar.getEntry('OSGI-OPT/src/doubler/impl/packageinfo')
          jartask_jar.getInputStream(jartask_jar.getEntry('OSGI-OPT/src/doubler/packageinfo')).text =~ /version 1\.0/
          jartask_jar.getInputStream(jartask_jar.getEntry('OSGI-OPT/src/doubler/impl/packageinfo')).text =~ /version 1\.2/
          jartask_jar.getEntry('foo.txt')
          jartask_jar.getInputStream(jartask_jar.getEntry('foo.txt')).text =~ /Hi!/
          jartask_jar.getEntry('bar.txt')
          jartask_jar.getInputStream(jartask_jar.getEntry('bar.txt')).text =~ /Some more TEXT/
          !jartask_jar.getEntry('test.txt')

          bundletask_manifest.getValue('Bundle-SymbolicName') == "${testProject}_bundle"
          bundletask_manifest.getValue('Bundle-Version') == '1.1.0'
          bundletask_manifest.getValue('My-Header') == 'my-value'
          bundletask_manifest.getValue('Export-Package') =~ /doubler/
          !bundletask_manifest.getValue('X-SomeProperty')
          bundletask_manifest.getValue('Override') == 'Override the jar task manifest'
          !bundletask_jar.getEntry('doubler/Doubler.class')
          !bundletask_jar.getEntry('doubler/impl/DoublerImpl.class')
          bundletask_jar.getEntry('doubler/impl/DoublerImplTest.class')
          bundletask_jar.getEntry('OSGI-OPT/src/')
          !bundletask_jar.getEntry('foo.txt')
          !bundletask_jar.getEntry('bar.txt')
          bundletask_jar.getEntry('test.txt')
          bundletask_jar.getInputStream(bundletask_jar.getEntry('test.txt')).text =~ /This is a test resource/
    }
}
