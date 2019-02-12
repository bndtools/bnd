package biz.aQute.resolve;

import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;

import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.OSGI_CORE;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;
import biz.aQute.resolve.ResolverValidator.Resolution;
import junit.framework.TestCase;

public class ValidatorTest extends TestCase {

	public void testMultipleSameRequirements() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_8);

			CapReqBuilder b1 = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
			b1.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, "javax.script");
			system.addCapability(b1.buildSyntheticCapability());

			CapReqBuilder b2 = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
			b2.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, "javax.script");
			system.addCapability(b2.buildSyntheticCapability());

			CapReqBuilder b3 = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
			b3.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, "javax.script");
			b3.addAttribute("version", "1.2");

			system.addCapability(b3.buildSyntheticCapability());

			system.addManifest(OSGI_CORE.R7_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/repo1.index.xml")
				.toURI());
			validator.validate();
			assertTrue(validator.check());
		}
	}

	public void testSmallWithSpecificResources() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_8);
			system.addManifest(OSGI_CORE.R7_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/repo1.index.xml")
				.toURI());
			List<Resource> resources = XMLResourceParser.getResources(IO.getFile("testdata/repo1.index.xml")
				.toURI());
			validator.validate(resources);
			assertTrue(validator.check());
		}
	}

	public void testSmall() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_8);
			system.addManifest(OSGI_CORE.R7_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/repo1.index.xml")
				.toURI());
			validator.validate();
			assertTrue(validator.check());
		}
	}

	public void testDelibarateFail() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_8);
			system.addManifest(OSGI_CORE.R7_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/repo5-broken.index.xml")
				.toURI());
			List<Resolution> resolutions = validator.validate();
			assertFalse(validator.check());
			assertEquals(1, resolutions.size());
			String message = resolutions.get(0).message;
			String expectedToContain = "missing requirement osgi.wiring.package;filter:='(osgi.wiring.package=org.apache.felix.gogo.api)'";
			assertTrue(String.format("expected to contain <%s> but was <%s>", expectedToContain, message),
				message.contains(expectedToContain));
		}
	}

	public void testDelibarateFailWithSpecificResources() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_8);
			system.addManifest(OSGI_CORE.R7_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			List<Resource> resources = XMLResourceParser.getResources(IO.getFile("testdata/repo5-broken.index.xml")
				.toURI());
			List<Resolution> resolutions = validator.validate(resources);
			assertFalse(validator.check());
			assertEquals(1, resolutions.size());
			String message = resolutions.get(0).message;
			String expectedToContain = "missing requirement osgi.wiring.package;filter:='(osgi.wiring.package=org.apache.felix.gogo.api)'";
			assertTrue(String.format("expected to contain <%s> but was <%s>", expectedToContain, message),
				message.contains(expectedToContain));
		}
	}

	public void testValidatingResourcesWithDocumentaryAttributes() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_8);
			system.addManifest(OSGI_CORE.R7_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);

			ResourcesRepository repository = new ResourcesRepository();
			ResourceBuilder builder = new ResourceBuilder();
			File file = IO.getFile("testdata/osgi.cmpn-4.3.0.jar");
			builder.addFile(file, file.toURI());
			repository.add(builder.build());
			builder = new ResourceBuilder();
			file = IO.getFile("testdata/org.apache.felix.framework-4.0.0.jar");
			builder.addFile(file, file.toURI());
			repository.add(builder.build());

			List<Resource> resources = repository.getResources();
			resources.removeIf(resource -> resource.getCapabilities(PACKAGE_NAMESPACE)
				.stream()
				.anyMatch(c -> Objects.equals(c.getAttributes()
					.get(PACKAGE_NAMESPACE), "org.osgi.framework")));
			validator.validateResources(repository, resources);
			assertTrue(validator.check());
		}
	}

	public void _testLarger() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_8);
			system.addManifest(OSGI_CORE.R6_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/larger-repo.xml")
				.toURI());
			validator.validate();
			assertTrue(validator.check());
		}
	}
}
