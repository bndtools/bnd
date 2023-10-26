package bndtools.wizards.repo;

import static aQute.bnd.version.VersionRange.parseVersionRange;
import static bndtools.model.repo.RepositoryBundleUtils.convertRepoBundle;
import static bndtools.model.repo.RepositoryBundleUtils.convertRepoBundleVersion;
import static bndtools.model.repo.RepositoryBundleUtils.toVersionRangeUpToNextMajor;
import static bndtools.model.repo.RepositoryBundleUtils.toVersionRangeUpToNextMicro;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import bndtools.model.repo.DependencyPhase;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;

/**
 * Tests behavior involved when dragging / dropping / adding repobundles to
 * -runbundles, -runrequires in the UI e.g. .bndrun editor. In these cases a
 * simple bsn or bsn+version needs to be converted to a range (or not). This
 * test tests the different combinations. e.g.
 * <ul>
 * <li>foo.bundle.bar v1.2.3 -> foo.bundle.bar;version=[1.2.3,1.2.4] (micro
 * version increment)</li>
 * <li>foo.bundle.bar v1.2.3 -> foo.bundle.bar;version=[1.2.3,2.0.0] (major
 * version increment)</li>
 * </ul>
 */
public class RepoBundleVersionToRangeTest {

	@Test
	public void testVersionToVersionRanges() {
		assertEquals(parseVersionRange("[1.2.3,1.2.4)").toString(),
			toVersionRangeUpToNextMicro(Version.valueOf("1.2.3")).toString());

		assertEquals(parseVersionRange("[1.2.3,2.0.0)").toString(),
			toVersionRangeUpToNextMajor(Version.valueOf("1.2.3")).toString());

	}

	@Test
	public void testVersionClauseConvertersWithNonWorkspaceRepo() {

		RepositoryPlugin nonWorkspaceRepo = mock(RepositoryPlugin.class);
		RepositoryBundle rb = new RepositoryBundle(nonWorkspaceRepo, "foo.bundle.bar");
		RepositoryBundleVersion rbv = new RepositoryBundleVersion(rb, Version.valueOf("1.2.3"));

		assertNull(convertRepoBundle(rb)
			.getVersionRange());

		// adding to -runrequires
		assertEquals("[1.2.3,2.0.0)", convertRepoBundleVersion(rbv, DependencyPhase.Req)
			.getVersionRange());

		// adding to -runbundles
		assertEquals("[1.2.3,1.2.4)", convertRepoBundleVersion(rbv, DependencyPhase.Run)
			.getVersionRange());

		assertEquals("1.2", convertRepoBundleVersion(rbv, DependencyPhase.Build)
			.getVersionRange());

	}

	@Test
	public void testVersionClauseConvertersWithWorkspaceRepo() {

		RepositoryPlugin wsrepo = new WorkspaceRepository(null);
		RepositoryBundle rb = new RepositoryBundle(wsrepo, "foo.bundle.bar");
		RepositoryBundleVersion rbv = new RepositoryBundleVersion(rb, Version.valueOf("1.2.3"));

		assertEquals("snapshot", convertRepoBundle(rb)
			.getVersionRange());

		// adding to -runrequires
		assertEquals("snapshot", convertRepoBundleVersion(rbv, DependencyPhase.Req)
			.getVersionRange());

		// adding to -runbundles
		assertEquals("snapshot", convertRepoBundleVersion(rbv, DependencyPhase.Run)
			.getVersionRange());

		assertEquals("snapshot", convertRepoBundleVersion(rbv, DependencyPhase.Build)
			.getVersionRange());

	}

}
