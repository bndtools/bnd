package bndtools.wizards.repo;

import static aQute.bnd.version.VersionRange.parseVersionRange;
import static bndtools.model.repo.DependencyPhase.Build;
import static bndtools.model.repo.DependencyPhase.Req;
import static bndtools.model.repo.DependencyPhase.Run;
import static bndtools.model.repo.RepositoryBundleUtils.convertRepoBundle;
import static bndtools.model.repo.RepositoryBundleUtils.convertRepoBundleVersion;
import static bndtools.model.repo.RepositoryBundleUtils.toVersionRangeUpToNextMajor;
import static bndtools.model.repo.RepositoryBundleUtils.toVersionRangeUpToNextMicro;
import static org.mockito.Mockito.mock;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
@ExtendWith(SoftAssertionsExtension.class)
public class RepoBundleVersionToRangeTest {

	@InjectSoftAssertions
	SoftAssertions softly;

	@Test
	public void testVersionToVersionRanges() {
		softly.assertThat(toVersionRangeUpToNextMicro(Version.valueOf("1.2.3")).toString())
			.isEqualTo("[1.2.3,1.2.4)");

		softly.assertThat(toVersionRangeUpToNextMajor(Version.valueOf("1.2.3")).toString())
			.isEqualTo("[1.2.3,2.0.0)");

		softly.assertThat(toVersionRangeUpToNextMicro(Version.valueOf("1.2.3")).toString())
			.isEqualTo(parseVersionRange("[1.2.3,1.2.4)").toString());

		softly.assertThat(toVersionRangeUpToNextMajor(Version.valueOf("1.2.3")).toString())
			.isEqualTo(parseVersionRange("[1.2.3,2.0.0)").toString());

	}

	@Test
	public void testVersionClauseConvertersWithNonWorkspaceRepo() {

		RepositoryPlugin nonWorkspaceRepo = mock(RepositoryPlugin.class);
		RepositoryBundle rb = new RepositoryBundle(nonWorkspaceRepo, "foo.bundle.bar");
		RepositoryBundleVersion rbv = new RepositoryBundleVersion(rb, Version.valueOf("1.2.3"));

		// test repobundle (root node in the repo browser... just the bsn,
		// without version)
		softly.assertThat(convertRepoBundle(rb, Req).toString())
			.isEqualTo("foo.bundle.bar");
		softly.assertThat(convertRepoBundle(rb, Run).toString())
			.isEqualTo("foo.bundle.bar");
		softly.assertThat(convertRepoBundle(rb, Build).toString())
			.isEqualTo("foo.bundle.bar");


		// adding to -runrequires
		softly.assertThat(convertRepoBundleVersion(rbv, Req).toString())
			.isEqualTo("foo.bundle.bar;version='[1.2.3,2.0.0)'");

		// adding to -runbundles
		softly.assertThat(convertRepoBundleVersion(rbv, Run).toString())
			.isEqualTo("foo.bundle.bar;version='[1.2.3,1.2.4)'");

		// adding to -buildpath
		softly.assertThat(convertRepoBundleVersion(rbv, Build).toString())
			.isEqualTo("foo.bundle.bar;version='1.2'");

	}

	@Test
	public void testVersionClauseConvertersWithWorkspaceRepo() {

		RepositoryPlugin wsrepo = new WorkspaceRepository(null);
		RepositoryBundle rb = new RepositoryBundle(wsrepo, "foo.bundle.bar");
		RepositoryBundleVersion rbv = new RepositoryBundleVersion(rb, Version.valueOf("1.2.3"));

		softly.assertThat(convertRepoBundle(rb, Req).toString())
			.isEqualTo("foo.bundle.bar");
		softly.assertThat(convertRepoBundle(rb, Run).toString())
			.isEqualTo("foo.bundle.bar;version=snapshot");
		softly.assertThat(convertRepoBundle(rb, Build).toString())
			.isEqualTo("foo.bundle.bar;version=snapshot");

		// adding to -runrequires
		softly.assertThat(convertRepoBundleVersion(rbv, Req).toString())
			.isEqualTo("foo.bundle.bar");

		// adding to -runbundles
		softly.assertThat(convertRepoBundleVersion(rbv, Run).toString())
			.isEqualTo("foo.bundle.bar;version=snapshot");

		// adding to -buildpath
		softly.assertThat(convertRepoBundleVersion(rbv, Build).toString())
			.isEqualTo("foo.bundle.bar;version=snapshot");

	}

}
