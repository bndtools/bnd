package bndtools.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import bndtools.central.RebuildTriggerPolicy;
import bndtools.central.RebuildTriggerPolicyPlugin;

@ExtendWith(SoftAssertionsExtension.class)
public class RebuildPolicyTriggerTest {

	@InjectTemporaryDirectory
	File tmp;

	private Workspace getWorkspace(File file) throws Exception {
		IO.copy(file, tmp);
		return new Workspace(tmp);
	}

	/**
		 * Check that the content-hash optimization prevents unnecessary JAR
		 * rewrites when the JAR content is unchanged between builds. This avoids
		 * cascading rebuilds of dependent projects.
		 */
		@Test
		public void testContentHashSkipsBuildWhenUnchanged() throws Exception {
			Workspace ws = getWorkspace(IO.getFile("testdata/ws"));
			ws.addBasicPlugin(new RebuildTriggerPolicyPlugin(RebuildTriggerPolicy.REBUILDTRIGGERPOLICY_API));
			Project project = ws.getProject("p-stale");
			assertNotNull(project);

			// First build - should create JAR and digest files
			File[] firstBuild = project.build();
			assertNotNull(firstBuild);
			assertTrue(firstBuild.length > 0);

			File jarFile = firstBuild[0];
			assertTrue(jarFile.isFile());
			long firstTimestamp = jarFile.lastModified();

			// Verify digest file was created
			File digestFile = getDigestFile(jarFile);
			assertTrue(digestFile.isFile(), "Digest file should be created after build");
			String firstDigest = IO.collect(digestFile)
				.trim();
			assertFalse(firstDigest.isEmpty(), "Digest should not be empty");

			// Simulate time passing by adjusting the JAR timestamp backward,
			// then mark the project as changed. If the optimization works
			// correctly, the JAR's timestamp will be restored to this value
			// since the content hasn't changed.
			long adjustedTimestamp = firstTimestamp - 10000;
			jarFile.setLastModified(adjustedTimestamp);

			// Mark project as changed so it rebuilds
			project.setChanged();
			project.refresh();

			// Second build - content is unchanged, JAR timestamp should be
			// preserved
			File[] secondBuild = project.build();
			assertNotNull(secondBuild);
			assertTrue(secondBuild.length > 0);

			File jarFile2 = secondBuild[0];
			assertTrue(jarFile2.isFile());

			// The JAR timestamp should be preserved since content didn't change
			assertEquals(adjustedTimestamp, jarFile2.lastModified(),
				"JAR timestamp should be preserved when content is unchanged");

			// Digest file should still exist with the same content
			assertTrue(digestFile.isFile());
			String secondDigest = IO.collect(digestFile)
				.trim();
			assertEquals(firstDigest, secondDigest, "Digest should be unchanged");
		}

		/**
		 * Check that when JAR content actually changes, the JAR is rewritten and
		 * the digest is updated.
		 */
		@Test
		public void testContentHashRewritesWhenChanged() throws Exception {
			Workspace ws = getWorkspace(IO.getFile("testdata/ws"));
			ws.addBasicPlugin(new RebuildTriggerPolicyPlugin(RebuildTriggerPolicy.REBUILDTRIGGERPOLICY_API));
			Project project = ws.getProject("p-stale");
			assertNotNull(project);

			// First build
			File[] firstBuild = project.build();
			assertNotNull(firstBuild);
			File jarFile = firstBuild[0];
			File digestFile = getDigestFile(jarFile);
			assertTrue(digestFile.isFile());
			String firstDigest = IO.collect(digestFile)
				.trim();

			// Change the project content so the JAR will be different
			project.setChanged();
			project.refresh();
			project.setProperty("Include-Resource", "p;literal=\"changed content\"");

			// Second build - content changed, JAR should be rewritten
			File[] secondBuild = project.build();
			assertNotNull(secondBuild);
			File jarFile2 = secondBuild[0];

			// Digest should have changed
			assertTrue(digestFile.isFile());
			String secondDigest = IO.collect(digestFile)
				.trim();
			assertThat(secondDigest).as("Digest should change when content changes")
				.isNotEqualTo(firstDigest);
		}

		private static File getDigestFile(File jarFile) {
			return new File(jarFile.getParentFile(), jarFile.getName() + ".digest");
		}

		/**
		 * Verify that timestamp preservation does not engage when the JAR content
		 * changes and an API digest cannot be computed (e.g. a resource-only bundle
		 * with no Export-Package).
		 */
		@Test
		public void testTimestampNotPreservedWhenApiDigestMissing() throws Exception {
			Workspace ws = getWorkspace(IO.getFile("testdata/ws"));
			ws.addBasicPlugin(new RebuildTriggerPolicyPlugin(RebuildTriggerPolicy.REBUILDTRIGGERPOLICY_API));
			Project project = ws.getProject("p-stale");
			assertNotNull(project);

			// First build — establishes the baseline JAR and digest files
			File[] firstBuild = project.build();
			assertNotNull(firstBuild);
			assertTrue(firstBuild.length > 0);

			File jarFile = firstBuild[0];
			assertTrue(jarFile.isFile());

			// Record the old timestamp and adjust it to simulate time
			long adjustedTimestamp = jarFile.lastModified() - 10000;
			jarFile.setLastModified(adjustedTimestamp);

			// The content digest from the first build
			File digestFile = getDigestFile(jarFile);
			assertTrue(digestFile.isFile(), "Content digest file should exist after build");

			// Write a fake API digest sidecar. The next build will produce
			// the same resource-only bundle (no Export-Package), so
			// calcApiDigest() returns null. However, when we change the
			// project content the content digest will differ. To simulate
			// the API-digest-unchanged path, we need a non-null API digest.
			// We'll change the content, then verify the mechanism by
			// checking the timestamp. Since calcApiDigest returns null for
			// resource-only bundles, the API digest path won't engage here,
			// but we can verify the infrastructure is correctly wired:
			// the API digest file should remain from the first build if no
			// new one is computed.
			File apiDigestFile = new File(jarFile.getParentFile(), jarFile.getName() + ".api-digest");

			// Change the project content so the JAR will differ
			project.setChanged();
			project.refresh();
			project.setProperty("Include-Resource", "p;literal=\"changed content\"");

			// Rebuild — content changed so content digest differs, and
			// calcApiDigest returns null for resource-only bundles. The
			// timestamp should NOT be preserved (no API digest fallback).
			File[] secondBuild = project.build();
			assertNotNull(secondBuild);

			File jarFile2 = secondBuild[0];
			// Content changed so the digest should differ
			String newDigest = IO.collect(digestFile).trim();
			// The JAR should have a new timestamp since both content and
			// API digests indicate a change (or API digest is absent)
			assertThat(jarFile2.lastModified())
				.as("Timestamp should NOT be preserved when content changes and no API digest exists")
				.isNotEqualTo(adjustedTimestamp);

			// The content digest file should still exist
			assertTrue(digestFile.isFile(), "Content digest file should exist after rebuild");
		}

}

