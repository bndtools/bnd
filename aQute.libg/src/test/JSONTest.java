package test;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import junit.framework.*;
import aQute.lib.json.*;
import aQute.libg.map.*;

public class JSONTest extends TestCase {
	JSONCodec	codec	= new JSONCodec();

	/**
	 * Test maps
	 * 
	 * @throws Exception
	 */

	public void testMaps() throws Exception {
		Encoder enc = codec.enc();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("a", new int[] { 1, 2 });
		String string = enc.put(map).toString();
		assertEquals("{\"a\":[1,2]}", string);
	}

	/**
	 * Test primitive arrays
	 * 
	 * @throws Exception
	 */
	public void testPrimitiveArrays() throws Exception {
		Decoder dec = new JSONCodec().dec();

		assertTrue(Arrays.equals(new Boolean[] { true, false },
				dec.from(" [ true , false ] ").get(Boolean[].class)));
		assertTrue(Arrays.equals(new boolean[] { true, false },
				dec.from(" [ true , false ] ").get(boolean[].class)));

		assertTrue(Arrays.equals(new Character[] { 'A', 'B' },
				dec.from(" [ 65,66 ] ").get(Character[].class)));
		assertTrue(Arrays
				.equals(new char[] { 'A', 'B' }, dec.from(" [ 65,66 ] ").get(char[].class)));
		assertTrue(Arrays
				.equals(new Short[] { -1, -2 }, dec.from("[ -1 , -2 ]").get(Short[].class)));
		assertTrue(Arrays
				.equals(new short[] { -1, -2 }, dec.from("[ -1 , -2 ]").get(short[].class)));
		assertTrue(Arrays.equals(new Integer[] { -1, -2 },
				dec.from("[ -1 , -2 ]").get(Integer[].class)));
		assertTrue(Arrays.equals(new int[] { -1, -2 }, dec.from("[ -1 , -2 ]").get(int[].class)));
		assertTrue(Arrays
				.equals(new Long[] { -1L, -2L }, dec.from("[ -1 , -2 ]").get(Long[].class)));
		assertTrue(Arrays.equals(new long[] { -1, -2 }, dec.from("[ -1 , -2 ]").get(long[].class)));
		assertTrue(Arrays.equals(new Float[] { -1f, -2f }, dec.from("[ -1 ,-2 ]")
				.get(Float[].class)));
		assertTrue(Arrays.equals(new float[] { -1f, -2f }, dec.from("[ -1, -2 ]")
				.get(float[].class)));
		assertTrue(Arrays.equals(new Double[] { -1d, -2d },
				dec.from("[-1 , -2 ]").get(Double[].class)));
		assertTrue(Arrays.equals(new double[] { -1d, -2d }, dec.from("[-1, -2]")
				.get(double[].class)));

		Encoder enc = new JSONCodec().enc();
		assertEquals("[false,true]", enc.to().put(new Boolean[] { false, true }).toString());
		assertEquals("[false,true]", enc.to().put(new boolean[] { false, true }).toString());
		assertEquals("[65,66]", enc.to().put(new Character[] { 'A', 'B' }).toString());
		assertEquals("[65,66]", enc.to().put(new char[] { 'A', 'B' }).toString());
		assertEquals("[1,2]", enc.to().put(new short[] { 1, 2 }).toString());
		assertEquals("[1,2]", enc.to().put(new Short[] { 1, 2 }).toString());
		assertEquals("[1,2]", enc.to().put(new int[] { 1, 2 }).toString());
		assertEquals("[1,2]", enc.to().put(new Integer[] { 1, 2 }).toString());
		assertEquals("[1,2]", enc.to().put(new long[] { 1, 2 }).toString());
		assertEquals("[1,2]", enc.to().put(new Long[] { 1L, 2L }).toString());
		assertEquals("[-1,-2]", enc.to().put(new float[] { -1, -2 }).toString());
		assertEquals("[1,-2]", enc.to().put(new Float[] { 1f, -2f }).toString());
		assertEquals("[-1,2]", enc.to().put(new double[] { -1d, 2d }).toString());
		assertEquals("[1,2]", enc.to().put(new Double[] { 1d, 2d }).toString());

	}

	/**
	 * Test byte arrays
	 * 
	 * @throws Exception
	 */
	public void testByteArrays() throws Exception {
		Encoder enc = new JSONCodec().enc();
		assertEquals("[43,41]", enc.to().put(new Byte[] { 43, 41 }).toString());
		assertEquals("\"Kyk=\"", enc.to().put(new byte[] { 43, 41 }).toString());

		Decoder dec = new JSONCodec().dec();
		assertTrue(Arrays.equals(new byte[] { 43, 41 }, dec.faq("'Kyk='").get(byte[].class)));
		assertTrue(Arrays.equals(new Byte[] { 43, 41 }, dec.from("[43,41]").get(Byte[].class)));
		assertTrue(Arrays.equals(new byte[] { 43, 41 }, dec.from("[43,41]").get(byte[].class)));
	}

	/**
	 * Basic tests to see if the default types returns something useful
	 * 
	 * @throws Exception
	 */
	public void testEncodeBasic() throws Exception {
		Encoder enc = new JSONCodec().enc();
		assertEquals("49", enc.to().put((byte) 49).toString());
		assertEquals("49", enc.to().put('1').toString());
		assertEquals("\"abc\"", enc.to().put("abc").toString());
		assertEquals("123", enc.to().put(123).toString());
		assertEquals("-123", enc.to().put(-123).toString());
		assertEquals("-123", enc.to().put(-123.0).toString());
		assertEquals("true", enc.to().put(true).toString());
		assertEquals("false", enc.to().put(false).toString());
		assertEquals("null", enc.to().put(null).toString());
		assertEquals("[1,2,3]", enc.to().put(Arrays.asList(1, 2, 3)).toString());
		assertEquals("{\"1\":1,\"2\":2,\"3\":3}", enc.to().put(MAP.$(1, 1).$(2, 2).$(3, 3))
				.toString());
		assertEquals("{\"1\":1,\"2\":2,\"3\":3}", enc.to().put(MAP.$("1", 1).$("2", 2).$("3", 3))
				.toString());
	}

	enum E {
		A, B;
	}

	public void testDecodeBasic() throws Exception {
		Decoder dec = new JSONCodec().dec();

		// Dates
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date d = sdf.parse("2012-03-01T10:23:00");
		System.out.println(d.getTime());
		assertEquals(d, dec.from("\"2012-03-01T10:23:00\"").get(Date.class));

		// Numbers
		assertEquals(49, dec.from("49").get());
		assertEquals((Integer) 49, dec.from("49").get(Integer.class));
		assertEquals((Long) 49L, dec.from("49").get(Long.class));
		assertEquals((Double) 49d, dec.from("49").get(Double.class));
		assertEquals((Double) 49.3d, dec.from("49.3").get(Double.class));
		assertEquals((Byte) (byte) 49, dec.from("49.3").get(Byte.class));
		assertEquals((Byte) (byte) 49, dec.from("49.9999").get(Byte.class));
		assertEquals((Short) (short) 49, dec.from("49.9999").get(Short.class));
		assertEquals((Float) 49.9999f, dec.from("49.9999").get(Float.class));
		assertEquals((Character) '0', dec.from("48").get(Character.class));
		assertEquals((Boolean) true, dec.from("48").get(Boolean.class));
		assertEquals((Boolean) false, dec.from("0").get(Boolean.class));
		assertEquals((Boolean) true, dec.from("48").get(boolean.class));
		assertEquals((Boolean) false, dec.from("0").get(boolean.class));

		// String based
		assertEquals("abc", dec.from("\"abc\"").get());

		// Patterns
		assertTrue(Pattern.class == dec.from("\"abc\"").get(Pattern.class).getClass());
		assertEquals(Pattern.compile("abc") + "", dec.from("\"abc\"").get(Pattern.class) + "");

		// Constructor string based
		assertEquals(new File(System.getProperty("user.home")),
				dec.from("\"" + System.getProperty("user.home") + "\"").get(File.class));

		// Enums
		assertEquals(E.A, dec.from("\"A\"").get(E.class));

		// Arrays as strings
		assertEquals("[1,2,3]", dec.from("[1,2,3]").get(String.class));

		// Objects as strings
		assertEquals("{\"a\":1, \"b\":\"abc\"}",
				dec.from("{\"a\":1, \"b\":\"abc\"}").get(String.class));
		assertEquals("{\"a\":1, \"b\":\"}{}\"}",
				dec.from("{\"a\":1, \"b\":\"}{}\"}").get(String.class));

	}

	/**
	 * Test arrays
	 */
	public List<Integer>	integers	= Arrays.asList(1, 2, 3);	// Provides
																	// generic
																	// types
	public int[]			array		= { 1, 2, 3 };				// Provides
																	// generic
																	// types

	public void testArrays() throws Exception {
		Decoder dec = new JSONCodec().dec();

		// Default is double
		assertEquals(Arrays.asList(1, 2, 3), dec.from(" [ 1 , 2 , 3 ] ").get());

		// Now use the integers list for the generic type

		Field field = getClass().getField("integers");
		assertEquals(integers, dec.from(" [ 1 , 2 , 3 ] ").get(field.getGenericType()));

		// And now use the array of primitives
		field = getClass().getField("array");
		assertTrue(Arrays.equals(array,
				(int[]) dec.from(" [ 1 , 2 , 3 ] ").get(field.getGenericType())));
	}

	/**
	 * Test the map functionality
	 */
	public Map<String, Integer>		map;
	public Map<Integer, Integer>	mapIntegerKeys;

	public void testObject() throws Exception {
		Decoder dec = new JSONCodec().dec();

		assertEquals(MAP.$("1", 1).$("2", 2).$("3", 3), dec.from("{\"1\":1,\"2\":2,\"3\":3}").get());
		assertEquals(MAP.$("1", 1).$("2", 2).$("3", 3),
				dec.from("\t\n\r {    \"1\"  :  1,  \"2\" :2 ,\"3\" : 3 \n}").get());

		Field field = getClass().getField("map");
		assertEquals(
				MAP.$("1", 1).$("2", 2).$("3", 3),
				dec.from("\t\n\r {    \"1\"  :  1,  \"2\" :2 ,\"3\" : 3 \n}").get(
						field.getGenericType()));

		field = getClass().getField("mapIntegerKeys");
		assertEquals(MAP.$(1, 1).$(2, 2).$(3, 3),
				dec.from("{\"1\":1,\"2\":2,\"3\":3}").get(field.getGenericType()));
	}

	/**
	 * Test the object support
	 * 
	 */
	public static class Data1 {
		public boolean				b;
		public byte					by;
		public char					ch;
		public double				d;
		public float				f;
		public int					i;
		public long					l;
		public String				s;
		public short				sh;
		public Map<String, Object>	map;
	}

	public static class Data1A {
		public Boolean		b;
		public Byte			by;
		public Character	ch;
		public Double		d;
		public Float		f;
		public Integer		i;
		public Long			l;
		public String		s;
		public Short		sh;
	}

	public void testEncodeTypeA() throws Exception {
		Encoder enc = new JSONCodec().enc();
		Data1A data1 = new Data1A();
		data1.b = false;
		data1.by = -1;
		data1.ch = '1';
		data1.d = 3.0d;
		data1.f = 3.0f;
		data1.i = 1;
		data1.l = 2l;
		data1.s = "abc";
		data1.sh = -10;

		assertEquals(
				"{\"b\":false,\"by\":-1,\"ch\":49,\"d\":3,\"f\":3,\"i\":1,\"l\":2,\"s\":\"abc\",\"sh\":-10}",
				enc.to().put(data1).toString());
	}

	public void testEncodeType() throws Exception {
		Encoder enc = new JSONCodec().enc();
		Data1 data1 = new Data1();
		data1.b = true;
		data1.by = -1;
		data1.ch = '1';
		data1.d = 3.0d;
		data1.f = 3.0f;
		data1.i = 1;
		data1.l = 2l;
		data1.s = "abc";
		data1.sh = -10;
		data1.map = new HashMap<String, Object>();
		data1.map.put("a", Arrays.asList(1, 2, 3));
		String s = enc.to().put(data1).toString();
		assertEquals(
				"{\"b\":true,\"by\":-1,\"ch\":49,\"d\":3,\"f\":3,\"i\":1,\"l\":2,\"map\":{\"a\":[1,2,3]},\"s\":\"abc\",\"sh\":-10}",
				s);
	}

	public void testDecodeType() throws Exception {
		Decoder dec = new JSONCodec().dec();
		Data1 d = dec
				.from("{\"b\":false,\"by\":-1,\"ch\":49,\"d\":3.0,\"f\":3.0,\"i\":1,\"l\":2,\"s\":\"abc\",\"sh\":-10}")
				.get(Data1.class);
		assertEquals(false, d.b);
		assertEquals(-1, d.by);
		assertEquals('1', d.ch);
		assertEquals(3.0d, d.d);
		assertEquals(3.0f, d.f);
		assertEquals(1, d.i);
		assertEquals(2l, d.l);
		assertEquals("abc", d.s);
		assertEquals(-10, d.sh);
	}

	public void testDecodeTypeA() throws Exception {
		Decoder dec = new JSONCodec().dec();
		Data1A d = dec
				.from("{\"b\":false,\"by\":-1,\"ch\":49,\"d\":3.0,\"f\":3.0,\"i\":1,\"l\":2,\"s\":\"abc\",\"sh\":-10}")
				.get(Data1A.class);
		assertEquals((Boolean) false, d.b);
		assertEquals((Byte) (byte) (-1), d.by);
		assertEquals((Character) '1', d.ch);
		assertEquals(3.0d, d.d);
		assertEquals(3.0f, d.f);
		assertEquals((Integer) 1, d.i);
		assertEquals((Long) 2l, d.l);
		assertEquals("abc", d.s);
		assertEquals((Short) (short) -10, d.sh);

	}

	/**
	 * Test complex generic types
	 */

	public static class Data2 {
		public Map<String, Integer>						integers;
		public Map<String, List<Map<Integer, String>>>	map;
	}

	public void testComplexMaps() throws Exception {
		Decoder dec = new JSONCodec().dec();
		Data2 d = dec.from("{\"map\": {\"a\":[ {\"1\":1}, {\"2\":2} ]},\"integers\":{\"c\":1}}")
				.get(Data2.class);

		// Check integers
		assertEquals(1, d.integers.size());
		assertEquals((Integer) 1, (Integer) d.integers.get("c"));

		// Check map
		assertEquals(1, d.map.size());
		List<Map<Integer, String>> sub = d.map.get("a");
		Map<Integer, String> subsub1 = sub.get(0);
		String subsubsub1 = subsub1.get(1);

		Map<Integer, String> subsub2 = sub.get(1);
		String subsubsub2 = subsub2.get(2);

		assertEquals("1", subsubsub1);
		assertEquals("2", subsubsub2);
	}

	/**
	 * Test extra field
	 */
	public static class Data3 {
		public Map<String, Object>	__extra;
	}

	public void testExtra() throws Exception {
		Decoder dec = new JSONCodec().dec();
		Data3 d = dec.from("{\"a\": 1, \"b\": [1], \"c\": {}}").get(Data3.class);

		assertEquals(1, d.__extra.get("a"));
		assertEquals(Arrays.asList(1), d.__extra.get("b"));
		assertEquals(new HashMap<String, Object>(), d.__extra.get("c"));
	}

	/**
	 * Test calling the encoder repeatedly
	 */

	public void testRepeat() throws Exception {
		Decoder dec = new JSONCodec().dec();
		StringReader r = new StringReader("1\t2\r3\n 4      5   \n\r");
		assertEquals((Integer) 1, dec.from(r).get(Integer.class));
		assertEquals((Integer) 2, dec.get(Integer.class));
		assertEquals((Integer) 3, dec.get(Integer.class));
		assertEquals((Integer) 4, dec.get(Integer.class));
		assertFalse(dec.isEof());
		assertEquals((Integer) 5, dec.get(Integer.class));
		assertTrue(dec.isEof());

	}

	/**
	 * Test arrays
	 */

	public static class Data4 {
		public boolean[]	booleans;
		public byte[]		bytes;
		public short[]		shorts;
		public char[]		chars;
		public int[]		ints;
		public long[]		longs;
		public float[]		floats;
		public double[]		doubles;
	}

	public void testEncodeTypePrimitiveArrays() throws Exception {
		Encoder enc = new JSONCodec().enc();
		Data4 d = new Data4();
		d.booleans = new boolean[] { false, false };
		d.bytes = new byte[] { 1, 2 };
		d.shorts = new short[] { 3, 4 };
		d.chars = new char[] { 'A', 'B' };
		d.ints = new int[] { 5, 6 };
		d.longs = new long[] { 7, 8 };
		d.floats = new float[] { 7, 8 };
		d.doubles = new double[] { 7, 8 };

		assertEquals(
				"{\"booleans\":[false,false],\"bytes\":\"AQI=\",\"chars\":[65,66],\"doubles\":[7,8],\"floats\":[7,8],\"ints\":[5,6],\"longs\":[7,8],\"shorts\":[3,4]}",
				enc.put(d).toString());
		Decoder dec = new JSONCodec().dec();
		Data4 dd = dec
				.from("{\"booleans\":[false,false],\"bytes\":\"AQI=\",\"chars\":[65,66],\"doubles\":[7,8],\"floats\":[7,8],\"ints\":[5,6],\"longs\":[7,8],\"shorts\":[3,4]}")
				.get(Data4.class);
		assertTrue(Arrays.equals(d.booleans, dd.booleans));
		assertTrue(Arrays.equals(d.bytes, dd.bytes));
		assertTrue(Arrays.equals(d.shorts, dd.shorts));
		assertTrue(Arrays.equals(d.chars, dd.chars));
		assertTrue(Arrays.equals(d.ints, dd.ints));
		assertTrue(Arrays.equals(d.longs, dd.longs));
		assertTrue(Arrays.equals(d.floats, dd.floats));
		assertTrue(Arrays.equals(d.doubles, dd.doubles));
	}

	/**
	 * Test defaults
	 */

	public static class DataDefaults {
		public int	a	= 3;
	}

	public void testDefaults() throws Exception {
		Encoder enc = new JSONCodec().enc();
		DataDefaults d = new DataDefaults();
		// We're not writing out fields with defaults.
		assertEquals("{}", enc.to().put(d).toString());
		assertEquals("{\"a\":3}", enc.to().writeDefaults().put(d).toString());

		Decoder dec = new JSONCodec().dec();
		DataDefaults dd = dec.from("{}").get(DataDefaults.class);
		assertEquals(d.a, dd.a);
		DataDefaults ddd = dec.from("{\"a\":4}").get(DataDefaults.class);
		assertEquals(4, ddd.a);
	}

	/**
	 * Test the checksum support
	 */
	public void testDigest() throws Exception {
		Encoder enc = new JSONCodec().enc();
		enc.mark().put("Hello World");
		byte[] original = enc.digest();
		String string = enc.put(original).append("\n").toString();

		Decoder dec = new JSONCodec().dec();
		String x = dec.mark().from(string).get(String.class);
		assertEquals("Hello World", x);
		byte read[] = dec.digest();
		byte read2[] = dec.get(byte[].class);
		assertTrue(Arrays.equals(original, read));
		assertTrue(Arrays.equals(read, read2));
	}

	
	/**
	 * Test for the blog
	 */
	public enum Sex { MALE, FEMALE; }
	public static class Person {
	  public String name;
	  public Sex    sex;
	  public Date   birthday;
	  public List<Person> offspring = new ArrayList<Person>();
	}

	public void testBlog() throws Exception {
		Person u1 = new Person();
		u1.name = "Peter";
		u1.sex = Sex.MALE;
		Person u2 = new Person();
		u2.name = "Mischa";
		u2.sex = Sex.FEMALE;
		u1.offspring.add(u2);
		Person u3 = new Person();
		u3.name = "Thomas";
		u3.sex = Sex.MALE;
		u1.offspring.add(u3);
		
		String s = codec.enc().put( u1 ).toString();
		System.out.println(s);
		Person u4 = codec.dec().from(s).get( Person.class );
		
	}
}
