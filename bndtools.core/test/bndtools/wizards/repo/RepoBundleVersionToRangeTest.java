package bndtools.wizards.repo;

import static aQute.bnd.version.VersionRange.parseVersionRange;
import static bndtools.model.repo.DependencyPhase.Build;
import static bndtools.model.repo.DependencyPhase.Req;
import static bndtools.model.repo.DependencyPhase.Run;
import static bndtools.model.repo.RepositoryBundleUtils.convertRepoBundle;
import static bndtools.model.repo.RepositoryBundleUtils.convertRepoBundleVersion;
import static bndtools.model.repo.RepositoryBundleUtils.toVersionRangeUpToNextMajor;
import static bndtools.model.repo.RepositoryBundleUtils.toVersionRangeUpToNextMicro;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
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

		// test repobundle (root node in the repo browser... just the bsn,
		// without version)
		assertEquals("foo.bundle.bar", convertRepoBundle(rb, Req).toString());
		assertEquals("foo.bundle.bar", convertRepoBundle(rb, Run).toString());
		assertEquals("foo.bundle.bar", convertRepoBundle(rb, Build).toString());


		// adding to -runrequires
		assertEquals("foo.bundle.bar;version='[1.2.3,2.0.0)'", convertRepoBundleVersion(rbv, Req).toString());

		// adding to -runbundles
		assertEquals("foo.bundle.bar;version='[1.2.3,1.2.4)'", convertRepoBundleVersion(rbv, Run).toString());

		// adding to -buildpath
		assertEquals("foo.bundle.bar;version='1.2'", convertRepoBundleVersion(rbv, Build).toString());

	}

	@Test
	public void testVersionClauseConvertersWithWorkspaceRepo() {

		RepositoryPlugin wsrepo = new WorkspaceRepository(null);
		RepositoryBundle rb = new RepositoryBundle(wsrepo, "foo.bundle.bar");
		RepositoryBundleVersion rbv = new RepositoryBundleVersion(rb, Version.valueOf("1.2.3"));

		assertEquals("foo.bundle.bar", convertRepoBundle(rb, Req).toString());
		assertEquals("foo.bundle.bar;version=snapshot", convertRepoBundle(rb, Run).toString());
		assertEquals("foo.bundle.bar;version=snapshot", convertRepoBundle(rb, Build).toString());

		// adding to -runrequires
		assertEquals("foo.bundle.bar", convertRepoBundleVersion(rbv, Req).toString());

		// adding to -runbundles
		assertEquals("foo.bundle.bar;version=snapshot", convertRepoBundleVersion(rbv, Run).toString());

		// adding to -buildpath
		assertEquals("foo.bundle.bar;version=snapshot", convertRepoBundleVersion(rbv, Build).toString());

	}

}
