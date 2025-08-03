package aQute.bnd.maven.lib.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Testcases to ensure certain dependency versions in different files are in
 * sync. The reason is that sometimes dependabot updates versions in pom.xml
 * automatically and when this slips through PR review unnoticed, it can cause
 * inconsistencies. The tests below are an attempt to make us at least aware of
 * it when it happens, so we can fix / revert dependabot PRs.
 */
public class ConfigVersionsTest {

	@Test
	public void testMavenTargetVersionInSync() throws Exception {
		Path bndPath = Paths.get("../cnf/ext/maven-plugin.bnd")
			.normalize();
		Path pomPath = Paths.get("../maven-plugins/bnd-plugin-parent/pom.xml")
			.normalize();

		System.out.println("Looking for BND at: " + bndPath.toAbsolutePath());
		System.out.println("Looking for POM at: " + pomPath.toAbsolutePath());

		assertTrue(Files.exists(bndPath), "BND file not found");
		assertTrue(Files.exists(pomPath), "POM file not found");

		// Read the full content of both files
		String bndFile = Files.readString(bndPath);
		String pomFile = Files.readString(pomPath);

		// Extract version from bnd file (e.g. maven.target.version: 3.3.9)
		Pattern bndPattern = Pattern.compile("maven\\.target\\.version:\\s*([\\d\\.]+)");
		Matcher bndMatcher = bndPattern.matcher(bndFile);
		assertTrue(bndMatcher.find(), "Version not found in BND file");
		String bndVersion = bndMatcher.group(1);

		// Extract version from POM file (e.g.
		// <maven.target.version>3.3.9</maven.target.version>)
		Pattern pomPattern = Pattern.compile("<maven\\.target\\.version>([\\d\\.]+)</maven\\.target\\.version>");
		Matcher pomMatcher = pomPattern.matcher(pomFile);
		assertTrue(pomMatcher.find(), "Version not found in POM file");
		String pomVersion = pomMatcher.group(1);

		// Compare
		// It would be better if we find a way to prevent dependabot from
		// updating pom.xml
		// but don't know how.
		assertEquals(bndVersion, pomVersion,
			"Versions do not match. Ensure cnf/ext/maven-plugin.bnd and /maven-plugins/bnd-plugin-parent/pom.xml use the same maven version (sometimes dependabot updates pom.xml incorrectly.)");
	}

	@Test
	public void testJunitVersionInSyncPomxml() throws Exception {
		Path junitbndPath = Paths.get("../cnf/ext/junit.bnd")
			.normalize();
		Path pomPath = Paths.get("../maven-plugins/bnd-plugin-parent/pom.xml")
			.normalize();

		System.out.println("Looking for BND at: " + junitbndPath.toAbsolutePath());
		System.out.println("Looking for POM at: " + pomPath.toAbsolutePath());

		assertTrue(Files.exists(junitbndPath), "BND file not found");
		assertTrue(Files.exists(pomPath), "POM file not found");

		// Read the full content of both files
		String bndFile = Files.readString(junitbndPath);
		String pomFile = Files.readString(pomPath);

		// Extract version from bnd file (e.g. junit.jupiter.version=5.12.2)
		Pattern bndPattern = Pattern.compile("junit\\.jupiter\\.version=\\s*([\\d\\.]+)");
		Matcher bndMatcher = bndPattern.matcher(bndFile);
		assertTrue(bndMatcher.find(), "Version not found in BND file");
		String bndVersion = bndMatcher.group(1);

		// Extract version from POM file (e.g.
		// <junit.jupiter.version>5.12.2</junit.jupiter.version>)
		Pattern pomPattern = Pattern.compile("<junit\\.jupiter\\.version>([\\d\\.]+)</junit\\.jupiter\\.version>");
		Matcher pomMatcher = pomPattern.matcher(pomFile);
		assertTrue(pomMatcher.find(), "Version not found in POM file");
		String pomVersion = pomMatcher.group(1);

		// Compare
		// It would be better if we find a way to prevent dependabot from
		// updating pom.xml
		// but don't know how.
		assertEquals(bndVersion, pomVersion,
			"Versions do not match. Ensure cnf/ext/junit.bnd and /maven-plugins/bnd-plugin-parent/pom.xml use the same junit.jupiter.version (sometimes dependabot updates pom.xml incorrectly.)");
	}

	@Test
	public void testJunitVersionInSyncGradle() throws Exception {
		Path junitbndPath = Paths.get("../cnf/ext/junit.bnd")
			.normalize();
		Path gradlePath = Paths.get("../gradle-plugins/biz.aQute.bnd.gradle/build.gradle.kts")
			.normalize();

		System.out.println("Looking for BND at: " + junitbndPath.toAbsolutePath());
		System.out.println("Looking for Gradle file at: " + gradlePath.toAbsolutePath());

		assertTrue(Files.exists(junitbndPath), "BND file not found");
		assertTrue(Files.exists(gradlePath), "Gradle file not found");

		// Read the full content of both files
		String bndFile = Files.readString(junitbndPath);
		String gradleFile = Files.readString(gradlePath);

		// Extract version from BND file (e.g. junit.jupiter.version=5.12.2)
		Pattern bndPattern = Pattern.compile("junit\\.jupiter\\.version=\\s*([\\d\\.]+)");
		Matcher bndMatcher = bndPattern.matcher(bndFile);
		assertTrue(bndMatcher.find(), "Version not found in BND file");
		String bndVersion = bndMatcher.group(1);

		// Extract version from Gradle file (e.g.
		// testImplementation(enforcedPlatform("org.junit:junit-bom:5.12.2"))
		Pattern gradlePattern = Pattern
			.compile("testImplementation\\(enforcedPlatform\\(\"org\\.junit:junit-bom:([\\d\\.]+)\"\\)\\)");
		Matcher gradleMatcher = gradlePattern.matcher(gradleFile);
		assertTrue(gradleMatcher.find(), "Version not found in Gradle file");
		String gradleVersion = gradleMatcher.group(1);

		// Compare
		assertEquals(bndVersion, gradleVersion,
			"Versions do not match. Ensure cnf/ext/junit.bnd and gradle-plugins/biz.aQute.bnd.gradle/build.gradle.kts use the same junit.jupiter.version. (sometimes dependabot updates pom.xml incorrectly.)");
	}
}
