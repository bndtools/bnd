package aQute.bnd.repository.p2.provider;

import static aQute.bnd.service.tags.Tags.parse;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.Feature;
import aQute.service.reporter.Reporter;

/**
 * A p2 repository
 */
@BndPlugin(name = "P2 Repo", parameters = P2Config.class)
public class P2Repository extends BaseRepository
	implements Plugin, RegistryPlugin, RepositoryPlugin, Refreshable, Closeable, Actionable {
	private P2Config	config;
	private Registry	registry;
	private Workspace	workspace;
	private P2Indexer	p2Index;
	private Reporter	reporter;
	private String		name;

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		return getP2Index().get(bsn, version, properties, listeners);

	}

	private synchronized P2Indexer getP2Index() {
		if (p2Index != null)
			return p2Index;

		return p2Index = getP2Index0();
	}

	P2Indexer getP2Index0() {
		this.workspace = registry.getPlugin(Workspace.class);
		HttpClient client = registry.getPlugin(HttpClient.class);
		URI url = config.url();

		if (url == null)
			throw new IllegalArgumentException("For a p2 repository you must set the url parameter to the repository");

		try {
			name = config.name(client.toName(url));
			File location = workspace.getFile(config.location("cnf/cache/p2-" + name));
			IO.mkdirs(location);
			File indexFile = new File(location, "index.xml.gz");

			return new P2Indexer(new Unpack200(this.workspace), reporter, location, client, url, name);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		return getP2Index().list(pattern);
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		return getP2Index().versions(bsn);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLocation() {
		P2Indexer index = getP2Index();
		return index.location.getPath();
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		this.config = Converter.cnv(P2Config.class, map);
		this.name = this.config.name("p2-" + config.url());
		super.setTags(parse(config.tags(), DEFAULT_REPO_TAGS));
	}

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	@Override
	public boolean refresh() throws Exception {
		getP2Index().refresh();
		return true;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return getP2Index().getBridge()
			.getRepository()
			.findProviders(requirements);
	}

	/**
	 * Expand an Eclipse feature (identity type {@code org.eclipse.update.feature})
	 * container into its member bundles. Members are the {@code <plugin>}
	 * references and, recursively, the members of {@code <includes>}
	 * referenced features. {@code <requires>} imports are dependencies, not
	 * members, and are ignored. Members whose os/ws/arch does not match the
	 * running platform are skipped. Returns {@code null} for any other
	 * identity type, or when this repository does not have the requested
	 * feature indexed, so that other repositories get a chance to expand it.
	 */
	@Override
	public List<Container> getTypedResourceMembers(Container container, Set<String> visited) throws Exception {
		if (!Feature.TYPE.equals(container.getAttributes()
			.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE)))
			return null;

		String featureId = container.getBundleSymbolicName();
		String key = featureId + ":" + container.getVersion();

		Resource resource = findFeatureResource(featureId, container.getVersion());
		if (resource == null)
			return null; // this repository does not have this feature indexed

		Project project = container.getProject();
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

			if (!EclipsePlatform.CURRENT.matches(Objects.toString(reqAttrs.get("os"), null),
				Objects.toString(reqAttrs.get("ws"), null), Objects.toString(reqAttrs.get("arch"), null)))
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

	/**
	 * Find the resource of this repository's feature via the OSGi Repository
	 * API.
	 */
	private Resource findFeatureResource(String bsn, String version) {
		Requirement requirement = CapReqBuilder.createIdentityRequirement(bsn, Feature.TYPE, version);
		Map<Requirement, Collection<Capability>> providers = findProviders(Collections.singleton(requirement));
		Collection<Capability> capabilities = providers != null ? providers.get(requirement) : null;
		if (capabilities == null || capabilities.isEmpty())
			return null;
		return capabilities.iterator()
			.next()
			.getResource();
	}


	@Override
	public File getRoot() throws Exception {
		P2Indexer index = getP2Index();
		return index.location;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("Cannot write to a p2 repo ");
	}

	@Override
	public void close() throws IOException {
		IO.close(p2Index);
	}

	@Override
	public String toString() {
		return "P2Repository [" + getName() + "]";
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		if (target.length == 0) {
			if (p2Index.indexFile.isFile()) {
				Map<String, Runnable> menu = new LinkedHashMap<>();
				menu.put("Refresh from " + p2Index.url, () -> {
					try {
						workspace.writeLocked(() -> {
							p2Index.reread();
							workspace.refresh();
							return null;
						});
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
				});
				return menu;
			}
		}
		return null;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		if (target.length == 0) {

			return "P2: " + name + "\n" //
				+ "index      " + p2Index.indexFile.getAbsolutePath() + "\n" //
				+ "uri        " + p2Index.url + "\n" //
				+ "hash       " + p2Index.urlHash;
		}
		return null;
	}

	@Override
	public String title(Object... target) throws Exception {
		return null;
	}

	/**
	 * Get all features available in this P2 repository.
	 *
	 * @return a list of features, or empty list if none available
	 * @throws Exception if an error occurs while fetching features
	 */
	public List<Feature> getFeatures() throws Exception {
		return getP2Index().getFeatures();
	}

	/**
	 * Get a specific feature by ID and version.
	 *
	 * @param id the feature ID
	 * @param version the feature version
	 * @return the feature, or null if not found
	 * @throws Exception if an error occurs while fetching the feature
	 */
	public Feature getFeature(String id, String version) throws Exception {
		return getP2Index().getFeature(id, version);
	}

}
