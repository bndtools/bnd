package bndtools.model.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.xml.sax.InputSource;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.repository.p2.provider.P2Repository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.xml.XML;
import aQute.p2.provider.Feature;
import bndtools.tasks.ResourceCapReqLoader;

public class RepositoryFeaturesTest {
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void featuresHaveTheSameHierarchyAndVisibility(boolean p2) throws Exception {
		Feature feature = new Feature(XML.newDocumentBuilderFactory().newDocumentBuilder().parse(new InputSource(
			new StringReader("""
				<feature id="test.feature" label="Test &amp; Feature" version="1.2.3">
				  <plugin id="test.bundle" version="2.0.0" os="win32"/>
				  <includes id="test.included" version="3.0.0" optional="true"/>
				  <requires><import feature="test.required" version="4.0.0" match="compatible"/></requires>
				</feature>
				"""))));
		feature.parse();
		Resource resource = feature.toResource();
		Resource bundle = new ResourceBuilder().addCapability(new CapReqBuilder("osgi.identity")
			.addAttribute("osgi.identity", "test.feature").addAttribute("type", "osgi.bundle")
			.addAttribute("version", new org.osgi.framework.Version("1.2.3"))).build();
		ResourcesRepository backing = new ResourcesRepository(List.of(resource, bundle));
		RepositoryPlugin plugin = p2 ? mock(P2Repository.class) : mock(OSGiRepository.class);
		Repository repository = (Repository) plugin;
		when(repository.findProviders(anyCollection())).thenAnswer(invocation -> {
			java.util.Collection<org.osgi.resource.Requirement> requirements = invocation.getArgument(0);
			return backing.findProviders(requirements);
		});
		RepositoryTreeContentProvider provider = new RepositoryTreeContentProvider();
		List<RepositoryFeatureResource> features = provider.getRepositoryFeatures(plugin, repository);
		assertThat(features).hasSize(1);
		RepositoryFeatureResource entry = features.get(0);
		assertThat(entry.getResource()).isSameAs(resource);
		assertThat(entry.getLabel()).isEqualTo("Test & Feature");
		assertThat(entry.getFeature().getPlugins()).singleElement().satisfies(member -> {
			assertThat(member.id).isEqualTo("test.bundle");
			assertThat(member.version).isEqualTo("2.0.0");
			assertThat(member.os).isEqualTo("win32");
		});
		assertThat(entry.getFeature().getIncludes()).singleElement().satisfies(member -> {
			assertThat(member.id).isEqualTo("test.included");
			assertThat(member.optional).isTrue();
		});
		assertThat(entry.getFeature().getRequires()).singleElement()
			.satisfies(member -> {
				assertThat(member.feature).isEqualTo("test.required");
				assertThat(member.version).isEqualTo("[4.0.0,5.0.0)");
			});
		FeatureVersionNode version = (FeatureVersionNode) provider.getChildren(entry)[0];
		assertThat(provider.getChildren(version)).hasSize(3);
		assertThat(new ResourceCapReqLoader(entry.getResource()).loadRequirements().get("osgi.identity"))
			.hasSize(3);
		assertThat(RepositoryFeatureResource.getMemberRequirement(entry, "test.required", Feature.RELATION_REQUIRE)
			.getDirectives().get("filter")).contains("(version>=4.0.0)", "(!(version>=5.0.0))");

		provider.setRequirementFilter(CapReqBuilder.createIdentityRequirement("test.feature", Feature.TYPE, null));
		assertThat(provider.searchR5Repository(plugin, repository)).hasSize(1);
		provider.setShowFeatures(false);
		assertThat(provider.getRepositoryFeatures(plugin, repository)).isEmpty();
		assertThat(provider.searchR5Repository(plugin, repository)).isEmpty();
		provider.setRequirementFilter(CapReqBuilder.createIdentityRequirement("test.feature", null, null));
		assertThat(provider.searchR5Repository(plugin, repository)).singleElement()
			.isInstanceOf(RepositoryResourceElement.class);
		provider.setShowFeatures(true);
		assertThat(provider.searchR5Repository(plugin, repository)).hasSize(2);
		assertThat(provider.getRepositoryFeatures(plugin, repository)).hasSize(1);
		provider.setFilter("missing");
		assertThat(provider.getRepositoryFeatures(plugin, repository)).isEmpty();
		provider.setFilter("TEST.FEATURE");
		assertThat(provider.getRepositoryFeatures(plugin, repository)).hasSize(1);
	}
}