package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import aQute.bnd.header.Attrs;
import aQute.bnd.version.Version;

public class AttrsTest {

	@Test
	public void testAttrs() {
		Attrs attr = new Attrs();
		attr.putTyped("xyz", new String[] {
			"a", ",b"
		});
		assertEquals("a,\\,b", attr.get("xyz"));
		assertEquals("xyz:List<String>=\"a,\\,b\"", attr.toString());
	}

	@Test
	public void testFloats() {
		Attrs attr = new Attrs();
		attr.putTyped("double", 3.1D);
		attr.putTyped("float", 3.1f);
		assertEquals(3.1D, attr.getTyped("double"));
		assertEquals(3.1D, attr.getTyped("float"));
		assertEquals("double:Double=\"3.1\";float:Double=\"3.1\"", attr.toString());
	}

	@Test
	public void testNumbers() {
		Attrs attr = new Attrs();
		attr.putTyped("long", 3L);
		attr.putTyped("int", 3);
		attr.putTyped("short", (short) 3);
		attr.putTyped("byte", (byte) 3);
		assertEquals(3L, attr.getTyped("long"));
		assertEquals(3L, attr.getTyped("int"));
		assertEquals(3L, attr.getTyped("short"));
		assertEquals(3L, attr.getTyped("byte"));
		assertEquals("long:Long=3;int:Long=3;short:Long=3;byte:Long=3", attr.toString());
	}

	@Test
	public void testVersion() {
		Attrs attr = new Attrs();
		attr.putTyped("version", new Version("1.2.3"));
		attr.putTyped("versions", new Version[] {
			new Version("1.2.3"), new Version("2.1.0")
		});

		assertEquals("List<Version>", attr.getType("versions")
			.toString());
		assertEquals(new Version("1.2.3"), attr.getTyped("version"));
		Object a = attr.getTyped("versions");
		List<Version> b = Arrays.asList(new Version("1.2.3"), new Version("2.1.0"));
		assertEquals(a.toString(), b.toString());
		assertEquals("1.2.3", attr.get("version"));
		assertEquals("1.2.3,2.1.0", attr.get("versions"));
		assertEquals("version:Version=\"1.2.3\";versions:List<Version>=\"1.2.3,2.1.0\"", attr.toString());
	}

	/**
	 * Test for directive ordering consistency issue.
	 * This reproduces the problem where merging an Attrs with itself
	 * can change the ordering of attributes and directives.
	 *
	 * The issue occurs when:
	 * 1. An Attrs object is created with attributes/directives in one order
	 * 2. It's merged with itself (simulating Analyzer behavior)
	 * 3. The resulting order may be different due to LinkedHashMap insertion order
	 */
	@Test
	public void testDirectiveOrderingConsistency() {
		// Create an Attrs object simulating an Export-Package clause
		// with version attribute and uses directive in a specific order
		Attrs original = new Attrs();

		// Add in the order that would appear in a fresh manifest generation
		original.put("uses:", "org.eclipse.aether.artifact,org.eclipse.aether.collection");
		original.put("version", "2.0.11");

		// Verify initial order
		String originalString = original.toString();
		System.out.println("Original: " + originalString);

		// When the manifest is loaded, some attributes are already populated
		Attrs merged = new Attrs();
		merged.put("version", "2.0.11");

		// Add in the order that would appear in a fresh manifest generation
		merged.put("uses:", "org.eclipse.aether.artifact,org.eclipse.aether.collection");
		merged.put("version", "2.0.11");

		String mergedString = merged.toString();
		System.out.println("Merged:   " + mergedString);

		// The order should remain consistent after merging with itself
		assertEquals(originalString, mergedString,
			"Merging Attrs with itself should preserve attribute/directive ordering");
	}

}
