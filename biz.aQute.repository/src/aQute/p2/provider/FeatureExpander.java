package aQute.p2.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.service.Strategy;

/**
 * Expands an Eclipse feature container into its member bundles using the
 * feature requirements of an OSGi repository index, see
 * {@link aQute.bnd.service.RepositoryPlugin#getTypedResourceMembers}.
 */
public class FeatureExpander {

	private FeatureExpander() {}

	/**
	 * Expand an Eclipse feature (identity type {@value Feature#TYPE})
	 * container into its member bundles. Members are the {@code <plugin>}
	 * references and, recursively, the members of {@code <includes>}
	 * referenced features. {@code <requires>} imports are dependencies, not
	 * members, and are ignored. Members whose os/ws/arch does not match the
	 * target platform (the osgi.os/osgi.ws/osgi.arch of the project's
	 * -runproperties, the running platform otherwise) are skipped.
	 *
	 * @param repository the repository that indexes the feature
	 * @param container the typed resource container to expand
	 * @param visited cycle-detection guard keyed by {@code bsn:version}
	 * @return the member containers, or {@code null} for any other identity
	 *         type or when the repository does not have the requested feature
	 *         indexed, so that other repositories get a chance to expand it
	 */
	public static List<Container> getMembers(Repository repository, Container container, Set<String> visited)
		throws Exception {
		if (!Feature.TYPE.equals(container.getAttributes()
			.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE)))
			return null;

		String featureId = container.getBundleSymbolicName();
		String key = featureId + ":" + container.getVersion();

		Resource resource = findFeatureResource(repository, featureId, container.getVersion());
		if (resource == null)
			return null; // this repository does not have this feature indexed

		Project project = container.getProject();
		EclipsePlatform platform = platform(project);
		List<Container> result = new ArrayList<>();
		List<Requirement> requirements = resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
		int marked = 0;
		for (Requirement requirement : requirements) {
			Map<String, Object> reqAttrs = requirement.getAttributes();
			String relation = Objects.toString(reqAttrs.get(Feature.RELATION_ATTRIBUTE), null);
			if (relation == null)
				continue;
			marked++;

			boolean include = Feature.RELATION_INCLUDE.equals(relation);
			if (!include && !Feature.RELATION_PLUGIN.equals(relation))
				continue; // a requires import is a dependency, not a member

			if (!platform.matches(requirement))
				continue;

			String id = Objects.toString(reqAttrs.get("id"), null);
			if (id == null)
				continue;
			String version = Objects.toString(reqAttrs.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE), null);
			boolean optional = Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives()
				.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));

			Container member = getFeatureMember(project, key, id, version, include, optional);
			if (member == null)
				continue;

			for (Container c : member.getMembers(visited)) {
				if (!result.contains(c))
					result.add(c);
			}
		}

		if (marked == 0 && !requirements.isEmpty()) {
			project.warning(
				"The index of the repository containing feature %s predates feature member support, refresh the repository to expand the feature",
				key);
		}
		return result;
	}

	private static EclipsePlatform platform(Project project) {
		try {
			return EclipsePlatform.of(project.getRunProperties());
		} catch (IllegalArgumentException e) {
			project.warning("Cannot parse -runproperties, using the running platform for features: %s",
				e.getMessage());
			return EclipsePlatform.of(Map.of());
		}
	}

	/**
	 * Resolve a single feature member. The version pinned in the feature is
	 * tried exactly first; if absent from the repositories the highest
	 * available version is used with a warning.
	 *
	 * @return the member container, an error container, or null when an
	 *         optional member is not available
	 */
	private static Container getFeatureMember(Project project, String featureKey, String id, String version,
		boolean include, boolean optional) throws Exception {
		Map<String, String> attrs = include
			? Collections.singletonMap(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, Feature.TYPE)
			: null;

		Container member = null;
		if (version != null && Verifier.isVersion(version)) {
			member = project.getBundle(id, version, Strategy.EXACT, attrs);
		}
		if (member == null || member.getError() != null) {
			Container highest = project.getBundle(id, null, Strategy.HIGHEST, attrs);
			if (highest.getError() == null) {
				if (member != null) {
					project.warning(
						"Member %s;version=%s of feature %s not found with the exact version, using %s instead", id,
						version, featureKey, highest.getVersion());
				}
				member = highest;
			} else if (member == null) {
				member = highest;
			}
		}

		if (member.getError() != null && optional) {
			return null;
		}
		return member;
	}

	private static Resource findFeatureResource(Repository repository, String bsn, String version) {
		Requirement requirement = CapReqBuilder.createIdentityRequirement(bsn, Feature.TYPE, version);
		Map<Requirement, Collection<Capability>> providers = repository
			.findProviders(Collections.singleton(requirement));
		Collection<Capability> capabilities = providers != null ? providers.get(requirement) : null;
		if (capabilities == null || capabilities.isEmpty())
			return null;
		return capabilities.iterator()
			.next()
			.getResource();
	}
}
