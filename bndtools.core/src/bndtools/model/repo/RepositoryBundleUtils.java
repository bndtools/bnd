package bndtools.model.repo;

import org.bndtools.utils.repos.RepoUtils;

import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;

public class RepositoryBundleUtils {
	private static final String VERSION_LATEST = "latest";

	public static VersionedClause convertRepoBundle(RepositoryBundle bundle) {
		Attrs attribs = new Attrs();
		if (RepoUtils.isWorkspaceRepo(bundle.getRepo())) {
			attribs.put(Constants.VERSION_ATTRIBUTE, VERSION_LATEST);
		}
		return new VersionedClause(bundle.getBsn(), attribs);
	}

	/**
	 * Converts a RepositoryBundleVersion into a version or version range. e.g.
	 * version='latest' or version='[1.2.3,2.0.0)'.
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
			attribs.put(Constants.VERSION_ATTRIBUTE, VERSION_LATEST);
		else {

			if (phase == DependencyPhase.Build) {
				// this gets executed when dragging a repo-version to
				// -buildpath in a bnd.bnd file
				// where only a Major.minor version should be inserted e.g.
				// version='1.24'

				String majorMinor = bundleVersion.getVersion() + "." + bundleVersion.getVersion()
					.getMinor();
				attribs.put(Constants.VERSION_ATTRIBUTE, majorMinor);

			} else {
				// #5816
				// this code gets executed in the .bndrun editor
				// when adding a version (e.g by drag&drop) to the -runbundles
				// panel
				// create a range from the given version up to the next major
				// as in
				// version='[1.2.3,2.0.0)'
				// instead of version='1.2.3' because the latter is actually an
				// open
				// range meaning "1.2.3 and everything above" (see
				// https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#d0e2221).
				// This could surprise developers, as version='1.2.3' looks more
				// like a exact version match.
				// Thus we create a more limited range from the given version
				// 1.2.3
				// `up to the next major version
				// this behavior is similar to what the resolver is inserting
				// into
				// the run bundles list.
				String range = toVersionRangeUpToNextMajor(bundleVersion.getVersion()).toString();
				attribs.put(Constants.VERSION_ATTRIBUTE, range);
			}

		}
		return new VersionedClause(bundleVersion.getParentBundle()
			.getBsn(), attribs);
	}

	private static VersionRange toVersionRangeUpToNextMajor(Version l) {
		Version h = l.bumpMajor();
		return new VersionRange(true, l, h, false);
	}

}
