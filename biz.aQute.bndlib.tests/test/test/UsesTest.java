package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;

@SuppressWarnings("resource")
public class UsesTest {

	@Test
	public void testUsesExtend() throws Exception {
		checkUses("test.uses.extend");
	}

	@Test
	public void testUsesGenericExtend() throws Exception {
		checkUses("test.uses.generic.extend");
	}

	@Test
	public void testUsesConstructor() throws Exception {
		checkUses("test.uses.constructor");
	}

	@Test
	public void testUsesImplement() throws Exception {
		checkUses("test.uses.implement");
	}

	@Test
	public void testUsesGenericImplement() throws Exception {
		checkUses("test.uses.generic.implement");
	}

	@Test
	public void testUsesParam() throws Exception {
		checkUses("test.uses.param");
	}

	@Test
	public void testUsesGenericParam() throws Exception {
		checkUses("test.uses.generic.param");
	}

	@Test
	public void testUsesRValue() throws Exception {
		checkUses("test.uses.rvalue");
	}

	@Test
	public void testUsesGenericRValue() throws Exception {
		checkUses("test.uses.generic.rvalue");
	}

	@Test
	public void testUsesException() throws Exception {
		checkUses("test.uses.exception");
	}

	@Test
	public void testUsesField() throws Exception {
		checkUses("test.uses.field");
	}

	@Test
	public void testUsesAnnotation() throws Exception {
		checkUses("test.uses.annotation", "test.uses.annotation.annotation");
	}

	@Test
	public void testUsesMulti() throws Exception {
		// Check for consistent ordering
		for (int i = 0; i < 10; i++)
			checkUses("test.uses.multi", "javax.security.auth.callback,javax.sql");
	}

	private static void checkUses(String export) throws IOException, Exception {
		String uses = "javax.security.auth.callback";
		checkUses(export, uses);
	}

	private static void checkUses(String export, String uses) throws IOException, Exception {
		Builder a = new Builder();
		a.addClasspath(new File("bin_test"));
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
