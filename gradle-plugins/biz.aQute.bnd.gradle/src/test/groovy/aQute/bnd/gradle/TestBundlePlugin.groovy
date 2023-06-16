package aQute.bnd.gradle

import groovy.xml.XmlSlurper
import spock.lang.Specification

import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.regex.Pattern

import static aQute.bnd.gradle.TestHelper.formatTime
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class TestBundlePlugin extends Specification {

	File buildDir = new File("build")
	File testResources = new File(buildDir, "testresources")
	String bnd_version = System.getProperty("bnd_version")

	def "Simple Bnd Builder Plugin Test"() {
		given:
		String testProject = "builderplugin1"
		File testProjectDir = new File(testResources, testProject).canonicalFile
		assert testProjectDir.isDirectory()
		File testProjectBuildDir = new File(testProjectDir, "build").canonicalFile

		when:
		def result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("--parallel", "--stacktrace", "--debug", "build")
				.withPluginClasspath()
				.forwardOutput()
				.build()

		then:
		result.task(":bundle").outcome == SUCCESS
		result.task(":jar").outcome == SUCCESS

		testProjectBuildDir.isDirectory()

		File jartask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.0.0.jar")
		jartask_bundle.isFile()
		JarFile jartask_jar = new JarFile(jartask_bundle)
		Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()
		File bundletask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.1.0-bundle.jar")
		bundletask_bundle.isFile()
		JarFile bundletask_jar = new JarFile(bundletask_bundle)
		Attributes bundletask_manifest = bundletask_jar.getManifest().getMainAttributes()

		jartask_manifest.getValue("Bundle-SymbolicName") == "${testProject}"
		jartask_manifest.getValue("Bundle-Version") == "1.0.0"
		jartask_manifest.getValue("Implementation-Title") == "${testProject}"
		jartask_manifest.getValue("Implementation-Version") == "1.0.0"
		jartask_manifest.getValue("My-Header") == "my-value"
		jartask_manifest.getValue("Export-Package") =~ /doubler/
		jartask_manifest.getValue("X-SomeProperty") == "Included via -include in jar task manifest"
		jartask_manifest.getValue("Override") == "Override the jar task manifest"
		jartask_manifest.getValue("Bundle-Name") == "test.bnd.gradle:${testProject}"
		jartask_manifest.getValue("Project-Name") == "${testProject}"
		new File(jartask_manifest.getValue("Project-Dir")).canonicalFile == testProjectDir
		new File(jartask_manifest.getValue("Project-Output")).canonicalFile == new File(testProjectBuildDir, "classes/java/main").canonicalFile
		jartask_manifest.getValue("Project-Sourcepath")
		jartask_manifest.getValue("Project-Buildpath")
		jartask_manifest.getValue("Bundle-ClassPath") =~ /commons-lang-2\.6\.jar/
		jartask_manifest.getValue("Gradle-Project-Prop") == "prop.project"
		jartask_manifest.getValue("Gradle-Task-Prop") == "prop.task"
		jartask_manifest.getValue("Gradle-Task-Project-Prop") == "prop.project"
		jartask_manifest.getValue("Gradle-Missing-Prop") == "\${task.projectprop}"
		jartask_manifest.getValue("Here") == testProjectDir.absolutePath.replace(File.separatorChar, (char)'/')
		jartask_jar.getEntry("doubler/Doubler.class")
		formatTime(jartask_jar.getEntry("doubler/Doubler.class")) == "2022-01-02T12:43:14Z"
		jartask_jar.getEntry("doubler/packageinfo")
		jartask_jar.getEntry("doubler/impl/DoublerImpl.class")
		jartask_jar.getEntry("doubler/impl/packageinfo")
		!jartask_jar.getEntry("doubler/impl/DoublerImplTest.class")
		jartask_jar.getEntry("META-INF/services/foo.properties")
		jartask_jar.getInputStream(jartask_jar.getEntry("META-INF/services/foo.properties")).text =~ /key=value/
		jartask_jar.getEntry("OSGI-OPT/src/")
		jartask_jar.getEntry("OSGI-OPT/src/doubler/packageinfo")
		jartask_jar.getEntry("OSGI-OPT/src/doubler/impl/packageinfo")
		jartask_jar.getInputStream(jartask_jar.getEntry("OSGI-OPT/src/doubler/packageinfo")).text =~ /version 1\.0/
		jartask_jar.getInputStream(jartask_jar.getEntry("OSGI-OPT/src/doubler/impl/packageinfo")).text =~ /version 1\.2/
		jartask_jar.getEntry("foo.txt")
		jartask_jar.getInputStream(jartask_jar.getEntry("foo.txt")).text =~ /Hi!/
		jartask_jar.getEntry("bar.txt")
		jartask_jar.getInputStream(jartask_jar.getEntry("bar.txt")).text =~ /Some more TEXT/
		!jartask_jar.getEntry("test.txt")
		jartask_jar.getEntry("commons-lang-2.6.jar")
		jartask_jar.close()

		bundletask_manifest.getValue("Bundle-SymbolicName") == "${testProject}-bundle"
		bundletask_manifest.getValue("Bundle-Version") == "1.1.0"
		bundletask_manifest.getValue("My-Header") == "my-value"
		bundletask_manifest.getValue("Export-Package") =~ /doubler\.impl/
		!bundletask_manifest.getValue("X-SomeProperty")
		bundletask_manifest.getValue("Bundle-Name") == "test.bnd.gradle:${testProject}-bundle"
		bundletask_manifest.getValue("Project-Name") == "${testProject}"
		new File(bundletask_manifest.getValue("Project-Dir")).canonicalFile == testProjectDir
		new File(bundletask_manifest.getValue("Project-Output")).canonicalFile == new File(testProjectBuildDir, "classes/java/test").canonicalFile
		bundletask_manifest.getValue("Project-Sourcepath")
		bundletask_manifest.getValue("Project-Buildpath")
		bundletask_manifest.getValue("Bundle-ClassPath") =~ /commons-lang-2\.6\.jar/
		bundletask_manifest.getValue("Here") == testProjectDir.absolutePath.replace(File.separatorChar, (char)'/')
		!bundletask_jar.getEntry("doubler/Doubler.class")
		!bundletask_jar.getEntry("doubler/impl/DoublerImpl.class")
		bundletask_jar.getEntry("doubler/impl/DoublerImplTest.class")
		formatTime(bundletask_jar.getEntry("doubler/impl/DoublerImplTest.class")) != "2022-01-02T12:43:14Z"
		bundletask_jar.getEntry("OSGI-OPT/src/")
		!bundletask_jar.getEntry("foo.txt")
		!bundletask_jar.getEntry("bar.txt")
		bundletask_jar.getEntry("test.txt")
		bundletask_jar.getInputStream(bundletask_jar.getEntry("test.txt")).text =~ /This is a test resource/
		bundletask_jar.getEntry("commons-lang-2.6.jar")
		bundletask_jar.close()

		result.output =~ Pattern.quote("### jar Bundle-SymbolicName: builderplugin1")
		result.output =~ Pattern.quote("### bundle Bundle-SymbolicName: builderplugin1-bundle")

		when:
		result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("--parallel", "--stacktrace", "--debug", "build")
				.withPluginClasspath()
				.forwardOutput()
				.build()

		then:
		result.task(":bundle").outcome == UP_TO_DATE
		result.task(":jar").outcome == UP_TO_DATE
	}

	def "Test Bnd instruction via Provider"() {
		given:
		String testProject = "builderplugin2"
		File testProjectDir = new File(testResources, testProject).canonicalFile
		assert testProjectDir.isDirectory()
		File testProjectBuildDir = new File(testProjectDir, "build").canonicalFile

		when:
		def result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("--parallel", "--stacktrace", "--debug", "build")
				.withPluginClasspath()
				.forwardOutput()
				.build()

		then:
		result.task(":bundle").outcome == SUCCESS
		result.task(":jar").outcome == SUCCESS

		File jartask_result = new File(testProjectBuildDir, "libs/${testProject}.jar")
		jartask_result.isFile()
		JarFile jartask_jar = new JarFile(jartask_result)
		Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()

		File bundletask_bundle = new File(testProjectBuildDir, "libs/${testProject}-bundle.jar")
		bundletask_bundle.isFile()
		JarFile bundletask_jar = new JarFile(bundletask_bundle)
		Attributes bundletask_manifest = bundletask_jar.getManifest().getMainAttributes()

		jartask_manifest.getValue("XX-Signed") == "true"
		bundletask_manifest.getValue("Bundle-SymbolicName") == "${testProject}-bundle"
		bundletask_manifest.getValue("Bundle-Version") == "0.0.0"
		bundletask_manifest.getValue("XX-Signed") == "true"
		bundletask_manifest.getValue("YY-Sealed") == "true"
		bundletask_manifest.getValue("ZZ-Delivered") == "true"
		bundletask_manifest.getValue("Bundle-Name") == "test.bnd.gradle:${testProject}-bundle"
	}

	def "Kotlin Bnd Builder Plugin Test"() {
		given:
		String testProject = "builderplugin3"
		File testProjectDir = new File(testResources, testProject).canonicalFile
		assert testProjectDir.isDirectory()
		File testProjectBuildDir = new File(testProjectDir, "build").canonicalFile

		when:
		def result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("--parallel", "--stacktrace", "--debug", "build")
				.withPluginClasspath()
				.forwardOutput()
				.build()

		then:
		result.task(":bundle").outcome == SUCCESS
		result.task(":jar").outcome == SUCCESS

		testProjectBuildDir.isDirectory()

		File jartask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.0.0.jar")
		jartask_bundle.isFile()
		JarFile jartask_jar = new JarFile(jartask_bundle)
		Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()
		File bundletask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.1.0-bundle.jar")
		bundletask_bundle.isFile()
		JarFile bundletask_jar = new JarFile(bundletask_bundle)
		Attributes bundletask_manifest = bundletask_jar.getManifest().getMainAttributes()

		jartask_manifest.getValue("Bundle-SymbolicName") == "${testProject}"
		jartask_manifest.getValue("Bundle-Version") == "1.0.0"
		jartask_manifest.getValue("Implementation-Title") == "${testProject}"
		jartask_manifest.getValue("Implementation-Version") == "1.0.0"
		jartask_manifest.getValue("My-Header") == "my-value"
		jartask_manifest.getValue("Export-Package") =~ /doubler/
		jartask_manifest.getValue("X-SomeProperty") == "Included via -include in jar task manifest"
		jartask_manifest.getValue("Override") == "Override the jar task manifest"
		jartask_manifest.getValue("Bundle-Name") == "test.bnd.gradle:${testProject}"
		jartask_manifest.getValue("Project-Name") == "${testProject}"
		new File(jartask_manifest.getValue("Project-Dir")).canonicalFile == testProjectDir
		new File(jartask_manifest.getValue("Project-Output")).canonicalFile == new File(testProjectBuildDir, "classes/java/main").canonicalFile
		jartask_manifest.getValue("Project-Sourcepath")
		jartask_manifest.getValue("Project-Buildpath")
		jartask_manifest.getValue("Bundle-ClassPath") =~ /commons-lang-2\.6\.jar/
		jartask_manifest.getValue("Gradle-Project-Prop") == "prop.project"
		jartask_manifest.getValue("Gradle-Task-Prop") == "prop.task"
		jartask_manifest.getValue("Gradle-Task-Project-Prop") == "prop.project"
		jartask_manifest.getValue("Gradle-Missing-Prop") == "\${task.projectprop}"
		jartask_manifest.getValue("Here") == testProjectDir.absolutePath.replace(File.separatorChar, (char)'/')
		jartask_jar.getEntry("doubler/Doubler.class")
		jartask_jar.getEntry("doubler/packageinfo")
		jartask_jar.getEntry("doubler/impl/DoublerImpl.class")
		jartask_jar.getEntry("doubler/impl/packageinfo")
		!jartask_jar.getEntry("doubler/impl/DoublerImplTest.class")
		jartask_jar.getEntry("META-INF/services/foo.properties")
		jartask_jar.getInputStream(jartask_jar.getEntry("META-INF/services/foo.properties")).text =~ /key=value/
		jartask_jar.getEntry("OSGI-OPT/src/")
		jartask_jar.getEntry("OSGI-OPT/src/doubler/packageinfo")
		jartask_jar.getEntry("OSGI-OPT/src/doubler/impl/packageinfo")
		jartask_jar.getInputStream(jartask_jar.getEntry("OSGI-OPT/src/doubler/packageinfo")).text =~ /version 1\.0/
		jartask_jar.getInputStream(jartask_jar.getEntry("OSGI-OPT/src/doubler/impl/packageinfo")).text =~ /version 1\.2/
		jartask_jar.getEntry("foo.txt")
		jartask_jar.getInputStream(jartask_jar.getEntry("foo.txt")).text =~ /Hi!/
		jartask_jar.getEntry("bar.txt")
		jartask_jar.getInputStream(jartask_jar.getEntry("bar.txt")).text =~ /Some more TEXT/
		!jartask_jar.getEntry("test.txt")
		jartask_jar.getEntry("commons-lang-2.6.jar")
		jartask_jar.close()

		bundletask_manifest.getValue("Bundle-SymbolicName") == "${testProject}-bundle"
		bundletask_manifest.getValue("Bundle-Version") == "1.1.0"
		bundletask_manifest.getValue("My-Header") == "my-value"
		bundletask_manifest.getValue("Export-Package") =~ /doubler\.impl/
		!bundletask_manifest.getValue("X-SomeProperty")
		bundletask_manifest.getValue("Bundle-Name") == "test.bnd.gradle:${testProject}-bundle"
		bundletask_manifest.getValue("Project-Name") == "${testProject}"
		new File(bundletask_manifest.getValue("Project-Dir")).canonicalFile == testProjectDir
		new File(bundletask_manifest.getValue("Project-Output")).canonicalFile == new File(testProjectBuildDir, "classes/java/test").canonicalFile
		bundletask_manifest.getValue("Project-Sourcepath")
		bundletask_manifest.getValue("Project-Buildpath")
		bundletask_manifest.getValue("Bundle-ClassPath") =~ /commons-lang-2\.6\.jar/
		bundletask_manifest.getValue("Here") == testProjectDir.absolutePath.replace(File.separatorChar, (char)'/')
		!bundletask_jar.getEntry("doubler/Doubler.class")
		!bundletask_jar.getEntry("doubler/impl/DoublerImpl.class")
		bundletask_jar.getEntry("doubler/impl/DoublerImplTest.class")
		bundletask_jar.getEntry("OSGI-OPT/src/")
		!bundletask_jar.getEntry("foo.txt")
		!bundletask_jar.getEntry("bar.txt")
		bundletask_jar.getEntry("test.txt")
		bundletask_jar.getInputStream(bundletask_jar.getEntry("test.txt")).text =~ /This is a test resource/
		bundletask_jar.getEntry("commons-lang-2.6.jar")
		bundletask_jar.close()

		when:
		result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("--parallel", "--stacktrace", "--debug", "build")
				.withPluginClasspath()
				.forwardOutput()
				.build()

		then:
		result.task(":bundle").outcome == UP_TO_DATE
		result.task(":jar").outcome == UP_TO_DATE
	}

	def "Configuration Cache Support Test"() {
		given:
		String testProject = "builderplugin4"
		File testProjectDir = new File(testResources, testProject).canonicalFile
		assert testProjectDir.isDirectory()
		File testProjectBuildDir = new File(testProjectDir, "build").canonicalFile

		when:
		def result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("-Pbnd_version=${bnd_version}", "--parallel", "--stacktrace", "--debug", "--configuration-cache", "build")
				.withPluginClasspath()
				.forwardOutput()
				.build()

		then:
		result.task(":test").outcome == SUCCESS
		result.task(":bundle").outcome == SUCCESS
		result.task(":jar").outcome == SUCCESS
		result.task(":testOSGi").outcome == SUCCESS

		testProjectBuildDir.isDirectory()

		File jartask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.0.0.jar")
		jartask_bundle.isFile()
		JarFile jartask_jar = new JarFile(jartask_bundle)
		Attributes jartask_manifest = jartask_jar.getManifest().getMainAttributes()
		File bundletask_bundle = new File(testProjectBuildDir, "libs/${testProject}-1.1.0-bundle.jar")
		bundletask_bundle.isFile()
		JarFile bundletask_jar = new JarFile(bundletask_bundle)
		Attributes bundletask_manifest = bundletask_jar.getManifest().getMainAttributes()

		jartask_manifest.getValue("Bundle-SymbolicName") == "${testProject}"
		jartask_manifest.getValue("Bundle-Version") == "1.0.0"
		jartask_manifest.getValue("Implementation-Title") == "${testProject}"
		jartask_manifest.getValue("Implementation-Version") == "1.0.0"
		jartask_manifest.getValue("My-Header") == "my-value"
		jartask_manifest.getValue("Export-Package") =~ /doubler/
		jartask_manifest.getValue("X-SomeProperty") == "Included via -include in jar task manifest"
		jartask_manifest.getValue("Override") == "Override the jar task manifest"
		jartask_manifest.getValue("Bundle-Name") == "test.bnd.gradle:${testProject}"
		jartask_manifest.getValue("Project-Name") == "${testProject}"
		new File(jartask_manifest.getValue("Project-Dir")).canonicalFile == testProjectDir
		new File(jartask_manifest.getValue("Project-Output")).canonicalFile == new File(testProjectBuildDir, "classes/java/main").canonicalFile
		jartask_manifest.getValue("Project-Sourcepath")
		jartask_manifest.getValue("Project-Buildpath")
		jartask_manifest.getValue("Gradle-Project-Prop") == "prop.project"
		jartask_manifest.getValue("Gradle-Task-Prop") == "prop.task"
		jartask_manifest.getValue("Gradle-Task-Project-Prop") == "prop.project"
		jartask_manifest.getValue("Gradle-Missing-Prop") == "\${task.projectprop}"
		jartask_manifest.getValue("Here") == testProjectDir.absolutePath.replace(File.separatorChar, (char)'/')
		jartask_manifest.getValue("Test-Cases") == "doubler.impl.DoublerImplOSGiTest"
		jartask_jar.getEntry("doubler/Doubler.class")
		jartask_jar.getEntry("doubler/packageinfo")
		jartask_jar.getEntry("doubler/impl/DoublerImpl.class")
		jartask_jar.getEntry("doubler/impl/packageinfo")
		jartask_jar.getEntry("doubler/impl/DoublerImplOSGiTest.class")
		!jartask_jar.getEntry("doubler/impl/DoublerImplUnitTest.class")
		jartask_jar.getEntry("META-INF/services/foo.properties")
		jartask_jar.getInputStream(jartask_jar.getEntry("META-INF/services/foo.properties")).text =~ /key=value/
		jartask_jar.getEntry("OSGI-OPT/src/")
		jartask_jar.getEntry("OSGI-OPT/src/doubler/packageinfo")
		jartask_jar.getEntry("OSGI-OPT/src/doubler/impl/packageinfo")
		jartask_jar.getInputStream(jartask_jar.getEntry("OSGI-OPT/src/doubler/packageinfo")).text =~ /version 1\.0/
		jartask_jar.getInputStream(jartask_jar.getEntry("OSGI-OPT/src/doubler/impl/packageinfo")).text =~ /version 1\.2/
		jartask_jar.getEntry("foo.txt")
		jartask_jar.getInputStream(jartask_jar.getEntry("foo.txt")).text =~ /Hi!/
		jartask_jar.getEntry("bar.txt")
		jartask_jar.getInputStream(jartask_jar.getEntry("bar.txt")).text =~ /Some more TEXT/
		!jartask_jar.getEntry("test.txt")
		jartask_jar.close()

		bundletask_manifest.getValue("Bundle-SymbolicName") == "${testProject}-bundle"
		bundletask_manifest.getValue("Bundle-Version") == "1.1.0"
		bundletask_manifest.getValue("My-Header") == "my-value"
		bundletask_manifest.getValue("Export-Package") =~ /doubler\.impl/
		!bundletask_manifest.getValue("X-SomeProperty")
		bundletask_manifest.getValue("Bundle-Name") == "test.bnd.gradle:${testProject}-overridden"
		bundletask_manifest.getValue("Project-Name") == "${testProject}"
		new File(bundletask_manifest.getValue("Project-Dir")).canonicalFile == testProjectDir
		new File(bundletask_manifest.getValue("Project-Output")).canonicalFile == new File(testProjectBuildDir, "classes/java/test").canonicalFile
		bundletask_manifest.getValue("Project-Sourcepath")
		bundletask_manifest.getValue("Project-Buildpath")
		bundletask_manifest.getValue("Here") == testProjectDir.absolutePath.replace(File.separatorChar, (char)'/')
		!bundletask_jar.getEntry("doubler/Doubler.class")
		!bundletask_jar.getEntry("doubler/impl/DoublerImpl.class")
		bundletask_jar.getEntry("doubler/impl/DoublerImplUnitTest.class")
		bundletask_jar.getEntry("OSGI-OPT/src/")
		!bundletask_jar.getEntry("foo.txt")
		!bundletask_jar.getEntry("bar.txt")
		bundletask_jar.getEntry("test.txt")
		bundletask_jar.getInputStream(bundletask_jar.getEntry("test.txt")).text =~ /This is a test resource/
		bundletask_jar.close()

		when:
		File testxml = new File(testProjectBuildDir, "test-results/test/TEST-doubler.impl.DoublerImplUnitTest.xml")
		assert testxml.isFile()
		def testsuite = new XmlSlurper().parse(testxml)

		then:
		testsuite.@tests == 1
		testsuite.@errors == 0
		testsuite.@failures == 0
		testsuite.testcase.size() == 1
		testsuite.testcase[0].@name == "testIt"
		testsuite.testcase[0].@classname == "doubler.impl.DoublerImplUnitTest"


		when:
		testxml = new File(testProjectBuildDir, "test-results/testOSGi/TEST-${testProject}-1.0.0.xml")
		assert testxml.isFile()
		testsuite = new XmlSlurper().parse(testxml)

		then:
		testsuite.@tests == 1
		testsuite.@errors == 0
		testsuite.@failures == 0
		testsuite.testcase.size() == 1
		testsuite.testcase[0].@name == "testIt"
		testsuite.testcase[0].@classname == "doubler.impl.DoublerImplOSGiTest"

		when:
		result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("-Pbnd_version=${bnd_version}", "--parallel", "--stacktrace", "--debug", "--configuration-cache", "build")
				.withPluginClasspath()
				.forwardOutput()
				.build()

		then:
		result.task(":test").outcome == UP_TO_DATE
		result.task(":bundle").outcome == UP_TO_DATE
		result.task(":jar").outcome == UP_TO_DATE
		result.task(":testOSGi").outcome == UP_TO_DATE
	}

	def "Bundle Extension added to Jar Task with Local Project Dependency receives jar file (Gradle = #gradleVersion)"() {
		given:
		String testProject = "builderplugin5"
		File testProjectDir = new File(testResources, testProject).canonicalFile
		assert testProjectDir.isDirectory()

		when:
		def result = TestHelper.getGradleRunner()
				.withProjectDir(testProjectDir)
				.withArguments("--parallel", "--stacktrace", "resolve")
				.withPluginClasspath()
				.withDebug(true)
				.withGradleVersion(gradleVersion)
				.forwardOutput()
				.build()

		then:
		result.task(":jar") == null // Jar tasks never run
		result.task(":resolve").outcome == SUCCESS

		where:
		gradleVersion << ["8.1", "8.2-rc-2"]
	}
}
