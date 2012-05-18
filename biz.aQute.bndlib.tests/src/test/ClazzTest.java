package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Descriptors.PackageRef;

public class ClazzTest extends TestCase {

	/**
	 * Check if the class is not picking up false references when the
	 * CLass.forName name is constructed. The DeploymentAdminPermission.1.jclass
	 * turned out to use Class.forName with a name that was prefixed with a
	 * package from a property. bnd discovered the suffix
	 * (.DeploymentAdminPermission) but this ended up in the default package. So
	 * now the clazz parser tests that the name guessed for Class.forName must
	 * actually resemble a class name.
	 */

	public void testClassForNameFalsePickup() throws Exception {
		Analyzer a = new Analyzer();
		Clazz c = new Clazz(a, "", null);
		c.parseClassFile(new FileInputStream("jar/DeploymentAdminPermission.1.jclass"),
				new ClassDataCollector() {
				});
		Set<PackageRef> referred = c.getReferred();
		Descriptors d = new Descriptors();
		assertFalse(referred.contains(d.getPackageRef("")));
		System.out.println(referred);
	}

	/**
	 * Test the uncamel
	 */

	public void testUncamel() throws Exception {
		assertEquals("New", Clazz.unCamel("_new"));
		assertEquals("An XMLMessage", Clazz.unCamel("anXMLMessage"));
		assertEquals("A message", Clazz.unCamel("aMessage"));
		assertEquals("URL", Clazz.unCamel("URL"));
		assertEquals("A nice party", Clazz.unCamel("aNiceParty"));
	}
}
