package test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class NoUsesTest extends TestCase {

	/*
	 * Check if we explicitly set a uses directive, prepend the calculated but
	 * the calculated is empty. This should remove the extraneuous comma
	 */
	public static void testExplicitUsesWithPrependZeroUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Private-Package", "org.osgi.framework");
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"<<USES>>,not.used\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker", "has 1,  private");
		assertEquals("not.used", uses);
	}

	/*
	 * Check if we explicitly set a uses directive, but append it with the
	 * calculated directive
	 */
	public static void testExplicitUsesWithAppend() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"not.used,<<USES>>\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertTrue("not.used", uses.contains("not.used"));
		assertTrue("org.osgi.framework", uses.contains("org.osgi.framework"));
	}

	/*
	 * Check if we explicitly set a uses directive, append the calculated but
	 * the calculated is empty. This should remove the extraneuous comma
	 */
	public static void testExplicitUsesWithAppendZeroUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Private-Package", "org.osgi.framework");
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"not.used,<<USES>>\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker", "has 1,  private");
		assertEquals("not.used", uses);
	}

	/*
	 * Check if we explicitly set a uses directive, but append it with the
	 * calculated directive
	 */
	public static void testExplicitUsesWithPrepend() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"<<USES>>,not.used\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertTrue("not.used", uses.contains("not.used"));
		assertTrue("org.osgi.framework", uses.contains("org.osgi.framework"));
	}

	/*
	 * Check if we explicitly set a uses directive
	 */
	public static void testExplicitUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"not.used\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertEquals("not.used", uses);
	}

	public static void testExportedUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker, org.osgi.framework");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertEquals("org.osgi.framework", uses);
	}

	public static void testPrivateUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Private-Package", "org.osgi.framework");
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker");
		String uses = findUses(bmaker, "org.osgi.util.tracker", "has 1,  private");
		assertNull("org.osgi.framework", uses);
	}

	public static void testHasUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "test.activator");
		String uses = findUses(bmaker, "test.activator");
		Set<String> usesSet = new HashSet<>(Arrays.asList(uses.split(",")));
		assertTrue(usesSet.contains("org.osgi.service.component"));
		assertTrue(usesSet.contains("org.osgi.framework"));
	}

	public static void testNoUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "test.activator");
		bmaker.setProperty("-nouses", "true");
		String uses = findUses(bmaker, "test.activator");
		assertNull("org.osgi.framework", uses);
	}

	static String findUses(Builder bmaker, String pack, String... ignore) throws Exception {
		File cp[] = {
			new File("bin_test"), IO.getFile("jar/osgi.jar")
		};
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		assertTrue(bmaker.check(ignore));
		String exports = jar.getManifest()
			.getMainAttributes()
			.getValue("Export-Package");
		assertNotNull("exports", exports);
		Parameters map = Processor.parseHeader(exports, null);
		if (map == null)
			return null;

		Map<String, String> clause = map.get(pack);
		if (clause == null)
			return null;

		return clause.get("uses:");
	}

}
