package aQute.bnd.repository.p2.provider;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.p2.provider.EclipsePlatform;
import aQute.p2.provider.Feature;

/**
 * A p2 repository with a feature that lists the fragments of three platforms.
 * The fragments declare their platform in Eclipse-PlatformFilter.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class P2PlatformFilterTest {

	@InjectSoftAssertions
	SoftAssertions	softly;

	@InjectTemporaryDirectory
	File			tmp;

	private static final String	FEATURE_CLAUSE	= "test.feature;version='1.0.0';type=org.eclipse.update.feature";

	private static void store(String content, File file) throws Exception {
		IO.mkdirs(file.getParentFile());
		IO.store(content, file);
	}

	private void bundle(File repository, String bsn, String fragmentHost, String platformFilter) throws Exception {
		Manifest manifest = new Manifest();
		Attributes main = manifest.getMainAttributes();
		main.putValue("Manifest-Version", "1.0");
		main.putValue("Bundle-ManifestVersion", "2");
		main.putValue("Bundle-SymbolicName", bsn);
		main.putValue("Bundle-Version", "1.0.0");
		if (fragmentHost != null)
			main.putValue("Fragment-Host", fragmentHost);
		if (platformFilter != null)
			main.putValue(EclipsePlatform.FILTER_HEADER, platformFilter);

		try (Jar jar = new Jar(bsn)) {
			jar.setManifest(manifest);
			jar.write(new File(repository, "plugins/" + bsn + "_1.0.0.jar"));
		}
	}

	private void createRepository(File repository) throws Exception {
		IO.mkdirs(new File(repository, "plugins"));
		IO.mkdirs(new File(repository, "features"));

		bundle(repository, "test.host", null, null);
		bundle(repository, "test.frag.win32", "test.host", "(& (osgi.ws=win32) (osgi.os=win32) (osgi.arch=x86_64))");
		bundle(repository, "test.frag.macosx", "test.host", "(& (osgi.ws=cocoa) (osgi.os=macosx) (osgi.arch=x86_64))");
		bundle(repository, "test.frag.linux", "test.host", "(& (osgi.ws=gtk) (osgi.os=linux) (osgi.arch=x86_64))");
		bundle(repository, "test.nl", "test.host", "(osgi.nl=de)");

		String featureXml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feature id="test.feature" label="Test" version="1.0.0">
			  <plugin id="test.host" version="1.0.0" unpack="false"/>
			  <plugin id="test.frag.win32" version="1.0.0" os="win32" ws="win32" arch="x86_64" fragment="true"/>
			  <plugin id="test.frag.macosx" version="1.0.0" os="macosx" ws="cocoa" arch="x86_64" fragment="true"/>
			  <plugin id="test.frag.linux" version="1.0.0" os="linux" ws="gtk" arch="x86_64" fragment="true"/>
			</feature>
			""";
		try (Jar jar = new Jar("test.feature")) {
			jar.putResource("feature.xml", new EmbeddedResource(featureXml.getBytes(UTF_8), 0L));
			jar.write(new File(repository, "features/test.feature_1.0.0.jar"));
		}

		store("""
			<?xml version='1.0' encoding='UTF-8'?>
			<?artifactRepository version='1.1.0'?>
			<repository name='Platform Fixture' type='org.eclipse.equinox.p2.artifact.repository.simpleRepository' version='1'>
			  <mappings size='2'>
			    <rule filter='(&amp; (classifier=osgi.bundle))' output='${repoUrl}/plugins/${id}_${version}.jar'/>
			    <rule filter='(&amp; (classifier=org.eclipse.update.feature))' output='${repoUrl}/features/${id}_${version}.jar'/>
			  </mappings>
			  <artifacts size='6'>
			    <artifact classifier='osgi.bundle' id='test.host' version='1.0.0'/>
			    <artifact classifier='osgi.bundle' id='test.frag.win32' version='1.0.0'/>
			    <artifact classifier='osgi.bundle' id='test.frag.macosx' version='1.0.0'/>
			    <artifact classifier='osgi.bundle' id='test.frag.linux' version='1.0.0'/>
			    <artifact classifier='osgi.bundle' id='test.nl' version='1.0.0'/>
			    <artifact classifier='org.eclipse.update.feature' id='test.feature' version='1.0.0'/>
			  </artifacts>
			</repository>
			""", new File(repository, "artifacts.xml"));
		store("""
			version = 1
			metadata.repository.factory.order = content.xml,!
			artifact.repository.factory.order = artifacts.xml,!
			""", new File(repository, "p2.index"));
	}

	/**
	 * A workspace with the fixture repository and the projects win, macosx,
	 * anywhere, host and broken, which have the feature on their -buildpath and
	 * different -runproperties.
	 */
	private Workspace workspace() throws Exception {
		File repository = new File(tmp, "p2");
		createRepository(repository);

		File ws = new File(tmp, "ws");
		store("""
			-connection-settings: false
			-plugin.p2: \\
			  aQute.bnd.repository.p2.provider.P2Repository; \\
			    name=fixture; \\
			    url="%s"
			""".formatted(repository.toURI()), new File(ws, "cnf/build.bnd"));

		project(ws, "win", "-runproperties: osgi.os=win32, osgi.ws=win32, osgi.arch=x86_64");
		project(ws, "macosx", "-runproperties: osgi.os=macosx, osgi.ws=cocoa, osgi.arch=x86_64");
		project(ws, "anywhere", "-runproperties: osgi.os=*, osgi.ws=*, osgi.arch=*");
		project(ws, "host", "");
		project(ws, "broken", "-runproperties: osgi.os=win32,");

		return Workspace.getWorkspace(ws);
	}

	private void project(File ws, String name, String runProperties) throws Exception {
		store("-buildpath: " + FEATURE_CLAUSE + "\n" + runProperties + "\n", new File(ws, name + "/bnd.bnd"));
	}

	private Resource resource(P2Repository repository, String bsn) {
		Requirement requirement = CapReqBuilder.createIdentityRequirement(bsn, null, null);
		Map<Requirement, Collection<Capability>> providers = repository
			.findProviders(Collections.singleton(requirement));
		return providers.get(requirement)
			.iterator()
			.next()
			.getResource();
	}

	private List<String> buildpath(Workspace ws, String project) throws Exception {
		try (Project p = ws.getProject(project)) {
			softly.assertThat(p.getErrors())
				.as("errors of " + project)
				.isEmpty();
			return p.getBuildpath()
				.stream()
				.map(Container::getBundleSymbolicName)
				.sorted()
				.collect(Collectors.toList());
		}
	}

	@Test
	public void testPlatformFilterOfBundlesIsIndexed() throws Exception {
		try (Workspace ws = workspace()) {
			P2Repository repository = ws.getPlugin(P2Repository.class);

			softly.assertThat(resource(repository, "test.frag.win32")
				.getRequirements(EclipsePlatform.NAMESPACE))
				.hasSize(1)
				.first()
				.extracting(r -> r.getDirectives()
					.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE))
				.isEqualTo("(&(osgi.ws=win32)(osgi.os=win32)(osgi.arch=x86_64))");

			softly.assertThat(resource(repository, "test.frag.macosx")
				.getRequirements(EclipsePlatform.NAMESPACE))
				.hasSize(1);
			softly.assertThat(resource(repository, "test.host")
				.getRequirements(EclipsePlatform.NAMESPACE))
				.as("no filter")
				.isEmpty();
			softly.assertThat(resource(repository, "test.nl")
				.getRequirements(EclipsePlatform.NAMESPACE))
				.as("a locale filter cannot be evaluated")
				.isEmpty();
		}
	}

	@Test
	public void testFeatureMembersCarryTheirPlatform() throws Exception {
		try (Workspace ws = workspace()) {
			P2Repository repository = ws.getPlugin(P2Repository.class);

			Requirement featureRequirement = CapReqBuilder.createIdentityRequirement("test.feature", Feature.TYPE,
				null);
			Resource feature = repository.findProviders(Collections.singleton(featureRequirement))
				.get(featureRequirement)
				.iterator()
				.next()
				.getResource();

			softly.assertThat(feature.getRequirements(EclipsePlatform.NAMESPACE))
				.isEmpty();
			softly.assertThat(feature.getRequirements("osgi.identity")
				.stream()
				.filter(r -> "macosx".equals(r.getAttributes()
					.get("os")))
				.count())
				.isEqualTo(1);
		}
	}

	@Test
	public void testBuildpathExpandsTheFeatureForTheTargetPlatform() throws Exception {
		try (Workspace ws = workspace()) {
			softly.assertThat(buildpath(ws, "win"))
				.containsExactly("test.frag.win32", "test.host");
			softly.assertThat(buildpath(ws, "macosx"))
				.containsExactly("test.frag.macosx", "test.host");
			softly.assertThat(buildpath(ws, "anywhere"))
				.containsExactly("test.frag.linux", "test.frag.macosx", "test.frag.win32", "test.host");
		}
	}

	@Test
	public void testBuildpathExpandsTheFeatureForTheRunningPlatformByDefault() throws Exception {
		try (Workspace ws = workspace()) {
			EclipsePlatform host = EclipsePlatform.of(Map.of());
			List<String> expected = new ArrayList<>(List.of("test.host"));
			for (String[] fragment : new String[][] {
				{
					"test.frag.win32", "win32", "win32"
				}, {
					"test.frag.macosx", "macosx", "cocoa"
				}, {
					"test.frag.linux", "linux", "gtk"
				}
			}) {
				CapReqBuilder member = new CapReqBuilder("osgi.identity")
					.addAttribute(Feature.RELATION_ATTRIBUTE, Feature.RELATION_PLUGIN)
					.addAttribute("os", fragment[1])
					.addAttribute("ws", fragment[2])
					.addAttribute("arch", "x86_64");
				if (host.matches(member.buildSyntheticRequirement()))
					expected.add(fragment[0]);
			}
			expected.sort(null);

			softly.assertThat(buildpath(ws, "host"))
				.containsExactlyElementsOf(expected);
		}
	}

	@Test
	public void testRunPropertiesThatCannotBeParsedUseTheRunningPlatform() throws Exception {
		try (Workspace ws = workspace()) {
			List<String> host = buildpath(ws, "host");

			softly.assertThat(buildpath(ws, "broken"))
				.isEqualTo(host);
		}
	}

	@Test
	public void testOSGiRepositoryOnTheP2IndexExpandsTheFeature() throws Exception {
		File index;
		try (Workspace ws = workspace()) {
			P2Repository repository = ws.getPlugin(P2Repository.class);
			repository.list(null);
			index = new File(repository.getLocation(), "index.xml.gz");
		}
		softly.assertThat(index)
			.isFile();

		File ws = new File(tmp, "osgi-ws");
		store("""
			-connection-settings: false
			-plugin.osgi: \\
			  aQute.bnd.repository.osgi.OSGiRepository; \\
			    name=fixture; \\
			    locations="%s"
			""".formatted(index.toURI()), new File(ws, "cnf/build.bnd"));
		project(ws, "win", "-runproperties: osgi.os=win32, osgi.ws=win32, osgi.arch=x86_64");
		project(ws, "macosx", "-runproperties: osgi.os=macosx, osgi.ws=cocoa, osgi.arch=x86_64");

		try (Workspace workspace = Workspace.getWorkspace(ws)) {
			softly.assertThat(buildpath(workspace, "win"))
				.containsExactly("test.frag.win32", "test.host");
			softly.assertThat(buildpath(workspace, "macosx"))
				.containsExactly("test.frag.macosx", "test.host");
			softly.assertThat(workspace.getRepository("fixture")
				.versions("test.feature"))
				.as("features are only found by type")
				.isEmpty();
		}
	}

	/**
	 * A p2 index with a bundle and a feature of the same identity, used via an
	 * OSGiRepository. The index keeps one resource per identity, so either one
	 * hides the other in the lookup by bsn and version.
	 */
	@Test
	public void testOSGiRepositoryDistinguishesBundleAndFeatureWithTheSameIdentity() throws Exception {
		File p2 = new File(tmp, "shared-p2");
		IO.mkdirs(new File(p2, "plugins"));
		IO.mkdirs(new File(p2, "features"));
		bundle(p2, "shared", null, null);
		String featureXml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feature id="shared" label="Shared" version="1.0.0">
			  <plugin id="shared" version="1.0.0" unpack="false"/>
			</feature>
			""";
		try (Jar jar = new Jar("shared")) {
			jar.putResource("feature.xml", new EmbeddedResource(featureXml.getBytes(UTF_8), 0L));
			jar.write(new File(p2, "features/shared_1.0.0.jar"));
		}
		store("""
			<?xml version='1.0' encoding='UTF-8'?>
			<?artifactRepository version='1.1.0'?>
			<repository name='Shared Fixture' type='org.eclipse.equinox.p2.artifact.repository.simpleRepository' version='1'>
			  <mappings size='2'>
			    <rule filter='(&amp; (classifier=osgi.bundle))' output='${repoUrl}/plugins/${id}_${version}.jar'/>
			    <rule filter='(&amp; (classifier=org.eclipse.update.feature))' output='${repoUrl}/features/${id}_${version}.jar'/>
			  </mappings>
			  <artifacts size='2'>
			    <artifact classifier='osgi.bundle' id='shared' version='1.0.0'/>
			    <artifact classifier='org.eclipse.update.feature' id='shared' version='1.0.0'/>
			  </artifacts>
			</repository>
			""", new File(p2, "artifacts.xml"));
		store("""
			version = 1
			metadata.repository.factory.order = content.xml,!
			artifact.repository.factory.order = artifacts.xml,!
			""", new File(p2, "p2.index"));

		File p2ws = new File(tmp, "shared-p2-ws");
		store("""
			-connection-settings: false
			-plugin.p2: \\
			  aQute.bnd.repository.p2.provider.P2Repository; \\
			    name=fixture; \\
			    url="%s"
			""".formatted(p2.toURI()), new File(p2ws, "cnf/build.bnd"));
		File index;
		try (Workspace workspace = Workspace.getWorkspace(p2ws)) {
			P2Repository repository = workspace.getPlugin(P2Repository.class);
			repository.list(null);
			index = new File(repository.getLocation(), "index.xml.gz");
		}

		File ws = new File(tmp, "shared-osgi-ws");
		store("""
			-connection-settings: false
			-plugin.osgi: \\
			  aQute.bnd.repository.osgi.OSGiRepository; \\
			    name=fixture; \\
			    locations="%s"
			""".formatted(index.toURI()), new File(ws, "cnf/build.bnd"));
		store("-buildpath: shared;version=1.0.0\n", new File(ws, "bundle/bnd.bnd"));
		store("-buildpath: shared;version=1.0.0;type=org.eclipse.update.feature\n", new File(ws, "feature/bnd.bnd"));

		try (Workspace workspace = Workspace.getWorkspace(ws)) {
			RepositoryPlugin repository = workspace.getRepository("fixture");
			Version version = new Version("1.0.0");

			softly.assertThat(repository.versions("shared"))
				.containsExactly(version);

			File bundle = repository.get("shared", version, Map.of());
			softly.assertThat(bundle)
				.as("without a type the bundle is returned")
				.isFile();
			try (Jar jar = new Jar(bundle)) {
				softly.assertThat(jar.getBsn())
					.isEqualTo("shared");
				softly.assertThat(jar.getResource("feature.xml"))
					.isNull();
			}

			File feature = repository.get("shared", version, Map.of("type", Feature.TYPE));
			softly.assertThat(feature)
				.as("with the feature type the feature is returned")
				.isFile()
				.isNotEqualTo(bundle);
			try (Jar jar = new Jar(feature)) {
				softly.assertThat(jar.getResource("feature.xml"))
					.isNotNull();
			}

			softly.assertThat(buildpath(workspace, "bundle"))
				.containsExactly("shared");
			softly.assertThat(buildpath(workspace, "feature"))
				.containsExactly("shared");
		}
	}
}
