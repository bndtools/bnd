package bndtools.model.repo;

import org.bndtools.utils.repos.RepoUtils;

import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;

public class RepositoryBundleUtils {

	/**
	 * Converts a RepositoryBundle into a versionioned clause. There are 2
	 * cases: a) If the bundle comes from a workspace repository, then
	 * bsn;version=snapshot is returned. b) Otherwise only the bsn without a
	 * version is returned.
	 *
	 * @param bundle
	 * @return a version as described above
	 */
	public static VersionedClause convertRepoBundle(RepositoryBundle bundle) {
		Attrs attribs = new Attrs();
		if (RepoUtils.isWorkspaceRepo(bundle.getRepo())) {
			attribs.put(Constants.VERSION_ATTRIBUTE, Constants.VERSION_ATTR_SNAPSHOT);
		}
		return new VersionedClause(bundle.getBsn(), attribs);
	}

	/**
	 * Converts a RepositoryBundleVersion into a version or version range. e.g.
	 * version='snapshot' or version='[1.2.3,1.2.4)'. version=snapshot means the
	 * Version build in your workspace. We use version=snapshot instead of
	 * version=latest, because version=latest is the highest version found.
	 * Usually the workspace holds the latest Version. It is possible though,
	 * that you may have a newer Version of one of the projects in your
	 * workspace in one of your repositories.
	 *
	 * @param bundleVersion
	 * @param phase
	 * @return a version or version range.
	 */
	public static VersionedClause convertRepoBundleVersion(RepositoryBundleVersion bundleVersion,
		DependencyPhase phase) {
		Attrs attribs = new Attrs();
		if (RepoUtils.isWorkspaceRepo(bundleVersion.getParentBundle()
			.getRepo()))
			attribs.put(Constants.VERSION_ATTRIBUTE, Constants.VERSION_ATTR_SNAPSHOT);
		else {

			if (phase == DependencyPhase.Build) {
				// this gets executed when dragging a repo-version to
				// -buildpath in a bnd.bnd file
				// where only a Major.minor version should be inserted e.g.
				// version='1.24'

				String majorMinor = bundleVersion.getVersion()
					.getMajor() + "."
					+ bundleVersion.getVersion()
						.getMinor();
				attribs.put(Constants.VERSION_ATTRIBUTE, majorMinor);

			} else {
				// #5816
				// this code gets executed in the .bndrun editor
				// when adding a version (e.g by drag&drop) to the -runbundles
				// panel
				// create a range from the given version up to the next micro
				// as in
				// version='[1.2.3,1.2.4)'
				// instead of version='1.2.3' because the latter is actually an
				// open
				// range meaning "1.2.3 and everything above" (see
				// https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#d0e2221).
				// This could surprise developers, as version='1.2.3' looks more
				// like a exact version match.
				// Thus we create a more limited range from the given version
				// 1.2.3
				// `up to the next micro version
				// this behavior is similar to what the resolver is inserting
				// into
				// the run bundles list.
				String range = toVersionRangeUpToNextMicro(bundleVersion.getVersion()).toString();
				attribs.put(Constants.VERSION_ATTRIBUTE, range);
			}

		}
		return new VersionedClause(bundleVersion.getParentBundle()
			.getBsn(), attribs);
	}

	private static VersionRange toVersionRangeUpToNextMicro(Version l) {
		// bumpMicro
		Version h = new Version(l.getMajor(), l.getMinor(), l.getMicro() + 1);
		return new VersionRange(true, l.getWithoutQualifier(), h, false);
	}

}
