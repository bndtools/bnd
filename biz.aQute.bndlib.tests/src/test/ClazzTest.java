package test;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Descriptors.PackageRef;

public class ClazzTest extends TestCase {

	/**
	 * {@code java.lang.IllegalArgumentException: Expected IDENTIFIER: <S:Z>()V;}
	 * 
	 * This actually looks wrong since 
	 */
	
	public void test375() throws Exception {
		Analyzer a = new Analyzer();
		Clazz c = new Clazz(a,"",null);
		c.parseDescriptor("<S:[LFoo;>()V", Modifier.PUBLIC);
		c.parseDescriptor("<S:[Z>()V", Modifier.PUBLIC);
		c.parseDescriptor("<S:Z>()V", Modifier.PUBLIC);
		
	}
	
	
	/**
	 * Complaint from Groovy that the dynamic instruction fails.
	 * 
	 * <pre>
	 * [bndwrap] java.lang.ArrayIndexOutOfBoundsException: 15
	 * [bndwrap]     at aQute.bnd.osgi.Clazz.parseClassFile(Clazz.java:387)
	 * [bndwrap]     at aQute.bnd.osgi.Clazz.parseClassFile(Clazz.java:308)
	 * [bndwrap]     at aQute.bnd.osgi.Clazz.parseClassFileWithCollector(Clazz.java:297)
	 * [bndwrap]     at aQute.bnd.osgi.Clazz.parseClassFile(Clazz.java:286)
	 * [bndwrap]     at aQute.bnd.osgi.Analyzer.analyzeJar(Analyzer.java:1489)
	 * [bndwrap]     at aQute.bnd.osgi.Analyzer.analyzeBundleClasspath(Analyzer.java:1387)
	 * [bndwrap] Invalid class file: groovy/inspect/swingui/AstNodeToScriptVisitor.class
	 * [bndwrap] Exception: 15
	 * </pre>
	 */
	public static void testDynamicInstr() throws Exception {
		Analyzer a = new Analyzer();
		Clazz c = new Clazz(a, "", null);
		c.parseClassFile(new FileInputStream("jar/AstNodeToScriptVisitor.jclass"), new ClassDataCollector() {});
		Set<PackageRef> referred = c.getReferred();
		Descriptors d = new Descriptors();
		assertFalse(referred.contains(d.getPackageRef("")));
		System.out.println(referred);
	}

	/**
	 * Check if the class is not picking up false references when the
	 * CLass.forName name is constructed. The DeploymentAdminPermission.1.jclass
	 * turned out to use Class.forName with a name that was prefixed with a
	 * package from a property. bnd discovered the suffix
	 * (.DeploymentAdminPermission) but this ended up in the default package. So
	 * now the clazz parser tests that the name guessed for Class.forName must
	 * actually resemble a class name.
	 */

	public static void testClassForNameFalsePickup() throws Exception {
		Analyzer a = new Analyzer();
		Clazz c = new Clazz(a, "", null);
		c.parseClassFile(new FileInputStream("jar/DeploymentAdminPermission.1.jclass"), new ClassDataCollector() {});
		Set<PackageRef> referred = c.getReferred();
		Descriptors d = new Descriptors();
		assertFalse(referred.contains(d.getPackageRef("")));
		System.out.println(referred);
	}

	/**
	 * Test the uncamel
	 */

	public static void testUncamel() throws Exception {
		assertEquals("New", Clazz.unCamel("_new"));
		assertEquals("An XMLMessage", Clazz.unCamel("anXMLMessage"));
		assertEquals("A message", Clazz.unCamel("aMessage"));
		assertEquals("URL", Clazz.unCamel("URL"));
		assertEquals("A nice party", Clazz.unCamel("aNiceParty"));
	}
}
