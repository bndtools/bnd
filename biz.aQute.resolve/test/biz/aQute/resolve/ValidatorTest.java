package biz.aQute.resolve;

import org.osgi.framework.namespace.PackageNamespace;

import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.OSGI_CORE;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;
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

			system.addManifest(OSGI_CORE.R6_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/repo1.index.xml").toURI());
			validator.validate();
			assertTrue(validator.check());
		}
	}

	public void testSmall() throws Exception {
		try (ResolverValidator validator = new ResolverValidator();) {
			ResourceBuilder system = new ResourceBuilder();
			system.addEE(EE.JavaSE_1_8);
			system.addManifest(OSGI_CORE.R6_0_0.getManifest());
			validator.setSystem(system.build());
			validator.setTrace(true);
			validator.addRepository(IO.getFile("testdata/repo1.index.xml").toURI());
			validator.validate();
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
			validator.addRepository(IO.getFile("testdata/larger-repo.xml").toURI());
			validator.validate();
			assertTrue(validator.check());
		}
	}
}
