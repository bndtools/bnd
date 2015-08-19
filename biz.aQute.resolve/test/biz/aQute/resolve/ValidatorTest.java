package biz.aQute.resolve;

import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.OSGI_CORE;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class ValidatorTest extends TestCase {

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
