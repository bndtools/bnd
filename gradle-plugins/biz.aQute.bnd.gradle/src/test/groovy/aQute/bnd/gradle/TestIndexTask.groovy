package aQute.bnd.gradle

import static aQute.bnd.gradle.TestHelper.formatTime
import static org.gradle.testkit.runner.TaskOutcome.*

import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.zip.GZIPInputStream

import groovy.xml.XmlSlurper
import spock.lang.Specification

class TestIndexTask extends Specification {

	File buildDir = new File("build")
	File testResources = new File(buildDir, "testresources")

	def "Simple Bnd Index Task Test"() {
		given:
		String testProject = "indexplugin1"
		File testProjectDir = new File(testResources, testProject).canonicalFile
		assert testProjectDir.isDirectory()
		File testProjectBuildDir = new File(testProjectDir, "build").canonicalFile

		when:
		def result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("--parallel", "--stacktrace", "--debug", "indexer", "indexer2")
				.withPluginClasspath()
				.forwardOutput()
				.build()

		then:
		result.task(":bundle").outcome == SUCCESS
		result.task(":jar").outcome == SUCCESS
		result.task(":indexer").outcome == SUCCESS
		result.task(":indexer2").outcome == SUCCESS

		testProjectBuildDir.isDirectory()

		File jartask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.0.0.jar")
		jartask_bundle.isFile()
		JarFile jartask_jar = new JarFile(jartask_bundle)
		Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()
		File bundletask_bundle = new File(testProjectBuildDir, "libs/${testProject}_bundle-1.1.0.jar")
		bundletask_bundle.isFile()
		JarFile bundletask_jar = new JarFile(bundletask_bundle)
		Attributes bundletask_manifest = bundletask_jar.getManifest().getMainAttributes()

		jartask_manifest.getValue("Bundle-SymbolicName") == "${testProject}"
		jartask_manifest.getValue("Bundle-Version") == "1.0.0"
		formatTime(jartask_jar.getEntry("doubler/Doubler.class")) == "2022-01-02T12:43:14Z"
		jartask_jar.close()

		bundletask_manifest.getValue("Bundle-SymbolicName") == "${testProject}_bundle"
		bundletask_manifest.getValue("Bundle-Version") == "1.1.0"
		formatTime(bundletask_jar.getEntry("doubler/impl/DoublerImplTest.class")) != "2022-01-02T12:43:14Z"
		bundletask_jar.close()

		when:
		File xmlfile = new File(testProjectBuildDir, "index.xml")
		assert xmlfile.isFile()
		def repository = new XmlSlurper().parse(xmlfile)

		then:
		repository.@name == testProject
		repository.lookupNamespace("") == "http://www.osgi.org/xmlns/repository/v1.0.0"
		repository.resource.size() == 2
		repository.resource[0]."*".find { node ->
			node.name() == "capability" && node.@namespace == "osgi.identity"
		}."*".find { node ->
			node.name() == "attribute" && node.@name == "osgi.identity"
		}.@value == testProject
		repository.resource[0]."*".find { node ->
			node.name() == "capability" && node.@namespace == "osgi.content"
		}."*".find { node ->
			node.name() == "attribute" && node.@name == "url"
		}.@value == "libs/${testProject}-1.0.0.jar"
		repository.resource[1]."*".find { node ->
			node.name() == "capability" && node.@namespace == "osgi.identity"
		}."*".find { node ->
			node.name() == "attribute" && node.@name == "osgi.identity"
		}.@value == "${testProject}_bundle"
		repository.resource[1]."*".find { node ->
			node.name() == "capability" && node.@namespace == "osgi.content"
		}."*".find { node ->
			node.name() == "attribute" && node.@name == "url"
		}.@value == "libs/${testProject}_bundle-1.1.0.jar"

		when:
		xmlfile = new File(testProjectBuildDir, "libs/index.xml.gz")

		then:
		!xmlfile.isFile()

		when:
		xmlfile = new File(testProjectBuildDir, "index/index2.xml")
		assert xmlfile.isFile()
		repository = new XmlSlurper().parse(xmlfile)

		then:
		repository.@name == "indexer2"
		repository.lookupNamespace("") == "http://www.osgi.org/xmlns/repository/v1.0.0"
		repository.resource.size() == 1
		repository.resource[0]."*".find { node ->
			node.name() == "capability" && node.@namespace == "osgi.identity"
		}."*".find { node ->
			node.name() == "attribute" && node.@name == "osgi.identity"
		}.@value == testProject
		repository.resource[0]."*".find { node ->
			node.name() == "capability" && node.@namespace == "osgi.content"
		}."*".find { node ->
			node.name() == "attribute" && node.@name == "url"
		}.@value == "bundles/${testProject}-1.0.0.jar"

		when:
		xmlfile = new File(testProjectBuildDir, "index/index2.xml.gz")
		assert xmlfile.isFile()
		repository = new XmlSlurper().parse(new GZIPInputStream(new FileInputStream(xmlfile)))

		then:
		repository.@name == "indexer2"
		repository.lookupNamespace("") == "http://www.osgi.org/xmlns/repository/v1.0.0"
		repository.resource.size() == 1
		repository.resource[0]."*".find { node ->
			node.name() == "capability" && node.@namespace == "osgi.identity"
		}."*".find { node ->
			node.name() == "attribute" && node.@name == "osgi.identity"
		}.@value == testProject
		repository.resource[0]."*".find { node ->
			node.name() == "capability" && node.@namespace == "osgi.content"
		}."*".find { node ->
			node.name() == "attribute" && node.@name == "url"
		}.@value == "bundles/${testProject}-1.0.0.jar"

		when:
		result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("--parallel", "--stacktrace", "--debug", "indexer", "indexer2")
				.withPluginClasspath()
				.forwardOutput()
				.build()

		then:
		result.task(":bundle").outcome == UP_TO_DATE
		result.task(":jar").outcome == UP_TO_DATE
		result.task(":indexer").outcome == UP_TO_DATE
		result.task(":indexer2").outcome == UP_TO_DATE
	}
}
