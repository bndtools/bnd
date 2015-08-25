package test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.deployer.repository.FixedIndexedRepo;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import junit.framework.TestCase;

public class ResourceTest extends TestCase {
	static String	defaultSHA		= "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	static String	alternativeSHA	= "AAAAAAAAAAAAFFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	static FilterParser	filterParser	= new FilterParser();

	public void testImportPackage() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		Requirement importPackage = rb.addImportPackage("com.foo",
				Attrs.create("version", "1.2.3").with("mandatory:", "a,b").with("a", "1").with("b", "2"));
		String filter = importPackage.getDirectives().get("filter");
		assertEquals("(&(osgi.wiring.package=com.foo)(version>=1.2.3)(a=1)(b=2))", filter);
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

		assertEquals(expected, rba.build().equals(rbb.build()));
	}

	public void testResourceEquals() throws MalformedURLException, URISyntaxException {
		String locations = ResourceTest.class.getResource("larger-repo.xml").toString();
		Set<Resource> a = getResources(locations);
		Set<Resource> b = getResources(locations);
		assertEquals(a, b);
	}

	private Set<Resource> getResources(String locations) throws MalformedURLException, URISyntaxException {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		repo.setLocations(locations);
		Requirement wildcard = ResourceUtils.createWildcardRequirement();
		Collection<Capability> caps = repo.findProviders(Collections.singleton(wildcard)).get(wildcard);
		return ResourceUtils.getResources(caps);
	}
}
