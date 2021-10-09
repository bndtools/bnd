package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;

public class NoUsesTest {

	/*
	 * Check if we explicitly set a uses directive, prepend the calculated but
	 * the calculated is empty. This should remove the extraneuous comma
	 */
	@Test
	public void testExplicitUsesWithPrependZeroUses() throws Exception {
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
	@Test
	public void testExplicitUsesWithAppend() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"not.used,<<USES>>\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertTrue(uses.contains("not.used"), "not.used");
		assertTrue(uses.contains("org.osgi.framework"), "org.osgi.framework");
	}

	/*
	 * Check if we explicitly set a uses directive, append the calculated but
	 * the calculated is empty. This should remove the extraneuous comma
	 */
	@Test
	public void testExplicitUsesWithAppendZeroUses() throws Exception {
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
	@Test
	public void testExplicitUsesWithPrepend() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"<<USES>>,not.used\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertTrue(uses.contains("not.used"), "not.used");
		assertTrue(uses.contains("org.osgi.framework"), "org.osgi.framework");
	}

	/*
	 * Check if we explicitly set a uses directive
	 */
	@Test
	public void testExplicitUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker;uses:=\"not.used\"");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertEquals("not.used", uses);
	}

	@Test
	public void testExportedUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker, org.osgi.framework");
		String uses = findUses(bmaker, "org.osgi.util.tracker");
		assertEquals("org.osgi.framework", uses);
	}

	@Test
	public void testPrivateUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Private-Package", "org.osgi.framework");
		bmaker.setProperty("Export-Package", "org.osgi.util.tracker");
		String uses = findUses(bmaker, "org.osgi.util.tracker", "has 1,  private");
		assertNull(uses, "org.osgi.framework");
	}

	@Test
	public void testHasUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "test.activator");
		String uses = findUses(bmaker, "test.activator");
		Set<String> usesSet = new HashSet<>(Arrays.asList(uses.split(",")));
		assertTrue(usesSet.contains("org.osgi.service.component"));
		assertTrue(usesSet.contains("org.osgi.framework"));
	}

	@Test
	public void testNoUses() throws Exception {
		Builder bmaker = new Builder();
		bmaker.setProperty("Export-Package", "test.activator");
		bmaker.setProperty("-nouses", "true");
		String uses = findUses(bmaker, "test.activator");
		assertNull(uses, "org.osgi.framework");
	}

	String findUses(Builder bmaker, String pack, String... ignore) throws Exception {
		File cp[] = {
			new File("bin_test"), IO.getFile("jar/osgi.jar")
		};
		bmaker.setClasspath(cp);
		Jar jar = bmaker.build();
		assertTrue(bmaker.check(ignore));
		String exports = jar.getManifest()
			.getMainAttributes()
			.getValue("Export-Package");
		assertNotNull(exports, "exports");
		Parameters map = Processor.parseHeader(exports, null);
		if (map == null)
			return null;

		Map<String, String> clause = map.get(pack);
		if (clause == null)
			return null;

		return clause.get("uses:");
	}

}
