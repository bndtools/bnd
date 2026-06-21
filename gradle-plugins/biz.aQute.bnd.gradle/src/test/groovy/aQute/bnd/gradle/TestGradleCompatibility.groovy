package aQute.bnd.gradle

import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class TestGradleCompatibility extends Specification {

	File buildDir = new File("build")
	File testResources = new File(buildDir, "testresources")

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
}
