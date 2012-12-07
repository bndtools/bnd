package test;

import java.util.*;

import junit.framework.*;
import aQute.bnd.header.*;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.osgi.*;
import aQute.bnd.version.*;

public class ParseHeaderTest extends TestCase {

	public static void testPropertiesSimple() {
		Map<String,String> p = OSGiHeader.parseProperties("a=1, b=\"3   3\", c=c");
		assertEquals("1", p.get("a"));
		assertEquals("3   3", p.get("b"));
		assertEquals("c", p.get("c"));
	}

	public static void testClauseName() {
		assertNames("a,b,c;", new String[] {
				"a", "b", "c"
		});
		assertNames("a,b,c", new String[] {
				"a", "b", "c"
		});
		assertNames("a;x=0,b;x=0,c;x=0", new String[] {
				"a", "b", "c"
		});
		assertNames("a;b;c;x=0", new String[] {
				"a", "b", "c"
		});
		assertNames(",", new String[] {}, null, "Empty clause, usually caused");
		assertNames("a;a,b", new String[] {
				"a", "a~", "b"
		}, null, "Duplicate name a used in header");
		assertNames("a;x=0;b", new String[] {
				"a", "b"
		}, "Header contains name field after attribute or directive", null);
		assertNames("a;x=0;x=0,b", new String[] {
				"a", "b"
		}, null, "Duplicate attribute/directive name");
		assertNames("a;;;,b", new String[] {
				"a", "b"
		});
		assertNames(",,a,,", new String[] {
			"a"
		}, null, "Empty clause, usually caused by repeating");
		assertNames(",a", new String[] {
			"a"
		}, null, "Empty clause, usually caused");
		assertNames(",a,b,c,", new String[] {
				"a", "b", "c"
		}, null, "Empty clause, usually caused");
		assertNames("a,b,c,", new String[] {
				"a", "b", "c"
		}, null, "Empty clause, usually caused");
		assertNames("a,b,,c", new String[] {
				"a", "b", "c"
		}, null, "Empty clause, usually caused");
	}

	static void assertNames(String header, String[] keys) {
		assertNames(header, keys, null, null);
	}

	static void assertNames(String header, String[] keys, String expectedError, String expectedWarning) {
		Processor p = new Processor();
		p.setPedantic(true);
		Parameters map = Processor.parseHeader(header, p);
		for (String key : keys)
			assertTrue(map.containsKey(key));

		assertEquals(keys.length, map.size());
		if (expectedError != null) {
			System.err.println(p.getErrors());
			assertTrue(p.getErrors().size() > 0);
			assertTrue(p.getErrors().get(0).indexOf(expectedError) >= 0);
		} else
			assertEquals(0, p.getErrors().size());
		if (expectedWarning != null) {
			System.err.println(p.getWarnings());
			assertTrue(p.getWarnings().size() > 0);
			String w = p.getWarnings().get(0);
			assertTrue(w.startsWith(expectedWarning));
		} else
			assertEquals(0, p.getWarnings().size());
	}

	public static void testSimple() {
		String s = "a;a=a1;b=a2;c=a3, b;a=b1;b=b2;c=b3, c;d;e;a=x1";
		Parameters map = Processor.parseHeader(s, null);
		assertEquals(5, map.size());

		Map<String,String> a = map.get("a");
		assertEquals("a1", a.get("a"));
		assertEquals("a2", a.get("b"));
		assertEquals("a3", a.get("c"));

		Map<String,String> d = map.get("d");
		assertEquals("x1", d.get("a"));

		Map<String,String> e = map.get("e");
		assertEquals(e, d);

		System.err.println(map);
	}
	
	public static void testParseMultiValueAttribute() {
		String s = "capability;foo:List<String>=\"MacOSX,Mac OS X\";version:List<Version>=\"1.0, 2.0, 2.1\"";
		Parameters map = Processor.parseHeader(s, null);
		
		Attrs attrs = map.get("capability");
		
		assertEquals(Type.STRINGS, attrs.getType("foo"));
		List<String> foo = (List<String>) attrs.getTyped("foo");
		assertEquals(2, foo.size());
		assertEquals("MacOSX", foo.get(0));
		assertEquals("Mac OS X", foo.get(1));
		
		assertEquals(Type.VERSIONS, attrs.getType("version"));
		List<Version> version = (List<Version>) attrs.getTyped("version");
		assertEquals(3, version.size());
		assertEquals(new Version(1), version.get(0));
		assertEquals(new Version(2), version.get(1));
		assertEquals(new Version(2, 1), version.get(2));
	}
}
