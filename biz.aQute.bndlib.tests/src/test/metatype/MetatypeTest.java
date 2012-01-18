package test.metatype;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.namespace.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import junit.framework.*;

import org.w3c.dom.*;

import aQute.bnd.annotation.metatype.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.libg.generics.*;

public class MetatypeTest extends TestCase {
	DocumentBuilderFactory	dbf		= DocumentBuilderFactory.newInstance();
	XPathFactory			xpathf	= XPathFactory.newInstance();
	XPath					xpath	= xpathf.newXPath();

	DocumentBuilder			db;
	{
		try {
			dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
			xpath.setNamespaceContext(new NamespaceContext() {

				public Iterator getPrefixes(String namespaceURI) {
					return Arrays.asList("md").iterator();
				}

				public String getPrefix(String namespaceURI) {
					return "md";
				}

				public String getNamespaceURI(String prefix) {
					return "http://www.osgi.org/xmlns/metatype/v1.1.0";
				}
			});
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void testOptions() {

	}

	/**
	 * Configuration should return null for nonprimitive properties if not
	 * defined. Now it returns 0.
	 * 
	 * Testcase:
	 * 
	 * @OCD interface Config {
	 * @ad(required = false) Integer port(); }
	 * 
	 *              Config config =
	 *              Configurable.createConfigurable(Config.class, properties);
	 *              assert config.port() == null; // property port is not set
	 * 
	 *              Fix: Please delete
	 *              "|| Number.class.isAssignableFrom(method.getReturnType())"
	 *              from aQute/bnd/annotation/metatype/Configurable.java
	 */
	@Meta.OCD interface C {
		@Meta.AD(required = false ) Integer port();
	}
	public void testConfigurableForNonPrimitives() {
		Map<String,String> p = new HashMap<String, String>();
		C config = Configurable.createConfigurable(C.class, p);
		assertNull(config.port());
		p.put("port", "10");
		 config = Configurable.createConfigurable(C.class, p);
		assertEquals( new Integer(10), config.port()); // property port is not set
	}

	/**
	 * Test method naming options with '.' and reserved names
	 */

	@Meta.OCD public static interface Naming {
		String secret();

		String _secret(); // .secret

		String __secret(); // _secret

		String $new(); // new

		String $$new(); // $new

		String a_b_c(); // a.b.c

		String a__b__c(); // a_b_c

		String _a__b(); // .a_b

		String $$$$$$$$a__b(); // $$$$a_b

		String $$$$$$$$a_b(); // $$$$a.b

		String a$(); // a

		String a$$(); // a$

		String a$$$(); // a$

		String a$$$$(); // a$$

		String a$$_$$(); // a$.$

		String a$$__$$(); // a$_$

		String a_$_(); // a..

		@Meta.AD(id = "secret") String xsecret();

		@Meta.AD(id = ".secret") String x_secret();

		@Meta.AD(id = "_secret") String x__secret(); // _secret

		@Meta.AD(id = "new") String x$new(); // new

		@Meta.AD(id = "$new") String x$$new(); // $new

		@Meta.AD(id = "a.b.c") String xa_b_c(); // a.b.c

		@Meta.AD(id = "a_b_c") String xa__b__c(); // a_b_c

		@Meta.AD(id = ".a_b") String x_a__b(); // .a_b

		@Meta.AD(id = "$$$$a_b") String x$$$$$$$$a__b(); // $$$$a_b

		@Meta.AD(id = "$$$$a.b") String x$$$$$$$$a_b(); // $$$$a.b

		@Meta.AD(id = "a") String xa$(); // a

		@Meta.AD(id = "a$") String xa$$(); // a$

		@Meta.AD(id = "a$") String xa$$$(); // a$

		@Meta.AD(id = "a$$") String xa$$$$(); // a$$

		@Meta.AD(id = "a$.$") String xa$$_$$(); // a$.$

		@Meta.AD(id = "a$_$") String xa$$__$$(); // a$_$

		@Meta.AD(id = "a..") String xa_$_(); // a..

		String noid();

		@Meta.AD(id = Meta.NULL) String nullid();
	}

	public void testNaming() throws Exception {
		Map<String, Object> map = Create.map();

		map.put("_secret", "_secret");
		map.put("_secret", "_secret");
		map.put(".secret", ".secret");
		map.put("$new", "$new");
		map.put("new", "new");
		map.put("secret", "secret");
		map.put("a_b_c", "a_b_c");
		map.put("a.b.c", "a.b.c");
		map.put(".a_b", ".a_b");
		map.put("$$$$a_b", "$$$$a_b");
		map.put("$$$$a.b", "$$$$a.b");
		map.put("a", "a");
		map.put("a$", "a$");
		map.put("a$$", "a$$");
		map.put("a$.$", "a$.$");
		map.put("a$_$", "a$_$");
		map.put("a..", "a..");
		map.put("noid", "noid");
		map.put("nullid", "nullid");

		Naming trt = Configurable.createConfigurable(Naming.class, map);

		// By name
		assertEquals("secret", trt.secret());
		assertEquals("_secret", trt.__secret());
		assertEquals(".secret", trt._secret());
		assertEquals("new", trt.$new());
		assertEquals("$new", trt.$$new());
		assertEquals("a.b.c", trt.a_b_c());
		assertEquals("a_b_c", trt.a__b__c());
		assertEquals(".a_b", trt._a__b());
		assertEquals("$$$$a.b", trt.$$$$$$$$a_b());
		assertEquals("$$$$a_b", trt.$$$$$$$$a__b());
		assertEquals("a", trt.a$());
		assertEquals("a$", trt.a$$());
		assertEquals("a$", trt.a$$$());
		assertEquals("a$.$", trt.a$$_$$());
		assertEquals("a$_$", trt.a$$__$$());
		assertEquals("a..", trt.a_$_());
		assertEquals("noid", trt.noid());
		assertEquals("nullid", trt.nullid());

		// By AD
		assertEquals("secret", trt.xsecret());
		assertEquals("_secret", trt.x__secret());
		assertEquals(".secret", trt.x_secret());
		assertEquals("new", trt.x$new());
		assertEquals("$new", trt.x$$new());
		assertEquals("a.b.c", trt.xa_b_c());
		assertEquals("a_b_c", trt.xa__b__c());
		assertEquals(".a_b", trt.x_a__b());
		assertEquals("$$$$a.b", trt.x$$$$$$$$a_b());
		assertEquals("$$$$a_b", trt.x$$$$$$$$a__b());
		assertEquals("a", trt.xa$());
		assertEquals("a$", trt.xa$$());
		assertEquals("a$", trt.xa$$$());
		assertEquals("a$.$", trt.xa$$_$$());

		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatype", "*");
		b.build();
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Resource r = b.getJar().getResource(
				"OSGI-INF/metatype/test.metatype.MetatypeTest$Naming.xml");
		IO.copy(r.openInputStream(), System.out);
		Document d = db.parse(r.openInputStream(), "UTF-8");
		assertEquals("http://www.osgi.org/xmlns/metatype/v1.1.0", d.getDocumentElement()
				.getNamespaceURI());

	}

	/**
	 * Test the special conversions.
	 */

	public static class MyList<T> extends ArrayList<T> {
		private static final long	serialVersionUID	= 1L;

		public MyList() {
			System.out.println("Constr");

		}
	}

	interface CollectionsTest {
		Collection<String> collection();

		List<String> list();

		Set<String> set();

		Queue<String> queue();

		// Deque<String> deque();
		Stack<String> stack();

		ArrayList<String> arrayList();

		LinkedList<String> linkedList();

		LinkedHashSet<String> linkedHashSet();

		MyList<String> myList();
	}

	public void testCollections() throws Exception {
		CollectionsTest trt = set(CollectionsTest.class, new int[] { 1, 2, 3 });
		List<String> source = Arrays.asList("1", "2", "3");

		assertTrue(trt.collection() instanceof Collection);
		assertEqualList(source, trt.collection());

		assertTrue(trt.list() instanceof List);
		assertEqualList(source, trt.list());
		assertTrue(trt.set() instanceof Set);
		assertEqualList(source, trt.set());
		assertTrue(trt.queue() instanceof Queue);
		assertEqualList(source, trt.queue());
		// assertTrue( trt.deque() instanceof Deque);
		// assertEqualList( source, trt.deque());
		assertTrue(trt.stack() instanceof Stack);
		assertEqualList(source, trt.stack());
		assertTrue(trt.arrayList() instanceof ArrayList);
		assertEqualList(source, trt.arrayList());
		assertTrue(trt.linkedList() instanceof LinkedList);
		assertEqualList(source, trt.linkedList());
		assertTrue(trt.linkedHashSet() instanceof LinkedHashSet);
		assertEqualList(source, trt.linkedHashSet());
		assertTrue(trt.myList() instanceof MyList);
		assertEqualList(source, trt.myList());
	}

	private void assertEqualList(List<?> a, Collection<?> b) {
		if (a.size() == b.size()) {
			for (Object x : a) {
				if (!b.contains(x))
					throw new AssertionFailedError("expected:<" + a + "> but was: <" + b + ">");
			}
			return;
		}
		throw new AssertionFailedError("expected:<" + a + "> but was: <" + b + ">");
	}

	/**
	 * Test the special conversions.
	 */
	interface SpecialConversions {
		enum X {
			A, B, C
		}

		X enumv();

		Pattern pattern();

		Class<?> clazz();

		URI constructor();
	}

	public void testSpecialConversions() throws URISyntaxException {
		Properties p = new Properties();
		p.put("enumv", "A");
		p.put("pattern", ".*");
		p.put("clazz", "java.lang.Object");
		p.put("constructor", "http://www.aQute.biz");

		SpecialConversions trt = Configurable.createConfigurable(SpecialConversions.class, (Map) p);
		assertEquals(SpecialConversions.X.A, trt.enumv());
		assertEquals(".*", trt.pattern().pattern());
		assertEquals(Object.class, trt.clazz());
		assertEquals(new URI("http://www.aQute.biz"), trt.constructor());
	}

	/**
	 * Test the converter.
	 * 
	 * @throws URISyntaxException
	 */

	public void testConverter() throws URISyntaxException {
		{
			// Test collections as value
			TestReturnTypes trt = set(TestReturnTypes.class, Arrays.asList(55));
			assertTrue(Arrays.equals(new boolean[] { true }, trt.rpaBoolean()));
			assertTrue(Arrays.equals(new byte[] { 55 }, trt.rpaByte()));
			assertTrue(Arrays.equals(new short[] { 55 }, trt.rpaShort()));
			assertTrue(Arrays.equals(new int[] { 55 }, trt.rpaInt()));
			assertTrue(Arrays.equals(new long[] { 55 }, trt.rpaLong()));
			assertTrue(Arrays.equals(new float[] { 55 }, trt.rpaFloat()));
			assertTrue(Arrays.equals(new double[] { 55 }, trt.rpaDouble()));
			assertEquals(Arrays.asList(true), trt.rBooleans());
			assertEquals(Arrays.asList(new Byte((byte) 55)), trt.rBytes());
			assertEquals(Arrays.asList(new Short((short) 55)), trt.rShorts());
			assertEquals(Arrays.asList(new Integer(55)), trt.rInts());
			assertEquals(Arrays.asList(new Long(55L)), trt.rLongs());
			assertEquals(Arrays.asList(new Float(55F)), trt.rFloats());
			assertEquals(Arrays.asList(new Double(55D)), trt.rDoubles());
			assertEquals(Arrays.asList("55"), trt.rStrings());
			assertEquals(Arrays.asList(new URI("55")), trt.rURIs());

			assertTrue(Arrays.equals(new Boolean[] { true }, trt.raBoolean()));
			assertTrue(Arrays.equals(new Byte[] { 55 }, trt.raByte()));
			assertTrue(Arrays.equals(new Short[] { 55 }, trt.raShort()));
			assertTrue(Arrays.equals(new Integer[] { 55 }, trt.raInt()));
			assertTrue(Arrays.equals(new Long[] { 55L }, trt.raLong()));
			assertTrue(Arrays.equals(new Float[] { 55F }, trt.raFloat()));
			assertTrue(Arrays.equals(new Double[] { 55D }, trt.raDouble()));
			assertTrue(Arrays.equals(new String[] { "55" }, trt.raString()));
			assertTrue(Arrays.equals(new URI[] { new URI("55") }, trt.raURI()));

		}
		{
			// Test primitive arrays as value
			TestReturnTypes trt = set(TestReturnTypes.class, new int[] { 55 });
			assertTrue(Arrays.equals(new boolean[] { true }, trt.rpaBoolean()));
			assertTrue(Arrays.equals(new byte[] { 55 }, trt.rpaByte()));
			assertTrue(Arrays.equals(new short[] { 55 }, trt.rpaShort()));
			assertTrue(Arrays.equals(new int[] { 55 }, trt.rpaInt()));
			assertTrue(Arrays.equals(new long[] { 55 }, trt.rpaLong()));
			assertTrue(Arrays.equals(new float[] { 55 }, trt.rpaFloat()));
			assertTrue(Arrays.equals(new double[] { 55 }, trt.rpaDouble()));
			assertEquals(Arrays.asList(true), trt.rBooleans());
			assertEquals(Arrays.asList(new Byte((byte) 55)), trt.rBytes());
			assertEquals(Arrays.asList(new Short((short) 55)), trt.rShorts());
			assertEquals(Arrays.asList(new Integer(55)), trt.rInts());
			assertEquals(Arrays.asList(new Long(55L)), trt.rLongs());
			assertEquals(Arrays.asList(new Float(55F)), trt.rFloats());
			assertEquals(Arrays.asList(new Double(55D)), trt.rDoubles());
			assertEquals(Arrays.asList("55"), trt.rStrings());
			assertEquals(Arrays.asList(new URI("55")), trt.rURIs());

			assertTrue(Arrays.equals(new Boolean[] { true }, trt.raBoolean()));
			assertTrue(Arrays.equals(new Byte[] { 55 }, trt.raByte()));
			assertTrue(Arrays.equals(new Short[] { 55 }, trt.raShort()));
			assertTrue(Arrays.equals(new Integer[] { 55 }, trt.raInt()));
			assertTrue(Arrays.equals(new Long[] { 55L }, trt.raLong()));
			assertTrue(Arrays.equals(new Float[] { 55F }, trt.raFloat()));
			assertTrue(Arrays.equals(new Double[] { 55D }, trt.raDouble()));
			assertTrue(Arrays.equals(new String[] { "55" }, trt.raString()));
			assertTrue(Arrays.equals(new URI[] { new URI("55") }, trt.raURI()));

		}

		{
			// Test single value
			TestReturnTypes trt = set(TestReturnTypes.class, 55);
			assertEquals(true, trt.rpBoolean());
			assertEquals(55, trt.rpByte());
			assertEquals(55, trt.rpShort());
			assertEquals(55, trt.rpInt());
			assertEquals(55L, trt.rpLong());
			assertEquals(55.0D, trt.rpDouble());
			assertEquals(55.0F, trt.rpFloat());
			assertEquals((Boolean) true, trt.rBoolean());
			assertEquals(new Byte((byte) 55), trt.rByte());
			assertEquals(new Short((short) 55), trt.rShort());
			assertEquals(new Integer(55), trt.rInt());
			assertEquals(new Long(55L), trt.rLong());
			assertEquals(new Float(55F), trt.rFloat());
			assertEquals(new Double(55), trt.rDouble());
			assertEquals("55", trt.rString());
			assertEquals(new URI("55"), trt.rURI());
			assertTrue(Arrays.equals(new boolean[] { true }, trt.rpaBoolean()));
			assertTrue(Arrays.equals(new byte[] { 55 }, trt.rpaByte()));
			assertTrue(Arrays.equals(new short[] { 55 }, trt.rpaShort()));
			assertTrue(Arrays.equals(new int[] { 55 }, trt.rpaInt()));
			assertTrue(Arrays.equals(new long[] { 55 }, trt.rpaLong()));
			assertTrue(Arrays.equals(new float[] { 55 }, trt.rpaFloat()));
			assertTrue(Arrays.equals(new double[] { 55 }, trt.rpaDouble()));
			assertEquals(Arrays.asList(true), trt.rBooleans());
			assertEquals(Arrays.asList(new Byte((byte) 55)), trt.rBytes());
			assertEquals(Arrays.asList(new Short((short) 55)), trt.rShorts());
			assertEquals(Arrays.asList(new Integer(55)), trt.rInts());
			assertEquals(Arrays.asList(new Long(55L)), trt.rLongs());
			assertEquals(Arrays.asList(new Float(55F)), trt.rFloats());
			assertEquals(Arrays.asList(new Double(55D)), trt.rDoubles());
			assertEquals(Arrays.asList("55"), trt.rStrings());
			assertEquals(Arrays.asList(new URI("55")), trt.rURIs());

			assertTrue(Arrays.equals(new Boolean[] { true }, trt.raBoolean()));
			assertTrue(Arrays.equals(new Byte[] { 55 }, trt.raByte()));
			assertTrue(Arrays.equals(new Short[] { 55 }, trt.raShort()));
			assertTrue(Arrays.equals(new Integer[] { 55 }, trt.raInt()));
			assertTrue(Arrays.equals(new Long[] { 55L }, trt.raLong()));
			assertTrue(Arrays.equals(new Float[] { 55F }, trt.raFloat()));
			assertTrue(Arrays.equals(new Double[] { 55D }, trt.raDouble()));
			assertTrue(Arrays.equals(new String[] { "55" }, trt.raString()));
			assertTrue(Arrays.equals(new URI[] { new URI("55") }, trt.raURI()));
		}
	}

	<T> T set(Class<T> interf, Object value) {
		Properties p = new Properties();
		Method ms[] = interf.getMethods();

		for (Method m : ms) {
			p.put(m.getName(), value);
		}
		return Configurable.createConfigurable(interf, (Map) p);
	}

	/**
	 * Test enum handling
	 */

	@Meta.OCD public static interface Enums {
		enum X {
			requireConfiguration, optionalConfiguration, ignoreConfiguration
		};

		X r();

		X i();

		X o();
	}

	public void testEnum() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatype", "*");
		b.build();
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Resource r = b.getJar().getResource(
				"OSGI-INF/metatype/test.metatype.MetatypeTest$Enums.xml");
		IO.copy(r.openInputStream(), System.out);

		Document d = db.parse(r.openInputStream());
		assertEquals("http://www.osgi.org/xmlns/metatype/v1.1.0", d.getDocumentElement()
				.getNamespaceURI());

		Properties p = new Properties();
		p.setProperty("r", "requireConfiguration");
		p.setProperty("i", "ignoreConfiguration");
		p.setProperty("o", "optionalConfiguration");
		Enums enums = Configurable.createConfigurable(Enums.class, (Map) p);
		assertEquals(Enums.X.requireConfiguration, enums.r());
		assertEquals(Enums.X.ignoreConfiguration, enums.i());
		assertEquals(Enums.X.optionalConfiguration, enums.o());
	}

	/**
	 * Test the OCD settings
	 */
	@Meta.OCD() public static interface OCDEmpty {
	}

	@Meta.OCD(description = "description") public static interface OCDDescription {
	}

	@Meta.OCD() public static interface OCDDesignatePidOnly {
	}

	@Meta.OCD(factory = true) public static interface OCDDesignatePidFactory {
	}

	@Meta.OCD(id = "id") public static interface OCDId {
	}

	@Meta.OCD(id = "id") public static interface OCDIdWithPid {
	}

	@Meta.OCD(localization = "localization") public static interface OCDLocalization {
	}

	@Meta.OCD(name = "name") public static interface OCDName {
	}

	public void testOCD() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatype", "*");
		b.build();
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.out.println(b.getJar().getResources().keySet());

		assertOCD(b, "test.metatype.MetatypeTest$OCDEmpty", "test.metatype.MetatypeTest$OCDEmpty",
				"Metatype test OCDEmpty", null, "test.metatype.MetatypeTest$OCDEmpty", false, null);
		assertOCD(b, "test.metatype.MetatypeTest$OCDName", "test.metatype.MetatypeTest$OCDName",
				"name", null, "test.metatype.MetatypeTest$OCDName", false, null);
		assertOCD(b, "test.metatype.MetatypeTest$OCDDescription",
				"test.metatype.MetatypeTest$OCDDescription", "Metatype test OCDDescription",
				"description", "test.metatype.MetatypeTest$OCDDescription", false, null);
		assertOCD(b, "test.metatype.MetatypeTest$OCDDesignatePidOnly",
				"test.metatype.MetatypeTest$OCDDesignatePidOnly",
				"Metatype test OCDDesignate pid only", null,
				"test.metatype.MetatypeTest$OCDDesignatePidOnly", false, null);
		assertOCD(b, "test.metatype.MetatypeTest$OCDDesignatePidFactory",
				"test.metatype.MetatypeTest$OCDDesignatePidFactory",
				"Metatype test OCDDesignate pid factory", null,
				"test.metatype.MetatypeTest$OCDDesignatePidFactory", true, null);
		assertOCD(b, "test.metatype.MetatypeTest$OCDId", "id", "Metatype test OCDId", null, "id",
				false, null);
		assertOCD(b, "test.metatype.MetatypeTest$OCDIdWithPid", "id",
				"Metatype test OCDId with pid", null, "id", false, null);
		assertOCD(b, "test.metatype.MetatypeTest$OCDLocalization",
				"test.metatype.MetatypeTest$OCDLocalization", "Metatype test OCDLocalization",
				null, "test.metatype.MetatypeTest$OCDLocalization", false, "localization");
	}

	void assertOCD(Builder b, String cname, String id, String name, String description,
			String designate, boolean factory, String localization) throws Exception {
		Resource r = b.getJar().getResource("OSGI-INF/metatype/" + cname + ".xml");
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.out);
		Document d = db.parse(r.openInputStream());
		assertEquals(id, xpath.evaluate("//OCD/@id", d, XPathConstants.STRING));
		assertEquals(name, xpath.evaluate("//OCD/@name", d, XPathConstants.STRING));
		assertEquals(localization == null ? cname : localization,
				xpath.evaluate("//OCD/@localization", d, XPathConstants.STRING));
		assertEquals(description == null ? "" : description,
				xpath.evaluate("//OCD/@description", d, XPathConstants.STRING));

		if (designate == null) {
			assertEquals(id, xpath.evaluate("//Designate/@pid", d, XPathConstants.STRING));
			if (factory)
				assertEquals(id,
						xpath.evaluate("//Designate/@factoryPid", d, XPathConstants.STRING));
		} else {
			assertEquals(designate, xpath.evaluate("//Designate/@pid", d, XPathConstants.STRING));
			if (factory)
				assertEquals(designate,
						xpath.evaluate("//Designate/@factoryPid", d, XPathConstants.STRING));
		}

		assertEquals(id, xpath.evaluate("//Object/@ocdref", d, XPathConstants.STRING));
	}

	/**
	 * Test the AD settings.
	 */

	@Meta.OCD(description = "advariations") public static interface TestAD {
		@Meta.AD String noSettings();

		@Meta.AD(id = "id") String withId();

		@Meta.AD(name = "name") String withName();

		@Meta.AD(max = "1") String withMax();

		@Meta.AD(min = "-1") String withMin();

		@Meta.AD(deflt = "deflt") String withDefault();

		@Meta.AD(cardinality = 0) String[] withC0();

		@Meta.AD(cardinality = 1) String[] withC1();

		@Meta.AD(cardinality = -1) Collection<String> withC_1();

		@Meta.AD(cardinality = -1) String[] withC_1ButArray();

		@Meta.AD(cardinality = 1) Collection<String> withC1ButCollection();

		@Meta.AD(type = Meta.Type.String) int withInt();

		@Meta.AD(type = Meta.Type.Integer) String withString();

		@Meta.AD(description = "description") String a();

		@Meta.AD(optionValues = { "a", "b" }) String valuesOnly();

		@Meta.AD(optionValues = { "a", "b" }, optionLabels = { "A", "B" }) String labelsAndValues();

		@Meta.AD(required = true) String required();

		@Meta.AD(required = false) String notRequired();
	}

	public void testAD() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatype", "*");
		b.build();
		Resource r = b.getJar().getResource(
				"OSGI-INF/metatype/test.metatype.MetatypeTest$TestAD.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.out.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.out);

		Document d = db.parse(r.openInputStream());

		assertAD(d, "noSettings", "No settings", "noSettings", null, null, null, 0, "String", null,
				null, null);
		assertAD(d, "withId", "With id", "id", null, null, null, 0, "String", null, null, null);
		assertAD(d, "name", "name", "withName", null, null, null, 0, "String", null, null, null);
		assertAD(d, "withMax", "With max", "withMax", null, "1", null, 0, "String", null, null,
				null);
		assertAD(d, "withMin", "With min", "withMin", "-1", null, null, 0, "String", null, null,
				null);
		assertAD(d, "withC1", "With c1", "withC1", null, null, null, 1, "String", null, null, null);
		assertAD(d, "withC0", "With c0", "withC0", null, null, null, 2147483647, "String", null,
				null, null);
		assertAD(d, "withC_1", "With c 1", "withC.1", null, null, null, -1, "String", null, null,
				null);
		assertAD(d, "withC_1ButArray", "With c 1 but array", "withC.1ButArray", null, null, null,
				-1, "String", null, null, null);
		assertAD(d, "withC1ButCollection", "With c1 but collection", "withC1ButCollection", null,
				null, null, 1, "String", null, null, null);
		assertAD(d, "withInt", "With int", "withInt", null, null, null, 0, "String", null, null,
				null);
		assertAD(d, "withString", "With string", "withString", null, null, null, 0, "Integer",
				null, null, null);
		assertAD(d, "a", "A", "a", null, null, null, 0, "String", "description", null, null);
		assertAD(d, "valuesOnly", "Values only", "valuesOnly", null, null, null, 0, "String", null,
				new String[] { "a", "b" }, new String[] { "a", "b" });
		assertAD(d, "labelsAndValues", "Labels and values", "labelsAndValues", null, null, null, 0,
				"String", null, new String[] { "a", "b" }, new String[] { "A", "A" });
	}

	void assertAD(Document d, String mname, String name, String id, String min, String max,
			String deflt, int cardinality, String type, String description, String[] optionvalues,
			String optionLabels[]) throws XPathExpressionException {
		assertEquals(name,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@name", d, XPathConstants.STRING));
		assertEquals(id, xpath.evaluate("//OCD/AD[@id='" + id + "']/@id", d, XPathConstants.STRING));
		assertEquals(min == null ? "" : min,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@min", d, XPathConstants.STRING));
		assertEquals(max == null ? "" : max,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@max", d, XPathConstants.STRING));
		assertEquals(deflt == null ? "" : deflt,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@deflt", d, XPathConstants.STRING));
		assertEquals(cardinality + "",
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@cardinality", d, XPathConstants.STRING));
		assertEquals(type,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@type", d, XPathConstants.STRING));
		assertEquals(description == null ? "" : description,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@description", d, XPathConstants.STRING));
	}

	/**
	 * Test all the return types.
	 */
	@Meta.OCD(description = "simple", name = "TestSimple") public static interface TestReturnTypes {
		boolean rpBoolean();

		byte rpByte();

		char rpCharacter();

		short rpShort();

		int rpInt();

		long rpLong();

		float rpFloat();

		double rpDouble();

		Boolean rBoolean();

		Byte rByte();

		Character rCharacter();

		Short rShort();

		Integer rInt();

		Long rLong();

		Float rFloat();

		Double rDouble();

		String rString();

		URI rURI();

		boolean[] rpaBoolean();

		byte[] rpaByte();

		char[] rpaCharacter();

		short[] rpaShort();

		int[] rpaInt();

		long[] rpaLong();

		float[] rpaFloat();

		double[] rpaDouble();

		Collection<Boolean> rBooleans();

		Collection<Byte> rBytes();

		Collection<Character> rCharacters();

		Collection<Short> rShorts();

		Collection<Integer> rInts();

		Collection<Long> rLongs();

		Collection<Float> rFloats();

		Collection<Double> rDoubles();

		Collection<String> rStrings();

		Collection<URI> rURIs();

		Boolean[] raBoolean();

		Byte[] raByte();

		Character[] raCharacter();

		Short[] raShort();

		Integer[] raInt();

		Long[] raLong();

		Float[] raFloat();

		Double[] raDouble();

		String[] raString();

		URI[] raURI();
	}

	public void testReturnTypes() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatype", "*");
		b.build();
		Resource r = b.getJar().getResource(
				"OSGI-INF/metatype/test.metatype.MetatypeTest$TestReturnTypes.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.out.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.out);

		Document d = db.parse(r.openInputStream());
		assertEquals("http://www.osgi.org/xmlns/metatype/v1.1.0", d.getDocumentElement()
				.getNamespaceURI());
		// Primitives
		assertEquals("Boolean", xpath.evaluate("//OCD/AD[@id='rpBoolean']/@type", d));
		assertEquals("Byte", xpath.evaluate("//OCD/AD[@id='rpByte']/@type", d));
		assertEquals("Character", xpath.evaluate("//OCD/AD[@id='rpCharacter']/@type", d));
		assertEquals("Short", xpath.evaluate("//OCD/AD[@id='rpShort']/@type", d));
		assertEquals("Integer", xpath.evaluate("//OCD/AD[@id='rpInt']/@type", d));
		assertEquals("Long", xpath.evaluate("//OCD/AD[@id='rpLong']/@type", d));
		assertEquals("Float", xpath.evaluate("//OCD/AD[@id='rpFloat']/@type", d));
		assertEquals("Double", xpath.evaluate("//OCD/AD[@id='rpDouble']/@type", d));

		// Primitive Wrappers
		assertEquals("Boolean", xpath.evaluate("//OCD/AD[@id='rBoolean']/@type", d));
		assertEquals("Byte", xpath.evaluate("//OCD/AD[@id='rByte']/@type", d));
		assertEquals("Character", xpath.evaluate("//OCD/AD[@id='rCharacter']/@type", d));
		assertEquals("Short", xpath.evaluate("//OCD/AD[@id='rShort']/@type", d));
		assertEquals("Integer", xpath.evaluate("//OCD/AD[@id='rInt']/@type", d));
		assertEquals("Long", xpath.evaluate("//OCD/AD[@id='rLong']/@type", d));
		assertEquals("Float", xpath.evaluate("//OCD/AD[@id='rFloat']/@type", d));
		assertEquals("Double", xpath.evaluate("//OCD/AD[@id='rDouble']/@type", d));

		// Primitive Arrays
		assertEquals("Boolean", xpath.evaluate("//OCD/AD[@id='rpaBoolean']/@type", d));
		assertEquals("Byte", xpath.evaluate("//OCD/AD[@id='rpaByte']/@type", d));
		assertEquals("Character", xpath.evaluate("//OCD/AD[@id='rpaCharacter']/@type", d));
		assertEquals("Short", xpath.evaluate("//OCD/AD[@id='rpaShort']/@type", d));
		assertEquals("Integer", xpath.evaluate("//OCD/AD[@id='rpaInt']/@type", d));
		assertEquals("Long", xpath.evaluate("//OCD/AD[@id='rpaLong']/@type", d));
		assertEquals("Float", xpath.evaluate("//OCD/AD[@id='rpaFloat']/@type", d));
		assertEquals("Double", xpath.evaluate("//OCD/AD[@id='rpaDouble']/@type", d));

		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='rpaBoolean']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='rpaByte']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='rpaCharacter']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='rpaShort']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='rpaInt']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='rpaLong']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='rpaFloat']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='rpaDouble']/@cardinality", d));

		// Wrapper + Object arrays
		assertEquals("Boolean", xpath.evaluate("//OCD/AD[@id='raBoolean']/@type", d));
		assertEquals("Byte", xpath.evaluate("//OCD/AD[@id='raByte']/@type", d));
		assertEquals("Character", xpath.evaluate("//OCD/AD[@id='raCharacter']/@type", d));
		assertEquals("Short", xpath.evaluate("//OCD/AD[@id='raShort']/@type", d));
		assertEquals("Integer", xpath.evaluate("//OCD/AD[@id='raInt']/@type", d));
		assertEquals("Long", xpath.evaluate("//OCD/AD[@id='raLong']/@type", d));
		assertEquals("Float", xpath.evaluate("//OCD/AD[@id='raFloat']/@type", d));
		assertEquals("Double", xpath.evaluate("//OCD/AD[@id='raDouble']/@type", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='raString']/@type", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='raURI']/@type", d));

		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raBoolean']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raByte']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raCharacter']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raShort']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raInt']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raLong']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raFloat']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raDouble']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raString']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raURI']/@cardinality", d));

		// Wrapper + Object collections
		assertEquals("Boolean", xpath.evaluate("//OCD/AD[@id='rBooleans']/@type", d));
		assertEquals("Byte", xpath.evaluate("//OCD/AD[@id='rBytes']/@type", d));
		assertEquals("Character", xpath.evaluate("//OCD/AD[@id='rCharacter']/@type", d));
		assertEquals("Short", xpath.evaluate("//OCD/AD[@id='rShorts']/@type", d));
		assertEquals("Integer", xpath.evaluate("//OCD/AD[@id='rInts']/@type", d));
		assertEquals("Long", xpath.evaluate("//OCD/AD[@id='rLongs']/@type", d));
		assertEquals("Float", xpath.evaluate("//OCD/AD[@id='rFloats']/@type", d));
		assertEquals("Double", xpath.evaluate("//OCD/AD[@id='rDoubles']/@type", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='rStrings']/@type", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='rURIs']/@type", d));

		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rBooleans']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rBytes']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rCharacters']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rShorts']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rInts']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rLongs']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rFloats']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rDoubles']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rStrings']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rURIs']/@cardinality", d));
	}

	/**
	 * Test simple
	 * 
	 * @author aqute
	 * 
	 */
	@Meta.OCD(description = "simple", name = "TestSimple") public static interface TestSimple {
		@Meta.AD String simple();

		String[] notSoSimple();

		Collection<String> stringCollection();
	}

	public void testSimple() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatype", "*");
		b.build();
		Resource r = b.getJar().getResource(
				"OSGI-INF/metatype/test.metatype.MetatypeTest$TestSimple.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.out.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.out);

		Document d = db.parse(r.openInputStream());

		assertEquals("TestSimple", xpath.evaluate("//OCD/@name", d));
		assertEquals("simple", xpath.evaluate("//OCD/@description", d));
		assertEquals("test.metatype.MetatypeTest$TestSimple", xpath.evaluate("//OCD/@id", d));
		assertEquals("test.metatype.MetatypeTest$TestSimple", xpath.evaluate("//Designate/@pid", d));
		assertEquals("test.metatype.MetatypeTest$TestSimple", xpath.evaluate("//Object/@ocdref", d));
		assertEquals("simple", xpath.evaluate("//OCD/AD[@id='simple']/@id", d));
		assertEquals("Simple", xpath.evaluate("//OCD/AD[@id='simple']/@name", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='simple']/@type", d));
		assertEquals(Integer.MAX_VALUE + "",
				xpath.evaluate("//OCD/AD[@id='notSoSimple']/@cardinality", d));

	}

}
