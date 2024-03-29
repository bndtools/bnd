package aQute.bnd.repository.maven.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.SortedSet;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;

public class MavenRepoTestHelper {

	/**
	 * This method is called by two maven repo tests to ensure that both repos
	 * behave the same and can get a resource by bns and gav in case both are
	 * available.
	 *
	 * @param repo
	 * @throws Exception
	 */
	public static void assertMavenReposGetViaBSNAndGAV(RepositoryPlugin repo) throws Exception {
		assertEquals(1, repo.list("org.apache.commons.cli")
			.size());
		System.out.println(repo.list("org.apache.commons.cli"));
		System.out.println(repo.versions("org.apache.commons.cli"));

		File f12maven = repo.get("commons-cli:commons-cli", new Version("1.2.0"), null);
		File f12osgi = repo.get("org.apache.commons.cli", new Version("1.2.0"), null);

		assertTrue(f12maven.exists());
		assertTrue(f12osgi.exists());

		assertEquals("commons-cli-1.2.jar", f12maven.getName());
		assertEquals(f12maven, f12osgi);

		// check if 1.2 instead of 1.2.0 works too
		File f12maven2DigitsVer = repo.get("commons-cli:commons-cli", new Version("1.2"), null);
		File f12osgi2DigitsVer = repo.get("org.apache.commons.cli", new Version("1.2"), null);

		assertTrue(f12maven2DigitsVer.exists());
		assertTrue(f12osgi2DigitsVer.exists());

		assertEquals("commons-cli-1.2.jar", f12maven2DigitsVer.getName());
		assertEquals(f12maven2DigitsVer, f12osgi2DigitsVer);

		// version should accept both too
		SortedSet<Version> versionsMaven = repo.versions("commons-cli:commons-cli");
		SortedSet<Version> versionsOsgi = repo.versions("org.apache.commons.cli");

		assertNotNull(versionsMaven);
		assertNotNull(versionsOsgi);
		assertTrue(versionsMaven.stream()
			.anyMatch(v -> "1.2.0".equals(v.toString())));
		assertTrue(versionsOsgi.stream()
			.anyMatch(v -> "1.2.0".equals(v.toString())));


	}

}
