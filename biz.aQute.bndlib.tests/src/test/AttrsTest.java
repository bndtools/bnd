package test;

import java.util.Arrays;
import java.util.List;

import aQute.bnd.header.Attrs;
import aQute.bnd.version.Version;
import junit.framework.TestCase;

public class AttrsTest extends TestCase {

	public void testAttrs() {
		Attrs attr = new Attrs();
		attr.putTyped("xyz", new String[] {
			"a", ",b"
		});
		assertEquals("a,\\,b", attr.get("xyz"));
		assertEquals("xyz:List<String>=\"a,\\,b\"", attr.toString());
	}

	public void testFloats() {
		Attrs attr = new Attrs();
		attr.putTyped("double", 3.1D);
		attr.putTyped("float", 3.1f);
		assertEquals(3.1D, attr.getTyped("double"));
		assertEquals(3.1D, attr.getTyped("float"));
		assertEquals("double:Double=\"3.1\";float:Double=\"3.1\"", attr.toString());
	}

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

}
