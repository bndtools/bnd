package test;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import junit.framework.*;
import aQute.lib.converter.*;
import aQute.lib.converter.Converter.Hook;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;
import aQute.libg.map.*;

@SuppressWarnings({
		"unchecked", "rawtypes"
})
public class ConverterTest extends TestCase {
	Converter	converter	= new Converter();

	
	interface M {
		String a();
		int b();
		int c();
		double d();
	}
	public void testMap() throws Exception {
		Map<String,String> map = MAP.$("a", "A").$("b", "2");
		M m = converter.convert(M.class, map);
		assertEquals("A", m.a());
		assertEquals(2, m.b());
		assertEquals(0, m.c());
		assertEquals(0d, m.d());
	}
	
	
	public void testTypeRef() throws Exception {
		Map<String,Integer> f;
		Type type = (new TypeReference<Map<String,Integer>>() {}).getType();
		assertTrue( type instanceof ParameterizedType);
		ParameterizedType ptype  = (ParameterizedType) type; 
		assertEquals( Map.class, ptype.getRawType());
		assertEquals( String.class, ptype.getActualTypeArguments()[0]);
		assertEquals( Integer.class, ptype.getActualTypeArguments()[1]);
		
		Map<Integer,String> m = MAP.$(1, "1").$(2,"2");
		f = converter.convert(new TypeReference<Map<String,Integer>>(){}, m);
		
		assertEquals( f.get("1"),(Integer) 1);
		assertEquals( f.get("2"),(Integer) 2);
	}
	
	
	public void hookTest() throws Exception {
		Converter converter = new Converter().hook(File.class, new Hook() {

			public Object convert(Type dest, Object o) {
				if (o instanceof String) {
					return IO.getFile(new File(""), o.toString());
				}
				return null;
			}

		});
		assertEquals(new Integer(6), converter.convert(Integer.class, "6"));
		assertEquals(new File("src").getAbsoluteFile(), converter.convert(File.class, "src"));
	}

	/**
	 * Test map to object
	 */

	public class DD {

	}

	public class D {
		int		n;
		String	s;

	}

	public void testMap2Object() throws Exception {

	}

	/**
	 * Digests as byte[]
	 * 
	 * @throws Exception
	 */

	public void testDigest() throws Exception {
		Digester<SHA1> digester = SHA1.getDigester();
		IO.copy("ABC".getBytes(), digester);
		SHA1 digest = digester.digest();
		byte[] out = converter.convert(byte[].class, digest);
		assertTrue(Arrays.equals(digest.digest(), out));

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		bout.write("Hello World".getBytes());
		assertTrue(Arrays.equals("Hello World".getBytes(), converter.convert(byte[].class, bout)));

	}

	/**
	 * Map a string to a char[], Character[], or Collection<Character>
	 */

	public void testCharacters() throws Exception {
		assertTrue(Arrays.equals(new char[] {
				'A', 'B', 'C'
		}, converter.convert(char[].class, "ABC")));
		assertEquals("ABC", converter.convert(String.class, new char[] {
				'A', 'B', 'C'
		}));

	}

	/**
	 * Test string to primitives
	 * 
	 * @throws Exception
	 */

	public void testStringtoPrimitives() throws Exception {
		assertEquals((Integer) (int) 'A', converter.convert(int.class, 'A'));
		assertEquals((Integer) (int) 'A', converter.convert(Integer.class, 'A'));
		assertEquals((Boolean) true, converter.convert(boolean.class, "1"));
		assertEquals((Boolean) true, converter.convert(Boolean.class, "1"));
		assertEquals((Boolean) false, converter.convert(boolean.class, "0"));
		assertEquals((Boolean) false, converter.convert(Boolean.class, "0"));
		assertEquals((Byte) (byte) 1, converter.convert(byte.class, "1"));
		assertEquals((Byte) (byte) 1, converter.convert(Byte.class, "1"));
		assertEquals((Short) (short) 1, converter.convert(short.class, "1"));
		assertEquals((Short) (short) 1, converter.convert(Short.class, "1"));
		assertEquals((Integer) 1, converter.convert(int.class, "1"));
		assertEquals((Integer) 1, converter.convert(Integer.class, "1"));
		assertEquals((Long) 1L, converter.convert(long.class, "1"));
		assertEquals((Long) 1L, converter.convert(Long.class, "1"));
		assertEquals(1f, converter.convert(float.class, "1"));
		assertEquals(1f, converter.convert(Float.class, "1"));
		assertEquals(1d, converter.convert(double.class, "1"));
		assertEquals(1d, converter.convert(double.class, "1"));
		assertEquals((Character) 'A', converter.convert(char.class, "A"));
		assertEquals((Character) 'A', converter.convert(Character.class, "A"));
	}

	/**
	 * Test the wrappers
	 * 
	 * @throws Exception
	 */
	public void testWrappers() throws Exception {
		Object[] types = {
				Boolean.FALSE, (byte) 0, '\u0000', (short) 0, 0, 0L, 0f, 0d
		};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < types.length; j++) {
				assertEquals("" + i + " " + j, types[i], converter.convert(types[i].getClass(), types[j]));
			}
		}
	}

	/**
	 * Create an array and see if we can convert a single number
	 * 
	 * @throws Exception
	 */
	public void testPrimitives() throws Exception {
		assertPrimitives1(1);
		assertPrimitives(0);
		assertPrimitives(new Object[] {
				0, 1, 2
		});
		assertPrimitives1(new Object[] {
				1, 2
		});
		assertPrimitives(false);
		assertPrimitives1(true);
		assertPrimitives('\u0000');
		assertPrimitives1('\u0001');
	}

	/**
	 * Test enums
	 * 
	 * @param source
	 * @throws Exception
	 */
	public enum X {
		A, B, C;
	}

	public void testEnums() throws Exception {
		assertEquals(X.A, converter.convert(X.class, "A"));
		assertEquals(X.B, converter.convert(X.class, 1));
	}

	/**
	 * Test collections
	 * 
	 * @param source
	 * @throws Exception
	 */
	static class XX {
		public ArrayList<String>				al;
		public Collection<String>				col;
		public Queue<String>					queue;
		public Stack<String>					stack;
		public Vector<String>					vector;
		public Set<String>						set;
		public TreeSet<String>					treeset;
		public SortedSet<String>				sorted;
		public ConcurrentLinkedQueue<String>	concurrent;
		public CopyOnWriteArrayList<String>		concurrentList;
		public CopyOnWriteArraySet<String>		concurrentSet;
	}

	public void testCollections() throws Exception {
		Class<XX> xx = XX.class;
		Object xxx = xx.newInstance();
		int count = 11;
		for (Field field : xx.getFields()) {
			Object o = converter.convert(field.getGenericType(), 1);
			assertTrue(o instanceof Collection);
			Collection c = (Collection) o;
			assertEquals("1", c.iterator().next());
			field.set(xxx, o);
			count--;
		}
		assertEquals(0, count);
	}

	/**
	 * Test generic collections
	 * 
	 * @param source
	 * @throws Exception
	 */
	static class GC {
		public Collection<String>				strings;
		public Collection<Collection<String>>	stringss;
		public Collection<String>[]				stringsarray;
		public List<X>							enums;
		public X[]								enuma;
		public List								list;
	}

	public void testGenericCollections() throws Exception {
		Class<GC> xx = GC.class;
		GC g = xx.newInstance();

		for (Field field : xx.getFields()) {
			Object o = converter.convert(field.getGenericType(), 1);
			field.set(g, o);
		}
		assertEquals("[1]", g.strings.toString());
		assertEquals(String.class, g.strings.iterator().next().getClass());
		assertEquals("[[1]]", g.stringss.toString());
		assertEquals("[1]", g.stringsarray[0].toString());
		assertEquals("[1]", g.list.toString());
		assertTrue(g.list.get(0) instanceof Integer);
		assertEquals(X.B, g.enuma[0]);
		assertEquals(X.B, g.enums.get(0));
	}

	/**
	 * Test generic maps
	 * 
	 * @param source
	 * @throws Exception
	 */
	public static class GM {
		public Map<String,Integer>					strings;
		public SortedMap<String,Integer>			sorted;
		public TreeMap<String,Integer>				tree;
		public ConcurrentHashMap<String,Integer>	concurrenthash;
		public ConcurrentMap<String,Integer>		concurrent;
		public Map									map;
	}

	public static class GT {
		public int		a	= 1;
		public double	b	= 2;
	}

	public void testGenericMaps() throws Exception {
		Class<GM> xx = GM.class;
		GM gMap = xx.newInstance();
		GM gSemiMap = xx.newInstance();

		GT semiMap = new GT();
		Map map = new HashMap<String,Integer>();
		map.put("a", 1);
		map.put("b", 2);

		for (Field field : xx.getFields()) {
			Object o = converter.convert(field.getGenericType(), map);
			field.set(gMap, o);
			Object o2 = converter.convert(field.getGenericType(), semiMap);
			field.set(gSemiMap, o2);
		}
		assertEquals("{a=1, b=2}", new TreeMap(gMap.strings).toString());
		assertEquals("{a=1, b=2}", new TreeMap(gSemiMap.strings).toString());
	}

	void assertPrimitives(@SuppressWarnings("unused") Object source) throws Exception {
		Class[] types = {
				byte.class, boolean.class, char.class, short.class, int.class, long.class, float.class, double.class
		};
		for (Class c : types) {
			Class at = Array.newInstance(c, 1).getClass();
			Object parray = converter.convert(at, 0);
			Object o = Array.get(parray, 0);
			if (o instanceof Number)
				assertEquals(0, ((Number) o).intValue());
			else if (o instanceof Character)
				assertEquals(0, ((Character) o).charValue());
			else if (o instanceof Boolean)
				assertEquals(false, ((Boolean) o).booleanValue());
			else
				fail(o.getClass() + " unexpected ");

			assertEquals(at, parray.getClass());
			assertEquals(c, parray.getClass().getComponentType());
		}
	}

	/**
	 * Test constructor
	 * 
	 * @param source
	 * @throws Exception
	 */

	public void testConstructor() throws Exception {
		String home = System.getProperty("user.home");
		assertEquals(new File(home), converter.convert(File.class, home));
		//assertEquals(new Version(1, 0, 0), converter.convert(Version.class, "1.0.0"));
	}

	/**
	 * Test valueOf
	 * 
	 * @param source
	 * @throws Exception
	 */

	public void testValueOf() throws Exception {
		assertEquals((Byte) (byte) 12, converter.convert(Byte.class, "12"));
		assertEquals((Boolean) true, converter.convert(Boolean.class, "TRUE"));
		assertEquals((Character) '1', converter.convert(char.class, "49"));
		assertEquals((Boolean) true, converter.convert(Boolean.class, "TRUE"));
		assertEquals((Boolean) true, converter.convert(Boolean.class, "TRUE"));
		assertEquals((Boolean) true, converter.convert(Boolean.class, "TRUE"));
	}

	void assertPrimitives1(Object source) throws Exception {
		Class[] types = {
				byte.class, boolean.class, char.class, short.class, int.class, long.class, float.class, double.class
		};
		for (Class c : types) {
			Class at = Array.newInstance(c, 1).getClass();
			Object parray = converter.convert(at, source);
			Object o = Array.get(parray, 0);
			if (o instanceof Number)
				assertEquals(1, ((Number) o).intValue());
			else if (o instanceof Character)
				assertEquals(1, ((Character) o).charValue());
			else if (o instanceof Boolean)
				assertEquals(true, ((Boolean) o).booleanValue());
			else
				fail(o.getClass() + " unexpected ");

			assertEquals(at, parray.getClass());
			assertEquals(c, parray.getClass().getComponentType());
		}
	}
}
