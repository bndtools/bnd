package test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class ResourceTest extends TestCase {
	static String		defaultSHA		= "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	static String		alternativeSHA	= "AAAAAAAAAAAAFFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	static FilterParser	filterParser	= new FilterParser();

	public void testImportPackage() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		Requirement importPackage = rb.addImportPackage("com.foo", Attrs.create("version", "1.2.3")
			.with("mandatory:", "a,b")
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
		rcb.addAttribute("objectClass", "org.osgi.service.log.LogService");
		findProviders = ResourceUtils.findProviders(requireLog.get(0),
			Collections.singleton(rcb.buildSyntheticCapability()));
		assertEquals(1, findProviders.size());
	}

	public void testImportExportServiceFromManifest() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		File f = IO.getFile("testresources/manifest/configadmin-1.8.8.mf");
		Manifest m = new Manifest(new FileInputStream(f));
		Domain d = Domain.domain(m);
		rb.addManifest(d);

		assertConfigAdminServices(rb.build());

	}

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

	public void testResourceEquals() throws Exception {
		String locations = ResourceTest.class.getResource("larger-repo.xml")
			.toString();
		Set<Resource> a = getResources(locations);
		Set<Resource> b = getResources(locations);
		assertEquals(a, b);
	}

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
		assertEquals("(&(osgi.wiring.host=demo)(&(bundle-version>=1.0.0)(!(bundle-version>=1.0.1))))", filter);
	}

	public void testResourceToVersionedClause() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		rb.addManifest(Domain.domain(IO.getFile("../demo-fragment/generated/demo-fragment.jar")));
		Resource resource = rb.build();
		VersionedClause versionClause = ResourceUtils.toVersionClause(resource, "[===,==+)");
		StringBuilder sb = new StringBuilder();
		versionClause.formatTo(sb);
		assertEquals("demo-fragment;version='[1.0.0,1.0.1)'", sb.toString());
	}

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

	private Set<Resource> getResources(String locations) throws Exception {
		try (OSGiRepository repo = new OSGiRepository(); HttpClient httpClient = new HttpClient()) {
			Map<String, String> map = new HashMap<>();
			map.put("locations", locations);
			map.put("name", getName());
			map.put("cache",
				new File("generated/tmp/test/cache/" + getClass().getName() + "/" + getName()).getAbsolutePath());
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
