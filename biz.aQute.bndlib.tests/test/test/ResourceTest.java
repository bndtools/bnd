package test;

import static aQute.bnd.osgi.resource.ResourceUtils.getSubstitutionPackages;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.build.model.clauses.ImportPattern;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.PackageExpression;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class ResourceTest {
	static String		defaultSHA		= "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	static String		alternativeSHA	= "AAAAAAAAAAAAFFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	static FilterParser	filterParser	= new FilterParser();
	@InjectTemporaryDirectory
	File				tmp;

	@Test
	public void packageDecoration() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		Parameters exports = new Parameters("com.foo;version=1.0;abc=bar;mandatory:=abc");
		rb.addExportPackages(exports, "com.bar", new Version("1.2.3"));
		Parameters imports = new Parameters(
			"com.foo;version=1.0;bundle-symbolic-name=com.bar;bundle-version=1.2.3;abc=bar;dirtest:=directive");
		rb.addImportPackages(imports);
		Resource built = rb.build();

		List<Capability> capabilities = built.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(capabilities).hasSize(1);
		assertThat(capabilities.get(0)
			.getAttributes()).containsEntry(PackageNamespace.PACKAGE_NAMESPACE, "com.foo")
				.containsEntry(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version("1"))
				.containsEntry(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE, "com.bar")
				.containsEntry(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, new Version("1.2.3"))
				.containsEntry("abc", "bar");
		assertThat(capabilities.get(0)
			.getDirectives()).containsEntry(AbstractWiringNamespace.CAPABILITY_MANDATORY_DIRECTIVE, "abc");

		List<Requirement> requirements = built.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(requirements).hasSize(1);
		assertThat(requirements.get(0)
			.getDirectives()).containsEntry("dirtest", "directive")
				.extractingByKey(Namespace.REQUIREMENT_FILTER_DIRECTIVE, InstanceOfAssertFactories.STRING)
				.contains("(" + PackageNamespace.PACKAGE_NAMESPACE + "=com.foo)",
					"(" + PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE + ">=1.0.0)",
					"(" + PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE + "=com.bar)",
					"(" + AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE + ">=1.2.3)", "(abc=bar)")
				.doesNotContain("dirtest", "directive");
		assertThat(requirements.get(0)
			.getAttributes()).containsEntry(PackageNamespace.PACKAGE_NAMESPACE, "com.foo");
	}

	@Test
	public void testImportPackage() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		Requirement importPackage = rb.addImportPackage("com.foo", Attrs.create("version", "1.2.3")
			.with("a", "1")
			.with("b", "2"));
		String filter = importPackage.getDirectives()
			.get("filter");
		assertEquals("(&(osgi.wiring.package=com.foo)(version>=1.2.3)(a=1)(b=2))", filter);
	}

	public String	is	= "org.osgi.service.log.LogService;availability:=optional;multiple:=false";
	public String	es	= "org.osgi.service.cm.ConfigurationAdmin;"
		+ "service.description=\"Configuration Admin Service Specification 1.5 Implementation\";"
		+ "service.pid=\"org.osgi.service.cm.ConfigurationAdmin\";" + "service.vendor=\"Apache Software Foundation\","
		+ "org.apache.felix.cm.PersistenceManager;" + "service.description=\"Platform Filesystem Persistence Manager\";"
		+ "service.pid=\"org.apache.felix.cm.file.FilePersistenceManager\";"
		+ "service.vendor=\"Apache	Software Foundation\"";

	@Test
	public void testImportExportService() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		rb.addImportServices(new Parameters(is));
		rb.addExportServices(new Parameters(es));
		Resource build = rb.build();

		assertConfigAdminServices(build);

	}

	public void assertConfigAdminServices(Resource build) throws Exception {
		assertEquals(2, build.getCapabilities(ServiceNamespace.SERVICE_NAMESPACE)
			.size());
		List<Requirement> requireLog = build.getRequirements(ServiceNamespace.SERVICE_NAMESPACE);
		assertEquals(1, requireLog.size());

		RequirementBuilder rqb = new RequirementBuilder(ServiceNamespace.SERVICE_NAMESPACE);
		rqb.addFilter("(objectClass=org.osgi.service.cm.ConfigurationAdmin)");
		List<Capability> findProviders = ResourceUtils.findProviders(rqb.buildSyntheticRequirement(),
			build.getCapabilities(ServiceNamespace.SERVICE_NAMESPACE));
		assertEquals(1, findProviders.size());

		rqb = new RequirementBuilder(ServiceNamespace.SERVICE_NAMESPACE);
		rqb.addFilter("(objectClass=org.apache.felix.cm.PersistenceManager)");
		findProviders = ResourceUtils.findProviders(rqb.buildSyntheticRequirement(),
			build.getCapabilities(ServiceNamespace.SERVICE_NAMESPACE));
		assertEquals(1, findProviders.size());

		CapabilityBuilder rcb = new CapabilityBuilder(ServiceNamespace.SERVICE_NAMESPACE);
		rcb.addAttribute("objectClass", Arrays.asList("org.osgi.service.log.LogService"));
		findProviders = ResourceUtils.findProviders(requireLog.get(0),
			Collections.singleton(rcb.buildSyntheticCapability()));
		assertEquals(1, findProviders.size());
	}

	@Test
	public void testImportExportServiceFromManifest() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		File f = IO.getFile("testresources/manifest/configadmin-1.8.8.mf");
		Manifest m = new Manifest(new FileInputStream(f));
		Domain d = Domain.domain(m);
		rb.addManifest(d);

		assertConfigAdminServices(rb.build());

	}

	@Test
	public void testEscapeFilterValue() throws Exception {
		assertEquals("abc", CapReqBuilder.escapeFilterValue("abc"));
		assertEquals("abc\\\\", CapReqBuilder.escapeFilterValue("abc\\"));
		assertEquals("ab\\\\c", CapReqBuilder.escapeFilterValue("ab\\c"));
		assertEquals("a\\\\bc", CapReqBuilder.escapeFilterValue("a\\bc"));
		assertEquals("\\\\abc", CapReqBuilder.escapeFilterValue("\\abc"));

		assertEquals("abc\\(", CapReqBuilder.escapeFilterValue("abc("));
		assertEquals("ab\\(c", CapReqBuilder.escapeFilterValue("ab(c"));
		assertEquals("a\\(bc", CapReqBuilder.escapeFilterValue("a(bc"));
		assertEquals("\\(abc", CapReqBuilder.escapeFilterValue("(abc"));

		assertEquals("abc\\)", CapReqBuilder.escapeFilterValue("abc)"));
		assertEquals("ab\\)c", CapReqBuilder.escapeFilterValue("ab)c"));
		assertEquals("a\\)bc", CapReqBuilder.escapeFilterValue("a)bc"));
		assertEquals("\\)abc", CapReqBuilder.escapeFilterValue(")abc"));

		assertEquals("abc\\*", CapReqBuilder.escapeFilterValue("abc*"));
		assertEquals("ab\\*c", CapReqBuilder.escapeFilterValue("ab*c"));
		assertEquals("a\\*bc", CapReqBuilder.escapeFilterValue("a*bc"));
		assertEquals("\\*abc", CapReqBuilder.escapeFilterValue("*abc"));
	}

	@Test
	public void testEquals() throws Exception {

		assertResourceEquals(false, null);
		assertResourceEquals(false, null, "http://foo");
		assertResourceEquals(false, "http://foo");
		assertResourceEquals(true, "http://foo", "http://foo");
		assertResourceEquals(true, "http://foo", "http://foo", "http://bar");
		assertResourceEquals(true, "http://foo", "http://baz", "http://foo", "http://bar");
		assertResourceEquals(false, "http://foo", "http://baz", "http://bar", "http://foo");
		assertResourceEquals(false, "http://foo", "http://bar");
	}

	void assertResourceEquals(boolean expected, String a, String... b) throws Exception {

		ResourceBuilder rba = new ResourceBuilder();
		if (a != null) {
			CapReqBuilder cap = new CapReqBuilder("osgi.content");
			cap.addAttribute("url", a);
			cap.addAttribute("osgi.content", defaultSHA);
			rba.addCapability(cap);
		}

		ResourceBuilder rbb = new ResourceBuilder();
		int n = 0;
		for (String bb : b) {
			CapReqBuilder cap = new CapReqBuilder("osgi.content");
			cap.addAttribute("url", bb);
			String sha;
			if (b.length > 1 && b.length == n + 1)
				sha = alternativeSHA;
			else
				sha = defaultSHA;

			cap.addAttribute("osgi.content", sha);
			rbb.addCapability(cap);
			n++;
		}

		assertEquals(expected, rba.build()
			.equals(rbb.build()));
	}

	@Test
	public void testResourceEquals() throws Exception {
		String locations = ResourceTest.class.getResource("larger-repo.xml")
			.toString();
		Set<Resource> a = getResources(locations);
		Set<Resource> b = getResources(locations);
		assertEquals(a, b);
	}

	@Test
	public void testOSGiWiringHostBundle() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		rb.addManifest(Domain.domain(IO.getFile("../demo/generated/demo.jar")));
		Resource resource = rb.build();
		List<Capability> capabilities = resource.getCapabilities(HostNamespace.HOST_NAMESPACE);
		assertEquals(1, capabilities.size());
		Map<String, Object> attributes = capabilities.get(0)
			.getAttributes();
		assertTrue(attributes.containsKey(HostNamespace.HOST_NAMESPACE));
		assertTrue(attributes.containsKey(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
		assertEquals("demo", attributes.get(HostNamespace.HOST_NAMESPACE));
		assertNotNull(attributes.get(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
		assertEquals(0, resource.getRequirements(HostNamespace.HOST_NAMESPACE)
			.size());
	}

	@Test
	public void testOSGiWiringHostFragment() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		rb.addManifest(Domain.domain(IO.getFile("../demo-fragment/generated/demo-fragment.jar")));
		Resource resource = rb.build();
		assertEquals(0, resource.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE)
			.size());
		assertEquals(0, resource.getCapabilities(HostNamespace.HOST_NAMESPACE)
			.size());
		List<Requirement> requirements = resource.getRequirements(HostNamespace.HOST_NAMESPACE);
		assertEquals(1, requirements.size());
		Map<String, String> directives = requirements.get(0)
			.getDirectives();
		assertTrue(directives.containsKey(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
		String filter = directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		assertEquals("(&(osgi.wiring.host=demo)(bundle-version>=1.0.0)(!(bundle-version>=1.0.1)))", filter);
	}

	@Test
	public void testResourceToVersionedClause() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		rb.addManifest(Domain.domain(IO.getFile("../demo-fragment/generated/demo-fragment.jar")));
		Resource resource = rb.build();
		VersionedClause versionClause = ResourceUtils.toVersionClause(resource, "[===,==+)");
		StringBuilder sb = new StringBuilder();
		versionClause.formatTo(sb);
		assertEquals("demo-fragment;version='[1.0.0,1.0.1)'", sb.toString());
	}

	@Test
	public void testSnapshotResourceToVersionedClause() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		rb.addManifest(Domain.domain(IO.getFile("../demo-fragment/generated/demo-fragment.jar")));
		Attrs attrs = new Attrs();
		attrs.put("bnd.workspace.project", "demo-fragment");
		rb.addCapability(CapReqBuilder.createCapReqBuilder("bnd.workspace.project", attrs));
		Resource resource = rb.build();
		VersionedClause versionClause = ResourceUtils.toVersionClause(resource, "[===,==+)");
		StringBuilder sb = new StringBuilder();
		versionClause.formatTo(sb);
		assertEquals("demo-fragment;version=snapshot", sb.toString());
	}

	@Test
	public void testResourceSubstitutionPackages1() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		Parameters exports = new Parameters("com.foo;version=1.0");
		rb.addExportPackages(exports, "bundleA", new Version("1.2.3"));
		Parameters imports = new Parameters("com.foo");
		rb.addImportPackages(imports);
		Resource r = rb.build();

		Collection<PackageExpression> substitutionPackages = getSubstitutionPackages(r);
		assertThat(substitutionPackages).hasSize(1);
		assertEquals("[com.foo]", substitutionPackages.toString());

	}

	@Test
	public void testResourceSubstitutionPackages2() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		Parameters exports = new Parameters("com.foo;version=1.0");
		rb.addExportPackages(exports, "bundleA", new Version("1.2.3"));
		Parameters imports = new Parameters("com.foo;version='[1.0.1,1.3.2)'");
		rb.addImportPackages(imports);
		Resource r = rb.build();

		Collection<PackageExpression> substitutionPackages = getSubstitutionPackages(r);
		assertThat(substitutionPackages).hasSize(1);
		assertEquals("[com.foo; version=[1.0.1,1.3.2)]", substitutionPackages.toString());

	}

	@Test
	public void testResourceHasNoSubstitutionPackages() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		Parameters exports = new Parameters("com.foo;version=1.0");
		rb.addExportPackages(exports, "bundleA", new Version("1.2.3"));
		Parameters imports = new Parameters("com.bar;version='[1.0.1,1.3.2)'");
		rb.addImportPackages(imports);
		Resource r = rb.build();

		Collection<PackageExpression> substitutionPackages = getSubstitutionPackages(r);
		assertThat(substitutionPackages).isEmpty();

	}

	@Test
	public void testDirectiveWithoutQuotes() {

		Attrs attrs = new Attrs();
		attrs.put(Constants.VERSION_ATTRIBUTE, "[1.0.0,2.0.0)");
		attrs.put(Constants.RESOLUTION_DIRECTIVE, Resolution.OPTIONAL);

		ImportPattern importPattern = new ImportPattern("com.foo.*", attrs);
		StringBuilder sb = new StringBuilder();
		importPattern.formatTo(sb);
		assertEquals("com.foo.*;version='[1.0.0,2.0.0)';resolution:=optional", sb.toString());
	}

	@Test
	public void testAttributeWithQuotes() {

		Attrs attrs = new Attrs();
		attrs.put(Constants.VERSION_ATTRIBUTE, "[1.0.0,2.0.0)");
		attrs.put("attribute with spaces", "and value which both needs quoting because of spaces");

		ImportPattern importPattern = new ImportPattern("com.foo.*", attrs);
		StringBuilder sb = new StringBuilder();
		importPattern.formatTo(sb);
		assertEquals(
			"com.foo.*;version='[1.0.0,2.0.0)';'attribute with spaces'='and value which both needs quoting because of spaces'",
			sb.toString());
	}

	private Set<Resource> getResources(String locations) throws Exception {
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			Map<String, String> map = new HashMap<>();
			map.put("locations", locations);
			map.put("name", tmp.getName());
			map.put("cache", tmp.getAbsolutePath());
			repo.setProperties(map);
			Processor p = new Processor();
			p.addBasicPlugin(httpClient);
			repo.setRegistry(p);
			Requirement wildcard = ResourceUtils.createWildcardRequirement();
			Collection<Capability> caps = repo.findProviders(Collections.singleton(wildcard))
				.get(wildcard);
			return ResourceUtils.getResources(caps);
		}
	}
}
