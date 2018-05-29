package aQute.bnd.osgi.repository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class XMLResourceGeneratorTest extends TestCase {
	private static final Requirement	WILDCARD	= ResourceUtils.createWildcardRequirement();
	File								tmp			= IO.getFile("generated/tmp");

	{
		IO.delete(tmp);
		tmp.mkdirs();
	}

	public void testBasic() throws URISyntaxException, Exception {
		Repository repository = getTestRepository();
		File location = new File(tmp, "index.xml");
		new XMLResourceGenerator().name("test")
			.repository(repository)
			.save(location);

		Repository other = getRepository(location.toURI());
		Map<Requirement, Collection<Capability>> findProviders = other.findProviders(Collections.singleton(WILDCARD));

		Set<Resource> resources = ResourceUtils.getResources(findProviders.get(WILDCARD));
		assertEquals(1, resources.size());
		Resource r = resources.iterator()
			.next();
		assertEquals(
			"http://macbadge-updates.s3.amazonaws.com/plugins/name.njbartlett.eclipse.macbadge_1.0.0.201110100042.jar",
			ResourceUtils.getContentCapability(r)
				.url()
				.toString());
	}

	public void testFillInDefaultCapabilityAttribs() throws Exception {
		// Identity capability with no version or type
		Capability idCap = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, "foo")
			.buildSyntheticCapability();

		// Create resource and repository
		Resource resource = new ResourceBuilder()
			.addCapability(idCap)
			.build();
		ResourcesRepository repo = new ResourcesRepository(resource);
		
		// Generate XML
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new XMLResourceGenerator().name("test")
			.repository(repo)
			.save(out);
		String xml = out.toString();

		// Validate
		assertTrue("XML did not contain version attribute. Actual XML was:\n" + xml,
			xml.contains("attribute name=\"version\" value=\"0.0.0\" type=\"Version\""));
		assertTrue("XML did not contain type attribute. Actual XML was:\n" + xml,
			xml.contains("attribute name=\"type\" value=\"osgi.bundle\""));
	}

	public void testErrorOnMissingCapabilityAttribs() throws Exception {
		// Package capability with no bundle-symbolic-name attribute
		Capability pkgCap = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE)
			.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, "org.example")
			.buildSyntheticCapability();

		// Create resource and repository
		Capability idCap = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, "org.example")
			.buildSyntheticCapability();
		Resource resource = new ResourceBuilder().addCapability(idCap)
			.addCapability(pkgCap)
			.build();
		ResourcesRepository repo = new ResourcesRepository(resource);

		// Try to generate XML
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			new XMLResourceGenerator().name("test")
				.repository(repo)
				.save(out);
			String xml = out.toString();
			fail("Should have thrown IllegalArgumentException but produced XML:\n" + xml);
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	private Repository getTestRepository() throws URISyntaxException, Exception {
		return getRepository(XMLResourceGeneratorTest.class.getResource("data/macbadge.xml")
			.toURI());
	}

	private Repository getRepository(URI uri) throws IOException, Exception {
		try (XMLResourceParser p = new XMLResourceParser(uri);) {

			List<Resource> parse = p.parse();
			assertEquals(1, parse.size());
			return new ResourcesRepository(parse);
		}
	}

}
