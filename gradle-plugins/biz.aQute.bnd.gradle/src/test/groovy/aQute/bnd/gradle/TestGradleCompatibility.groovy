package aQute.bnd.gradle

import spock.lang.Specification
import spock.lang.Unroll

import java.util.jar.Attributes
import java.util.jar.JarFile

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class TestGradleCompatibility extends Specification {

	File buildDir = new File("build")
	File testResources = new File(buildDir, "testresources")
	String pluginClasspath

	def setup() {
		URL propertiesFile = getClass().getResource("/plugin-under-test-metadata.properties")
		assert propertiesFile != null
		def properties = new Properties()
		propertiesFile.withInputStream {
			properties.load(it)
		}
		pluginClasspath = properties."implementation-classpath"
		assert pluginClasspath != null
	}

	// -------------------------------------------------------------------------
	// Bnd Builder plugin – version compatibility
	// -------------------------------------------------------------------------

	@Unroll
	def "Bnd Builder plugin supports Gradle #gradleVersion"() {
		given:
		File testProjectDir = new File(testResources, "builderplugin1").canonicalFile
		assert testProjectDir.isDirectory()

		when:
		def result = TestHelper.getCompatibilityGradleRunner(gradleVersion)
			.withProjectDir(testProjectDir)
			.withArguments("help")
			.withPluginClasspath()
			.forwardOutput()
			.build()

		then:
		result.task(":help").outcome == SUCCESS

		where:
		gradleVersion << TestHelper.compatibilityGradleVersions()
	}

	@Unroll
	def "Bnd Builder plugin build succeeds on Gradle #gradleVersion"() {
		given:
		File testProjectDir = new File(testResources, "builderplugin1").canonicalFile
		assert testProjectDir.isDirectory()

		when:
		def result = TestHelper.getCompatibilityGradleRunner(gradleVersion)
			.withProjectDir(testProjectDir)
			.withArguments("--parallel", "--stacktrace", "build")
			.withPluginClasspath()
			.forwardOutput()
			.build()

		then:
		result.task(":jar").outcome == SUCCESS

		File jartask_bundle = new File(testProjectDir, "build/libs/builderplugin1-1.0.0.jar")
		jartask_bundle.isFile()

		JarFile jartask_jar = new JarFile(jartask_bundle)
		Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()
		jartask_manifest.getValue("Bundle-SymbolicName") == "builderplugin1"
		jartask_jar.close()

		where:
		gradleVersion << TestHelper.gradle9Versions()
	}

	@Unroll
	def "Bnd Builder plugin has no deprecation warnings on Gradle #gradleVersion"() {
		given:
		File testProjectDir = new File(testResources, "builderplugin1").canonicalFile
		assert testProjectDir.isDirectory()

		when:
		def result = TestHelper.getCompatibilityGradleRunner(gradleVersion)
			.withProjectDir(testProjectDir)
			.withArguments("--parallel", "--stacktrace", "--warning-mode=fail", "build")
			.withPluginClasspath()
			.forwardOutput()
			.build()

		then:
		result.task(":jar").outcome == SUCCESS

		where:
		gradleVersion << TestHelper.gradle9Versions()
	}

	// -------------------------------------------------------------------------
	// Bnd Workspace plugin – version compatibility
	// -------------------------------------------------------------------------

	@Unroll
	def "Bnd Workspace plugin supports Gradle #gradleVersion"() {
		given:
		File testProjectDir = new File(testResources, "workspaceplugin1")
		assert testProjectDir.isDirectory()

		when:
		def result = TestHelper.getCompatibilityGradleRunner(gradleVersion)
			.withProjectDir(testProjectDir)
			.withArguments("-Pbnd_plugin=${pluginClasspath}", "--parallel", "--stacktrace", ":tasks")
			.forwardOutput()
			.build()

		then:
		result.task(":tasks").outcome == SUCCESS

		where:
		gradleVersion << TestHelper.compatibilityGradleVersions()
	}

	@Unroll
	def "Bnd Workspace plugin build succeeds on Gradle #gradleVersion"() {
		given:
		File testProjectDir = new File(testResources, "workspaceplugin1")
		assert testProjectDir.isDirectory()

		when:
		def result = TestHelper.getCompatibilityGradleRunner(gradleVersion)
			.withProjectDir(testProjectDir)
			.withArguments("-Pbnd_plugin=${pluginClasspath}", "--parallel", "--stacktrace", "build", "release")
			.forwardOutput()
			.build()

		then:
		result.task(":test.simple:jar").outcome == SUCCESS
		result.task(":test.simple:build").outcome == SUCCESS
		result.task(":test.simple:release").outcome == SUCCESS

		File simple_bundle = new File(testProjectDir, "test.simple/generated/test.simple.jar")
		simple_bundle.isFile()

		JarFile simple_jar = new JarFile(simple_bundle)
		Attributes simple_manifest = simple_jar.getManifest().getMainAttributes()
		simple_manifest.getValue("Bundle-SymbolicName") == "test.simple"
		simple_manifest.getValue("Bundle-Version") =~ /0\.0\.0\./
		simple_jar.close()

		where:
		gradleVersion << TestHelper.gradle9Versions()
	}

	@Unroll
	def "Bnd Workspace plugin has no deprecation warnings on Gradle #gradleVersion"() {
		given:
		File testProjectDir = new File(testResources, "workspaceplugin1")
		assert testProjectDir.isDirectory()

		when:
		def result = TestHelper.getCompatibilityGradleRunner(gradleVersion)
			.withProjectDir(testProjectDir)
			.withArguments("-Pbnd_plugin=${pluginClasspath}", "--parallel", "--stacktrace", "--warning-mode=fail", ":tasks")
			.forwardOutput()
			.build()

		then:
		result.task(":tasks").outcome == SUCCESS

		where:
		gradleVersion << TestHelper.gradle9Versions()
	}
}
