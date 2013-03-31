package test;

import java.io.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;

public class UsesTest extends TestCase {

	public static void testUsesExtend() throws Exception {
		checkUses("test.uses.extend");
	}

	public static void testUsesGenericExtend() throws Exception {
		checkUses("test.uses.generic.extend");
	}

	public static void testUsesConstructor() throws Exception {
		checkUses("test.uses.constructor");
	}

	public static void testUsesImplement() throws Exception {
		checkUses("test.uses.implement");
	}

	public static void testUsesGenericImplement() throws Exception {
		checkUses("test.uses.generic.implement");
	}

	public static void testUsesParam() throws Exception {
		checkUses("test.uses.param");
	}

	public static void testUsesGenericParam() throws Exception {
		checkUses("test.uses.generic.param");
	}

	public static void testUsesRValue() throws Exception {
		checkUses("test.uses.rvalue");
	}

	public static void testUsesGenericRValue() throws Exception {
		checkUses("test.uses.generic.rvalue");
	}

	public static void testUsesException() throws Exception {
		checkUses("test.uses.exception");
	}

	public static void testUsesField() throws Exception {
		checkUses("test.uses.field");
	}

	public static void testUsesAnnotation() throws Exception {
		checkUses("test.uses.annotation", "test.uses.annotation.annotation");
	}
	
	public static void testUsesMulti() throws Exception {
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
