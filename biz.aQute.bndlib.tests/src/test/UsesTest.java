package test;

import java.io.File;
import java.io.IOException;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import junit.framework.TestCase;

public class UsesTest extends TestCase {

	public void testUsesExtend() throws Exception {
		checkUses("test.uses.extend");
	}

	public void testUsesGenericExtend() throws Exception {
		checkUses("test.uses.generic.extend");
	}

	public void testUsesConstructor() throws Exception {
		checkUses("test.uses.constructor");
	}

	public void testUsesImplement() throws Exception {
		checkUses("test.uses.implement");
	}

	public void testUsesGenericImplement() throws Exception {
		checkUses("test.uses.generic.implement");
	}

	public void testUsesParam() throws Exception {
		checkUses("test.uses.param");
	}

	public void testUsesGenericParam() throws Exception {
		checkUses("test.uses.generic.param");
	}

	public void testUsesRValue() throws Exception {
		checkUses("test.uses.rvalue");
	}

	public void testUsesGenericRValue() throws Exception {
		checkUses("test.uses.generic.rvalue");
	}

	public void testUsesException() throws Exception {
		checkUses("test.uses.exception");
	}

	public void testUsesField() throws Exception {
		checkUses("test.uses.field");
	}

	public void testUsesAnnotation() throws Exception {
		checkUses("test.uses.annotation", "test.uses.annotation.annotation");
	}

	private void checkUses(String export) throws IOException, Exception {
		String uses = "javax.security.auth.callback";
		checkUses(export, uses);
	}

	private void checkUses(String export, String uses) throws IOException, Exception {
		Builder a = new Builder();
		a.addClasspath(new File("bin"));
		a.setExportPackage(export);
		a.setProperty("build", "123");
		Jar jar = a.build();
		assertTrue(a.check());
		Manifest m = jar.getManifest();
		m.write(System.err);
		Domain d = Domain.domain(m);

		Parameters parameters = d.getExportPackage();
		Attrs attrs = parameters.get(export);
		assertNotNull(attrs);
		assertEquals(uses, attrs.get("uses:"));
	}

}
