package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.osgi.*;
import aQute.libg.header.*;

public class NoUsesTest extends TestCase {

	/*
	 * Check if we explicitly set a uses directive, prepend the calculated
	 * but the calculated is empty. This should remove the extraneuous comma
	 */
	public void testExplicitUsesWithPrependZeroUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Private-Package", "org.osgi.framework");
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"<<USES>>,not.used\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertEquals("not.used", uses);
	}
	
	/*
	 * Check if we explicitly set a uses directive, but append it
	 * with the calculated directive
	 */
	public void testExplicitUsesWithAppend() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"not.used,<<USES>>\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertTrue("not.used", uses.indexOf("not.used")>=0);
		assertTrue("org.osgi.framework", uses.indexOf("org.osgi.framework")>=0);
	}

	/*
	 * Check if we explicitly set a uses directive, append the calculated
	 * but the calculated is empty. This should remove the extraneuous comma
	 */
	public void testExplicitUsesWithAppendZeroUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Private-Package", "org.osgi.framework");
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"not.used,<<USES>>\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertEquals("not.used", uses);
	}
	
	/*
	 * Check if we explicitly set a uses directive, but append it
	 * with the calculated directive
	 */
	public void testExplicitUsesWithPrepend() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"<<USES>>,not.used\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertTrue("not.used", uses.indexOf("not.used")>=0);
		assertTrue("org.osgi.framework", uses.indexOf("org.osgi.framework")>=0);
	}
	/*
	 * Check if we explicitly set a uses directive
	 */
	public void testExplicitUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"not.used\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertEquals("not.used", uses);
	}
	
	public void testExportedUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker, org.osgi.framework");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertEquals("org.osgi.framework", uses);
	}
	

	public void testPrivateUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Private-Package", "org.osgi.framework");
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertNull("org.osgi.framework", uses);
	}
	
	public void testHasUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "test.activator");
		String uses = findUses(bmaker, "test.activator");
		assertEquals("org.osgi.framework", uses);
	}

	public void testNoUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "test.activator");
		bmaker.setProperty("-nouses", "true");
		String uses = findUses(bmaker, "test.activator");
		assertNull("org.osgi.framework", uses);
	}


	
	String findUses(Builder bmaker, String pack ) throws Exception {
			File cp[] = { new File("bin"), new File("jar/osgi.jar") };
			bmaker.setClasspath(cp);
			Jar jar = bmaker.build();
			assertOk(bmaker);
			String exports = jar.getManifest().getMainAttributes().getValue("Export-Package");
			assertNotNull("exports", exports );
			Parameters map = Processor.parseHeader(exports, null);
			if ( map == null )
				return null;
			
			Map<String,String> clause = map.get(pack);
			if ( clause == null )
				return null;
			
			return (String) clause.get("uses:");			
	}
	
	void assertOk(Analyzer bmaker) throws Exception {
		System.out.println(bmaker.getErrors());
		System.out.println(bmaker.getWarnings());
		bmaker.getJar().getManifest().write(System.out);
		assertEquals(0,bmaker.getErrors().size());
		assertEquals(0,bmaker.getWarnings().size());
		
	}
}
