package test;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;

/**
 * Test the expansion of Eclipse features on -buildpath like paths. A feature
 * is a container of included bundles ({@code <plugin>}) and included features
 * ({@code <includes>}); on a path it expands to its member bundles. Required
 * features/plugins ({@code <requires>}) are dependencies and are not
 * expanded.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class FeatureBuildpathTest {
	private static final String	FEATURE_TYPE	= "org.eclipse.update.feature";

	@InjectSoftAssertions
	SoftAssertions				softly;

	@InjectTemporaryDirectory
	File						tmp;

	/**
	 * A repository mimicking the P2 repository semantics: bundles and
	 * features can share bsn+version, get() dispatches on the requested
	 * identity type, versions() only lists bundles, and the index is
	 * queryable via the OSGi Repository API.
	 */
	static class FeatureRepo extends BaseRepository implements RepositoryPlugin {
		final ResourcesRepository	index	= new ResourcesRepository();
		final Map<String, File>		files	= new HashMap<>();
		final Map<String, TreeSet<Version>>	bundleVersions	= new HashMap<>();

		void addBundle(String bsn, String version, File file) throws Exception {
			ResourceBuilder rb = new ResourceBuilder();
			rb.addCapability(new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
				.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, bsn)
				.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE)
				.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(version)));
			index.add(rb.build());
			files.put(bsn + ":" + version + ":" + IdentityNamespace.TYPE_BUNDLE, file);
			bundleVersions.computeIfAbsent(bsn, k -> new TreeSet<>())
				.add(new Version(version));
		}

		void addFeature(String id, String version, File file, Collection<Requirement> members) throws Exception {
			ResourceBuilder rb = new ResourceBuilder();
			rb.addCapability(new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
				.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, id)
				.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, FEATURE_TYPE)
				.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(version)));
			for (Requirement member : members) {
				rb.addRequirement(CapReqBuilder.clone(member));
			}
			index.add(rb.build());
			files.put(id + ":" + version + ":" + FEATURE_TYPE, file);
		}

		@Override
		public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
			throws Exception {
			String requestedType = properties != null ? properties.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE)
				: null;
			String type = requestedType != null ? requestedType : IdentityNamespace.TYPE_BUNDLE;
			File file = files.get(bsn + ":" + version + ":" + type);
			if (file == null)
				return null;
			for (DownloadListener listener : listeners) {
				listener.success(file);
			}
			return file;
		}

		@Override
		public SortedSet<Version> versions(String bsn) throws Exception {
			TreeSet<Version> versions = bundleVersions.get(bsn);
			return versions != null ? versions : new TreeSet<>();
		}

		@Override
		public List<String> list(String pattern) throws Exception {
			return List.copyOf(bundleVersions.keySet());
		}

		@Override
		public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
			return index.findProviders(requirements);
		}

		@Override
		public PutResult put(java.io.InputStream stream, PutOptions options) throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean canWrite() {
			return false;
		}

		@Override
		public String getName() {
			return "FeatureRepo";
		}

		@Override
		public String getLocation() {
			return "test";
		}
	}

	static Requirement pluginMember(String id, String version, Map<String, String> extra) throws Exception {
		CapReqBuilder builder = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective("filter", "(&(osgi.identity=" + id + ")(version=" + version + "))")
			.addAttribute(ResourceUtils.FEATURE_RELATION_ATTRIBUTE, ResourceUtils.FEATURE_RELATION_PLUGIN)
			.addAttribute("id", id)
			.addAttribute("version", new Version(version));
		if (extra != null) {
			extra.forEach(builder::addAttribute);
		}
		return builder.buildSyntheticRequirement();
	}

	static Requirement includeMember(String id, String version, boolean optional) throws Exception {
		CapReqBuilder builder = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective("filter",
				"(&(osgi.identity=" + id + ")(type=" + FEATURE_TYPE + ")(version=" + version + "))")
			.addAttribute(ResourceUtils.FEATURE_RELATION_ATTRIBUTE, ResourceUtils.FEATURE_RELATION_INCLUDE)
			.addAttribute("id", id)
			.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, FEATURE_TYPE)
			.addAttribute("version", new Version(version));
		if (optional) {
			builder.addDirective("resolution", "optional");
		}
		return builder.buildSyntheticRequirement();
	}

	static Requirement requireMember(String id) throws Exception {
		return new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective("filter", "(osgi.identity=" + id + ")")
			.addAttribute(ResourceUtils.FEATURE_RELATION_ATTRIBUTE, ResourceUtils.FEATURE_RELATION_REQUIRE)
			.addAttribute("id", id)
			.buildSyntheticRequirement();
	}

	File dummyJar(String name) throws Exception {
		File file = new File(tmp, name + ".jar");
		try (Jar jar = new Jar(name)) {
			jar.putResource("about.txt", new aQute.bnd.osgi.EmbeddedResource(name.getBytes(), 0L));
			jar.write(file);
		}
		return file;
	}

	private Workspace getWorkspace() throws Exception {
		File wsDir = new File(tmp, "ws");
		IO.copy(IO.getFile("testresources/ws"), wsDir);
		return new Workspace(wsDir);
	}

	@Test
	public void expandFeatureWithNestedIncludeOnBuildpath() throws Exception {
		try (Workspace ws = getWorkspace()) {
			FeatureRepo repo = new FeatureRepo();
			repo.addBundle("bundle.a", "1.0.0", dummyJar("bundle.a-1.0.0"));
			repo.addBundle("bundle.b", "2.0.0", dummyJar("bundle.b-2.0.0"));
			repo.addBundle("bundle.c", "3.0.0", dummyJar("bundle.c-3.0.0"));
			repo.addFeature("sub.feature", "2.0.0", dummyJar("sub.feature-2.0.0"),
				List.of(pluginMember("bundle.c", "3.0.0", null)));
			repo.addFeature("test.feature", "1.0.0", dummyJar("test.feature-1.0.0"), List.of(
				pluginMember("bundle.a", "1.0.0", null), //
				pluginMember("bundle.b", "2.0.0", null), //
				includeMember("sub.feature", "2.0.0", false), //
				requireMember("required.bundle.not.in.repo")));
			ws.addBasicPlugin(repo);

			try (Project project = ws.getProject("p1")) {
				List<Container> containers = project.getBundles(Strategy.LOWEST,
					"test.feature;version='1.0.0';type=org.eclipse.update.feature", "-buildpath");

				softly.assertThat(project.check())
					.as("no errors or warnings expected")
					.isTrue();
				softly.assertThat(containers)
					.extracting(Container::getBundleSymbolicName)
					.containsExactly("bundle.a", "bundle.b", "bundle.c");
				softly.assertThat(containers)
					.allMatch(c -> c.getType() == Container.TYPE.REPO)
					.allMatch(c -> c.getError() == null);
			}
		}
	}

	@Test
	public void expandFeatureSkipsForeignPlatformMembers() throws Exception {
		try (Workspace ws = getWorkspace()) {
			FeatureRepo repo = new FeatureRepo();
			repo.addBundle("bundle.a", "1.0.0", dummyJar("bundle.a-1.0.0"));
			repo.addBundle("bundle.foreign", "1.0.0", dummyJar("bundle.foreign-1.0.0"));
			repo.addFeature("test.feature", "1.0.0", dummyJar("test.feature-1.0.0"), List.of(
				pluginMember("bundle.a", "1.0.0", null), //
				pluginMember("bundle.foreign", "1.0.0", Map.of("os", "qnx"))));
			ws.addBasicPlugin(repo);

			try (Project project = ws.getProject("p1")) {
				List<Container> containers = project.getBundles(Strategy.LOWEST,
					"test.feature;version='1.0.0';type=org.eclipse.update.feature", "-buildpath");

				softly.assertThat(project.check())
					.isTrue();
				softly.assertThat(containers)
					.extracting(Container::getBundleSymbolicName)
					.containsExactly("bundle.a");
			}
		}
	}

	@Test
	public void expandFeatureCycleTerminates() throws Exception {
		try (Workspace ws = getWorkspace()) {
			FeatureRepo repo = new FeatureRepo();
			repo.addBundle("bundle.a", "1.0.0", dummyJar("bundle.a-1.0.0"));
			repo.addBundle("bundle.b", "1.0.0", dummyJar("bundle.b-1.0.0"));
			repo.addFeature("feature.a", "1.0.0", dummyJar("feature.a-1.0.0"), List.of(
				pluginMember("bundle.a", "1.0.0", null), //
				includeMember("feature.b", "1.0.0", false)));
			repo.addFeature("feature.b", "1.0.0", dummyJar("feature.b-1.0.0"), List.of(
				pluginMember("bundle.b", "1.0.0", null), //
				includeMember("feature.a", "1.0.0", false)));
			ws.addBasicPlugin(repo);

			try (Project project = ws.getProject("p1")) {
				List<Container> containers = project.getBundles(Strategy.LOWEST,
					"feature.a;version='1.0.0';type=org.eclipse.update.feature", "-buildpath");

				softly.assertThat(project.check("Detected a cycle in the includes of feature"))
					.isTrue();
				softly.assertThat(containers)
					.extracting(Container::getBundleSymbolicName)
					.containsExactly("bundle.a", "bundle.b");
			}
		}
	}

	@Test
	public void expandFeatureFallsBackToHighestWhenExactVersionMissing() throws Exception {
		try (Workspace ws = getWorkspace()) {
			FeatureRepo repo = new FeatureRepo();
			repo.addBundle("bundle.a", "1.0.0", dummyJar("bundle.a-1.0.0"));
			repo.addFeature("test.feature", "1.0.0", dummyJar("test.feature-1.0.0"),
				List.of(pluginMember("bundle.a", "9.9.9", null)));
			ws.addBasicPlugin(repo);

			try (Project project = ws.getProject("p1")) {
				List<Container> containers = project.getBundles(Strategy.LOWEST,
					"test.feature;version='1.0.0';type=org.eclipse.update.feature", "-buildpath");

				softly.assertThat(project
					.check("Member bundle.a;version=9.9.9 of feature test.feature:1.0.0 not found with the exact"))
					.isTrue();
				softly.assertThat(containers)
					.extracting(Container::getBundleSymbolicName)
					.containsExactly("bundle.a");
				softly.assertThat(containers.get(0)
					.getVersion())
					.isEqualTo("1.0.0");
			}
		}
	}

	@Test
	public void expandFeatureSkipsMissingOptionalInclude() throws Exception {
		try (Workspace ws = getWorkspace()) {
			FeatureRepo repo = new FeatureRepo();
			repo.addBundle("bundle.a", "1.0.0", dummyJar("bundle.a-1.0.0"));
			repo.addFeature("test.feature", "1.0.0", dummyJar("test.feature-1.0.0"), List.of(
				pluginMember("bundle.a", "1.0.0", null), //
				includeMember("no.such.feature", "1.0.0", true)));
			ws.addBasicPlugin(repo);

			try (Project project = ws.getProject("p1")) {
				List<Container> containers = project.getBundles(Strategy.LOWEST,
					"test.feature;version='1.0.0';type=org.eclipse.update.feature", "-buildpath");

				softly.assertThat(project.check())
					.as("missing optional include must not cause errors")
					.isTrue();
				softly.assertThat(containers)
					.extracting(Container::getBundleSymbolicName)
					.containsExactly("bundle.a");
			}
		}
	}

	@Test
	public void missingFeatureReportsError() throws Exception {
		try (Workspace ws = getWorkspace()) {
			ws.addBasicPlugin(new FeatureRepo());

			try (Project project = ws.getProject("p1")) {
				List<Container> containers = project.getBundles(Strategy.LOWEST,
					"no.such.feature;version='1.0.0';type=org.eclipse.update.feature", "-buildpath");

				softly.assertThat(project.check())
					.as("the error is carried by the container and reported by doPath later")
					.isTrue();
				softly.assertThat(containers)
					.hasSize(1);
				softly.assertThat(containers.get(0)
					.getType())
					.isEqualTo(Container.TYPE.ERROR);
				softly.assertThat(containers.get(0)
					.getError())
					.contains("Not found in");
			}
		}
	}

	@Test
	public void featureAndBundleWithSameIdAreDispatchedByType() throws Exception {
		try (Workspace ws = getWorkspace()) {
			FeatureRepo repo = new FeatureRepo();
			// bundle.shared has the same id as the feature but is a plain bundle
			repo.addBundle("bundle.shared", "1.0.0", dummyJar("bundle.shared-1.0.0"));
			repo.addBundle("bundle.member", "1.0.0", dummyJar("bundle.member-1.0.0"));
			// feature named bundle.shared — same id as the bundle above
			repo.addFeature("bundle.shared", "1.0.0", dummyJar("bundle.shared-feature-1.0.0"),
				List.of(pluginMember("bundle.member", "1.0.0", null)));
			ws.addBasicPlugin(repo);

			try (Project project = ws.getProject("p1")) {
				// requesting by type=feature must expand to member bundles, not return the bundle with the same id
				List<Container> featureContainers = project.getBundles(Strategy.LOWEST,
					"bundle.shared;version='1.0.0';type=org.eclipse.update.feature", "-buildpath");

				softly.assertThat(project.check())
					.as("no errors or warnings expected")
					.isTrue();
				softly.assertThat(featureContainers)
					.as("feature expansion must yield its member, not the bundle with the same id")
					.extracting(Container::getBundleSymbolicName)
					.containsExactly("bundle.member");

				// requesting without type must resolve to the plain bundle
				List<Container> bundleContainers = project.getBundles(Strategy.LOWEST,
					"bundle.shared;version='1.0.0'", "-buildpath");

				softly.assertThat(bundleContainers)
					.as("plain request must yield the bundle, not the feature members")
					.extracting(Container::getBundleSymbolicName)
					.containsExactly("bundle.shared");
			}
		}
	}
}
