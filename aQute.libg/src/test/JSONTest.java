package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.collections.*;
import aQute.lib.json.*;

@SuppressWarnings("unchecked") public class JSONTest extends TestCase {

	static public class A {
		public int i;
		public byte[] b;
		public String s;
	}
	
	public void testType() throws Exception {
		String test = "{'i':1, 'b':'CgAAAQ==','s':'ssss'}".replace('\'', '"');
		JSONCodec codec = new JSONCodec();
		A a = codec.decode(new StringReader(test), A.class);
		assertEquals(a.i, 1);
		assertTrue(Arrays.equals(a.b,new byte[] {10,0,0,1}));
		assertEquals(a.s, "ssss");
	}

	public void testEncodeMap() throws Exception {
		Map<String, Object> r = new HashMap<String, Object>();

		r.put("service.pid", "a");
		r.put("service.factoryPid", "a.1");
		r.put("string", "A STRING");
		r.put("bytes1", new byte[] { 127, 0, 0, 1 });
		r.put("bytesA", new byte[] { 10, 0, 0, 1 });
		r.put("strings", new String[] { "a", "b", "c", "d" });
		r.put("int", 6);
		r.put("float", 6.0);
		r.put("bool", true);

		JSONCodec codec = new JSONCodec();
		codec.encode(Map.class, r, System.out);

	}

	public void testDecodeMap() throws Exception {
		String test = "{'a':'A', 'b':'CgAAAQ==','c':[1,2]}".replace('\'', '"');
		JSONCodec codec = new JSONCodec();
		Map<String, ?> map = codec.decode(new StringReader(test), Map.class);
		assertEquals("A", map.get("a"));
		assertEquals("CgAAAQ==", map.get("b"));
		List<Object> list = new ExtList<Object>((Object[]) map.get("c"));
		assertTrue(list.contains("1"));
		assertTrue(list.contains("2"));
		System.out.println(map);
	}
}
