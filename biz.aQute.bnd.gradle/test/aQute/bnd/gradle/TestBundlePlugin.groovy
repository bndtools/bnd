package aQute.bnd.gradle

import java.util.jar.*

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.Specification

class TestBundlePlugin extends Specification {

    File buildDir = new File('generated')
    File testResources = new File(buildDir, 'testresources')

    def "Simple Bnd Builder Plugin Test"() {
        given:
          String testProject = 'builderplugin1'
          File testProjectDir = new File(testResources, testProject).canonicalFile
          assert testProjectDir.isDirectory()
          File testProjectBuildDir = new File(testProjectDir, 'build').canonicalFile

        when:
          def result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--parallel', '--stacktrace', '--debug', 'build')
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(':bundle').outcome == SUCCESS
          result.task(':jar').outcome == SUCCESS

          testProjectBuildDir.isDirectory()

          File jartask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.0.0.jar")
          jartask_bundle.isFile()
          JarFile jartask_jar = new JarFile(jartask_bundle)
          Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()
          File bundletask_bundle = new File(testProjectBuildDir, "libs/${testProject}_bundle-1.1.0.jar")
          bundletask_bundle.isFile()
          JarFile bundletask_jar = new JarFile(bundletask_bundle)
          Attributes bundletask_manifest = bundletask_jar.getManifest().getMainAttributes()

          jartask_manifest.getValue('Bundle-SymbolicName') == "${testProject}"
          jartask_manifest.getValue('Bundle-Version') == '1.0.0'
          jartask_manifest.getValue('Implementation-Title') == "${testProject}"
          jartask_manifest.getValue('Implementation-Version') == '1.0.0'
          jartask_manifest.getValue('My-Header') == 'my-value'
          jartask_manifest.getValue('Export-Package') =~ /doubler/
          jartask_manifest.getValue('X-SomeProperty') == 'Included via -include in jar task manifest'
          jartask_manifest.getValue('Override') == 'Override the jar task manifest'
          jartask_manifest.getValue('Bundle-Name') == "test.bnd.gradle:${testProject}"
          jartask_manifest.getValue('Project-Name') == "${testProject}"
          new File(jartask_manifest.getValue('Project-Dir')).canonicalFile == testProjectDir
          new File(jartask_manifest.getValue('Project-Output')).canonicalFile == testProjectBuildDir
          jartask_manifest.getValue('Project-Sourcepath')
          jartask_manifest.getValue('Project-Buildpath')
          jartask_manifest.getValue('Bundle-ClassPath') =~ /commons-lang-2\.6\.jar/
          jartask_manifest.getValue('Gradle-Project-Prop') == 'prop.project'
          jartask_manifest.getValue('Gradle-Task-Prop') == 'prop.task'
          jartask_manifest.getValue('Gradle-Task-Project-Prop') == 'prop.project'
          jartask_manifest.getValue('Gradle-Missing-Prop') == '${task.projectprop}'
          jartask_manifest.getValue('Here') == testProjectDir.absolutePath.replace(File.separatorChar, '/' as char)
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
          jartask_jar.getEntry('commons-lang-2.6.jar')
          jartask_jar.close()

          bundletask_manifest.getValue('Bundle-SymbolicName') == "${testProject}_bundle"
          bundletask_manifest.getValue('Bundle-Version') == '1.1.0'
          bundletask_manifest.getValue('My-Header') == 'my-value'
          bundletask_manifest.getValue('Export-Package') =~ /doubler\.impl/
          !bundletask_manifest.getValue('X-SomeProperty')
          bundletask_manifest.getValue('Bundle-Name') == "test.bnd.gradle:${testProject}_bundle"
          bundletask_manifest.getValue('Project-Name') == "${testProject}"
          new File(bundletask_manifest.getValue('Project-Dir')).canonicalFile == testProjectDir
          new File(bundletask_manifest.getValue('Project-Output')).canonicalFile == testProjectBuildDir
          bundletask_manifest.getValue('Project-Sourcepath')
          bundletask_manifest.getValue('Project-Buildpath')
          bundletask_manifest.getValue('Bundle-ClassPath') =~ /commons-lang-2\.6\.jar/
          bundletask_manifest.getValue('Here') == testProjectDir.absolutePath.replace(File.separatorChar, '/' as char)
          !bundletask_jar.getEntry('doubler/Doubler.class')
          !bundletask_jar.getEntry('doubler/impl/DoublerImpl.class')
          bundletask_jar.getEntry('doubler/impl/DoublerImplTest.class')
          bundletask_jar.getEntry('OSGI-OPT/src/')
          !bundletask_jar.getEntry('foo.txt')
          !bundletask_jar.getEntry('bar.txt')
          bundletask_jar.getEntry('test.txt')
          bundletask_jar.getInputStream(bundletask_jar.getEntry('test.txt')).text =~ /This is a test resource/
          bundletask_jar.getEntry('commons-lang-2.6.jar')
          bundletask_jar.close()

        when:
          result = TestHelper.getGradleRunner()
            .withProjectDir(testProjectDir)
            .withArguments('--parallel', '--stacktrace', '--debug', 'build')
            .withPluginClasspath()
            .forwardOutput()
            .build()

        then:
          result.task(':bundle').outcome == UP_TO_DATE
          result.task(':jar').outcome == UP_TO_DATE
    }
}
