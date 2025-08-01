package aQute.bnd.maven.lib.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class ConfigVersionsTest {

	@Test
	public void testJunitVersionInSync() throws Exception {
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
}
