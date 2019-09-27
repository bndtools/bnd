package bndtools.model.repo;

import org.bndtools.utils.repos.RepoUtils;

import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;

public class RepositoryBundleUtils {
	private static final String VERSION_LATEST = "latest";

	public static VersionedClause convertRepoBundle(RepositoryBundle bundle) {
		Attrs attribs = new Attrs();
		if (RepoUtils.isWorkspaceRepo(bundle.getRepo())) {
			attribs.put(Constants.VERSION_ATTRIBUTE, VERSION_LATEST);
		}
		return new VersionedClause(bundle.getBsn(), attribs);
	}

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
			if (phase != DependencyPhase.Build)
				builder.append('.')
					.append(bundleVersion.getVersion()
						.getMicro());

			attribs.put(Constants.VERSION_ATTRIBUTE, builder.toString());
		}
		return new VersionedClause(bundleVersion.getParentBundle()
			.getBsn(), attribs);
	}
}
