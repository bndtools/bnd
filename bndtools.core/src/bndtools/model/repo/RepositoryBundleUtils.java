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

			StringBuilder builder = new StringBuilder();

			builder.append(bundleVersion.getVersion()
				.getMajor());
			builder.append('.')
				.append(bundleVersion.getVersion()
					.getMinor());

			// TODO why is this check?
			if (phase != DependencyPhase.Build)
				builder.append('.')
					.append(bundleVersion.getVersion()
						.getMicro());


			// #5816 create a range from the given version up to the next major
			// as in
			// version='[1.2.3,2.0.0)'
			// instead of version='1.2.3' because the latter is actually an open
			// range meaning "1.2.3 and everything above" (see
			// https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#d0e2221).
			// This could surprise developers, as version='1.2.3' looks more
			// like a exact version match.
			// Thus we create a more limited range from the given version 1.2.3
			// `up to the next major version
			// this behavior is similar to what the resolver is inserting into
			// the run bundles list.
			Version l = new Version(builder.toString());
			Version h = l.bumpMajor();
			VersionRange vr = new VersionRange(true, l, h, false);
			String range = vr.toString();

			attribs.put(Constants.VERSION_ATTRIBUTE, range);
		}
		return new VersionedClause(bundleVersion.getParentBundle()
			.getBsn(), attribs);
	}
}
