package aQute.bnd.workspace.extension;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.resource.Capability;

import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.resource.FilterBuilder;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.result.Result;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;

/**
 * A workspace extension can be applied on the workspace levell
 *
 * <pre>
 * bnd.workspace.extension      name of the workspace extension
 * version                      the version of the extension
 * include                      declares the bnd file to be included
 * description                  A useful description for the extension
 * </pre>
 */
public interface WorkspaceExtensionNamespace {


	/**
	 * The name of the external plugin
	 */
	String	CAPABILITY_NAME_ATTRIBUTE			= "bnd.workspace.extension";

	/**
	 * The version of the extension
	 */
	String					CAPABILITY_VERSION_ATTRIBUTE		= Constants.VERSION_ATTRIBUTE;

	/**
	 * An extension needs to declare a bndfile to include when activated.
	 */
	String	CAPABILITY_INCLUDE_ATTRIBUTE		= "include";

	/**
	 * A useful description of the Workspace Extension
	 */
	String	CAPABILITY_DESCRIPTION_ATTRIBUTE	= "description";

	Comparator<Capability>	VERSION_COMPARATOR					=								//
		(o1, o2) -> {
			if (o1 == o2)
				return 0;

			if (o1 == null)
				return -1;

			if (o2 == null)
				return 1;

			if (o1.equals(o2))
				return 0;

			String v1 = (String) o1.getAttributes()
				.get(CAPABILITY_VERSION_ATTRIBUTE);
			String v2 = (String) o2.getAttributes()
				.get(CAPABILITY_VERSION_ATTRIBUTE);

			if (v1 == v2)
				return 0;

			if (v1 == null)
				return -1;

			if (v2 == null)
				return 1;

			return new Version(v1).compareTo(new Version(v2));
		};

	static Result<Capability> findWorkspaceExtension(String extensionName, String versionString,
		Set<Capability> availableWorkspaceExtensions) {
		RequirementBuilder rb = new RequirementBuilder(CAPABILITY_NAME_ATTRIBUTE);

		FilterBuilder filter = new FilterBuilder();
		filter = filter.and()
			.eq(CAPABILITY_NAME_ATTRIBUTE, extensionName);
		if (versionString != null) {
			boolean isVersion = Verifier.isVersion(versionString);
			boolean isVersionRange = VersionRange.isVersionRange(versionString);

			if (!(isVersion || isVersionRange)) {
				return Result.err("Invalid version %s on workspaceextension %s", versionString, extensionName);
			}
			VersionRange range = isVersionRange ? VersionRange.parseVersionRange(versionString)
				: new VersionRange(Version.parseVersion(versionString), Version.parseVersion(versionString)
					.bumpMajor());
			filter = filter.and()
				.in(WorkspaceExtensionNamespace.CAPABILITY_VERSION_ATTRIBUTE, range);
		}
		filter.endAnd();
		rb.addFilter(filter);

		List<Capability> matches = ResourceUtils.findProviders(rb.buildSyntheticRequirement(),
			availableWorkspaceExtensions);
		Optional<Capability> capOpt = matches.stream()
			.sorted(VERSION_COMPARATOR)
			.findFirst();
		if (!capOpt.isPresent()) {
			return Result.err("Could not find workspaceextension %s with version %s", extensionName, versionString);
		}
		return Result.ok(capOpt.get());

	}

}
