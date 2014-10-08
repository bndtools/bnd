package test.metatype;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.namespace.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import junit.framework.*;

import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.w3c.dom.*;

import aQute.bnd.osgi.*;
import aQute.lib.io.*;

@SuppressWarnings("resource")
public class SpecMetatypeTest extends TestCase {
	static DocumentBuilderFactory	dbf		= DocumentBuilderFactory.newInstance();
	static XPathFactory				xpathf	= XPathFactory.newInstance();
	static XPath					xpath	= xpathf.newXPath();

	static DocumentBuilder			db;
	static {
		try {
			dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
			xpath.setNamespaceContext(new NamespaceContext() {

				@Override
				public Iterator<String> getPrefixes(String namespaceURI) {
					return Arrays.asList("md").iterator();
				}

				@Override
				public String getPrefix(String namespaceURI) {
					return "md";
				}

				@Override
				public String getNamespaceURI(String prefix) {
					return "http://www.osgi.org/xmlns/metatype/v1.3.0";
				}
			});
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new ExceptionInInitializerError(e);
		}
	}


	/**
	 * Test method naming options with '.' and reserved names
	 */

	@ObjectClassDefinition
	public static interface Naming {
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

		@AttributeDefinition(name = "secret")
		String xsecret();

		@AttributeDefinition(name = ".secret")
		String x_secret();

		@AttributeDefinition(name = "_secret")
		String x__secret(); // _secret

		@AttributeDefinition(name = "new")
		String x$new(); // new

		@AttributeDefinition(name = "$new")
		String x$$new(); // $new

		@AttributeDefinition(name = "a.b.c")
		String xa_b_c(); // a.b.c

		@AttributeDefinition(name = "a_b_c")
		String xa__b__c(); // a_b_c

		@AttributeDefinition(name = ".a_b")
		String x_a__b(); // .a_b

		@AttributeDefinition(name = "$$$$a_b")
		String x$$$$$$$$a__b(); // $$$$a_b

		@AttributeDefinition(name = "$$$$a.b")
		String x$$$$$$$$a_b(); // $$$$a.b

		@AttributeDefinition(name = "a")
		String xa$(); // a

		@AttributeDefinition(name = "a$")
		String xa$$(); // a$

		@AttributeDefinition(name = "a$")
		String xa$$$(); // a$

		@AttributeDefinition(name = "a$$")
		String xa$$$$(); // a$$

		@AttributeDefinition(name = "a$.$")
		String xa$$_$$(); // a$.$

		@AttributeDefinition(name = "a$_$")
		String xa$$__$$(); // a$_$

		@AttributeDefinition(name = "a..")
		String xa_$_(); // a..

		String noid();

		@AttributeDefinition(name = "§NULL§") //Meta.NULL
		String nullid();
	}
	
	static void assertAD(Document d, String id, String name) throws XPathExpressionException {
		assertAD(d, id, name, null, null, null, 0, "String", null, null, null);
	}

	public static void testNaming() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", Naming.class.getName());
		b.build();
		assertEquals(0, b.getErrors().size());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getWarnings().size());

		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$Naming.xml");
		IO.copy(r.openInputStream(), System.err);
		Document d = db.parse(r.openInputStream(), "UTF-8");
		assertEquals("http://www.osgi.org/xmlns/metatype/v1.3.0", d.getDocumentElement().getNamespaceURI());

		assertAD(d, "secret", "Secret");
		assertAD(d, ".secret", "Secret");
		assertAD(d, "_secret", "Secret");
		assertAD(d, "new", "New");
		assertAD(d, "$new", "New");
		assertAD(d, "a.b.c", "A b c");
		assertAD(d, "a_b_c", "A b c");
		assertAD(d, ".a_b", "A b");
		assertAD(d, "$$$$a_b", "A b");
		assertAD(d, "$$$$a.b", "A b");
		assertAD(d, "a", "A ");
		assertAD(d, "a$", "A ");
		assertAD(d, "a$$", "A ");
		assertAD(d, "a$.$", "A ");
		assertAD(d, "a$_$", "A ");
		assertAD(d, "a..", "A ");
		assertAD(d, "xsecret", "secret");
		assertAD(d, "x.secret", ".secret");
		assertAD(d, "x_secret", "_secret");
		assertAD(d, "xnew", "new");
		assertAD(d, "x$new", "$new");
		assertAD(d, "xa.b.c", "a.b.c");
		assertAD(d, "xa_b_c", "a_b_c");
		assertAD(d, "x.a_b", ".a_b");
		assertAD(d, "x$$$$a_b", "$$$$a_b");
		assertAD(d, "x$$$$a.b", "$$$$a.b");
		assertAD(d, "xa", "a");
		assertAD(d, "xa$", "a$");
		assertAD(d, "xa$$", "a$$");
		assertAD(d, "xa$.$", "a$.$");
		assertAD(d, "xa$_$", "a$_$");
		assertAD(d, "xa..", "a..");
		assertAD(d, "noid", "Noid");
		assertAD(d, "nullid", "§NULL§");
	}

	/**
	 * Test the special conversions.
	 */

	public static class MyList<T> extends ArrayList<T> {
		private static final long	serialVersionUID	= 1L;

		public MyList() {
			System.err.println("Constr");

		}
	}

	static interface CollectionsTest {
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

	public static void xtestCollections() throws Exception {
//		CollectionsTest trt = set(CollectionsTest.class, new int[] {
//				1, 2, 3
//		});
//		List<String> source = Arrays.asList("1", "2", "3");
//
//		assertTrue(trt.collection() instanceof Collection);
//		assertEqualList(source, trt.collection());
//
//		assertTrue(trt.list() instanceof List);
//		assertEqualList(source, trt.list());
//		assertTrue(trt.set() instanceof Set);
//		assertEqualList(source, trt.set());
//		assertTrue(trt.queue() instanceof Queue);
//		assertEqualList(source, trt.queue());
//		// assertTrue( trt.deque() instanceof Deque);
//		// assertEqualList( source, trt.deque());
//		assertTrue(trt.stack() instanceof Stack);
//		assertEqualList(source, trt.stack());
//		assertTrue(trt.arrayList() instanceof ArrayList);
//		assertEqualList(source, trt.arrayList());
//		assertTrue(trt.linkedList() instanceof LinkedList);
//		assertEqualList(source, trt.linkedList());
//		assertTrue(trt.linkedHashSet() instanceof LinkedHashSet);
//		assertEqualList(source, trt.linkedHashSet());
//		assertTrue(trt.myList() instanceof MyList);
//		assertEqualList(source, trt.myList());
	}

//	private static void assertEqualList(List< ? > a, Collection< ? > b) {
//		if (a.size() == b.size()) {
//			for (Object x : a) {
//				if (!b.contains(x))
//					throw new AssertionFailedError("expected:<" + a + "> but was: <" + b + ">");
//			}
//			return;
//		}
//		throw new AssertionFailedError("expected:<" + a + "> but was: <" + b + ">");
//	}

	/**
	 * Test the special conversions.
	 */
	static interface SpecialConversions {
		enum X {
			A, B, C
		}

		X enumv();

		Pattern pattern();

		Class< ? > clazz();

		URI constructor();
	}

	public static void xtestSpecialConversions() throws URISyntaxException {
//		Properties p = new Properties();
//		p.put("enumv", "A");
//		p.put("pattern", ".*");
//		p.put("clazz", "java.lang.Object");
//		p.put("constructor", "http://www.aQute.biz");
//
//		SpecialConversions trt = Configurable.createConfigurable(SpecialConversions.class, (Map<Object,Object>) p);
//		assertEquals(SpecialConversions.X.A, trt.enumv());
//		assertEquals(".*", trt.pattern().pattern());
//		assertEquals(Object.class, trt.clazz());
//		assertEquals(new URI("http://www.aQute.biz"), trt.constructor());
	}


	/**
	 * Test enum handling
	 */

	@ObjectClassDefinition
	public static interface Enums {
		enum X {
			requireConfiguration, optionalConfiguration, ignoreConfiguration
		}

		X r();

		X i();

		X o();
	}

	private static final String[] optionLabels = {"requireConfiguration", "optionalConfiguration", "ignoreConfiguration"};
	private static final String[] optionValues = optionLabels;
	static void assertAD(Document d, String id, String name, String[] optionLabels, String[] optionValues) throws XPathExpressionException {
		assertAD(d, id, name, null, null, null, 0, "String", null, optionLabels, optionValues);
	}
	public static void testEnum() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", Enums.class.getName()+ "*");
		b.build();
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$Enums.xml");
		IO.copy(r.openInputStream(), System.err);

		Document d = db.parse(r.openInputStream());
		assertEquals("http://www.osgi.org/xmlns/metatype/v1.3.0", d.getDocumentElement().getNamespaceURI());

		assertAD(d, "r", "R", optionLabels, optionValues);
		assertAD(d, "i", "I", optionLabels, optionValues);
		assertAD(d, "o", "O", optionLabels, optionValues);
	}

	/**
	 * Test the OCD settings
	 */
	@ObjectClassDefinition(pid={"ocdEmptyPid"})
	public static interface OCDEmpty {}

	@ObjectClassDefinition(description = "description", pid={"ocdDescriptionPid"})
	public static interface OCDDescription {}

	@ObjectClassDefinition(pid={"ocdDesignatePidOnlyPid"})
	public static interface OCDDesignatePidOnly {}

	@ObjectClassDefinition(factoryPid = {"ocdDesignatePidFactoryFactoryPid"})
	public static interface OCDDesignatePidFactory {}

	@ObjectClassDefinition(id = "id", pid={"ocdIdPid"})
	public static interface OCDId {}

	@ObjectClassDefinition(id = "id2", pid={"ocdId2Pid"})
	public static interface OCDIdWithPid {}

	@ObjectClassDefinition(localization = "localization", pid={"ocdLocalizationPid"})
	public static interface OCDLocalization {}

	@ObjectClassDefinition(name = "name", pid={"ocdNamePid"})
	public static interface OCDName {}

	public static void testOCD() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", "*");
		b.build();
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());

		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDEmpty",
				"test.metatype.SpecMetatypeTest$OCDEmpty",
				"Test metatype spec metatype test OCDEmpty", 
				null,
				"ocdEmptyPid", 
				false,
				null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDName",
				"test.metatype.SpecMetatypeTest$OCDName",
				"name", 
				null,
				"ocdNamePid", 
				false, 
				null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDDescription", 
				"test.metatype.SpecMetatypeTest$OCDDescription",
				"Test metatype spec metatype test OCDDescription", 
				"description", 
				"ocdDescriptionPid",
				false,
				null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDDesignatePidOnly",
				"test.metatype.SpecMetatypeTest$OCDDesignatePidOnly", 
				"Test metatype spec metatype test OCDDesignate pid only",
				null,
				"ocdDesignatePidOnlyPid", 
				false,
				null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDDesignatePidFactory",
				"test.metatype.SpecMetatypeTest$OCDDesignatePidFactory", 
				"Test metatype spec metatype test OCDDesignate pid factory", 
				null,
				"ocdDesignatePidFactoryFactoryPid",
				true,
				null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDId",
				"id",
				"Id", //5.2 name is derived from id 
				null,
				"ocdIdPid", 
				false, 
				null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDIdWithPid",
				"id2", 
				"Id2", 
				null, 
				"ocdId2Pid",
				false,
				null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDLocalization",
				"test.metatype.SpecMetatypeTest$OCDLocalization",
				"Test metatype spec metatype test OCDLocalization", 
				null, 
				"ocdLocalizationPid",
				false,
				"localization");
	}

	static void assertOCD(Builder b, String cname, String id, String name, String description, String designate,
			boolean factory, String localization) throws Exception {
		Resource r = b.getJar().getResource("OSGI-INF/metatype/" + id + ".xml");
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);
		Document d = db.parse(r.openInputStream());
		assertEquals(id, xpath.evaluate("//OCD/@id", d, XPathConstants.STRING));
		assertEquals(name, xpath.evaluate("//OCD/@name", d, XPathConstants.STRING));
		String expected = localization == null ? "OSGI-INF/I10n/" + cname : localization;
		String actual = (String)xpath.evaluate("//@localization", d, XPathConstants.STRING);
		assertEquals(localization == null ? "OSGI-INF/l10n/" + cname : localization,
				xpath.evaluate("//@localization", d, XPathConstants.STRING));
		assertEquals(description == null ? "" : description,
				xpath.evaluate("//OCD/@description", d, XPathConstants.STRING));

		if (designate != null) {
			if (factory)
				assertEquals(designate, xpath.evaluate("//Designate/@factoryPid", d, XPathConstants.STRING));
			else
				assertEquals(designate, xpath.evaluate("//Designate/@pid", d, XPathConstants.STRING));
		}

		assertEquals(id, xpath.evaluate("//Object/@ocdref", d, XPathConstants.STRING));
	}

	/**
	 * Test the AD settings.
	 */

	@ObjectClassDefinition(description = "advariations")
	public static interface TestAD {
		@AttributeDefinition
		String noSettings();

		@AttributeDefinition(name = "id")
		String withId();

		@AttributeDefinition(name = "name")
		String withName();

		@AttributeDefinition(max = "1")
		String withMax();

		@AttributeDefinition(min = "-1")
		String withMin();

		@AttributeDefinition(defaultValue = {"deflt"})
		String withDefault();

		@AttributeDefinition(cardinality = 0)
		String[] withC0();

		@AttributeDefinition(cardinality = 1)
		String[] withC1();

		@AttributeDefinition(cardinality = -1)
		Collection<String> withC_1();

		@AttributeDefinition(cardinality = -1)
		String[] withC_1ButArray();

		@AttributeDefinition(cardinality = 1)
		Collection<String> withC1ButCollection();

		@AttributeDefinition(type = AttributeType.STRING)
		int withInt();

		@AttributeDefinition(type = AttributeType.INTEGER)
		String withString();

		@AttributeDefinition(description = "description_xxx\"xxx'xxx")
		String a();

		@AttributeDefinition(options = {@Option(label= "a", value = "a"), @Option(label= "b", value = "b")})
		String valuesOnly();

		@AttributeDefinition(options = {@Option(label= "a", value = "A"), @Option(label= "b", value = "B")})
		String labelsAndValues();

		@AttributeDefinition(required = true)
		String required();

		@AttributeDefinition(required = false)
		String notRequired();
	}

	public static void testAD() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", TestAD.class.getName());
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestAD.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		Document d = db.parse(r.openInputStream());

		assertAD(d, "noSettings", "No settings", null, null, null, 0, "String", null, null, null);
		assertAD(d, "withId", "id", null, null, null, 0, "String", null, null, null);
		assertAD(d, "withName", "name", null, null, null, 0, "String", null, null, null);
		assertAD(d, "withMax", "With max", null, "1", null, 0, "String", null, null, null);
		assertAD(d, "withMin", "With min", "-1", null, null, 0, "String", null, null, null);
		assertAD(d, "withC1", "With c1", null, null, null, 1, "String", null, null, null);
		assertAD(d, "withC0", "With c0", null, null, null, 0, "String", null, null, null);
		assertAD(d, "withC.1", "With c 1", null, null, null, -1, "String", null, null, null);
		assertAD(d, "withC.1ButArray", "With c 1 but array", null, null, null, -1, "String", null, null,
				null);
		assertAD(d, "withC1ButCollection", "With c1 but collection", null, null, null, 1, "String",
				null, null, null);
		assertAD(d, "withInt", "With int", null, null, null, 0, "String", null, null, null);
		assertAD(d, "withString", "With string", null, null, null, 0, "Integer", null, null, null);
		assertAD(d, "a", "A", null, null, null, 0, "String", "description_xxx\"xxx'xxx", null, null);
		assertAD(d, "valuesOnly", "Values only", null, null, null, 0, "String", null, new String[] {
				"a", "b"
		}, new String[] {
				"a", "b"
		});
		assertAD(d, "labelsAndValues", "Labels and values", null, null, null, 0, "String", null, new String[] {
				"a", "b"
		},
				new String[] {
						"A", "B"
				});
	}

	/**
	 * Test the AD inheritance.
	 */

	@ObjectClassDefinition(description = "adinheritance-super-one")
	public static interface TestADWithInheritanceSuperOne {
		@AttributeDefinition
		String fromSuperOne();
	}

	@ObjectClassDefinition(description = "adinheritance-super")
	public static interface TestADWithInheritanceSuperTwo {
		@AttributeDefinition
		String fromSuperTwo();
	}

	@ObjectClassDefinition(description = "adinheritance-child")
	public static interface TestADWithInheritanceChild extends TestADWithInheritanceSuperOne,
		TestADWithInheritanceSuperTwo {
		@AttributeDefinition
		String fromChild();
	}
	
	public static void testADWithInheritance() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", TestADWithInheritanceChild.class.getName());
		b.setProperty("-metatypeannotations-inherit", "true");
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestADWithInheritanceChild.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);
		
		Document d = db.parse(r.openInputStream());
		
		assertAD(d, "fromChild", "From child", null, null, null, 0, "String", null, null, null);
//		assertAD(d, "fromSuperOne", "From super one", null, null, null, 0, "String", null, null, null);
//		assertAD(d, "fromSuperTwo", "From super two", null, null, null, 0, "String", null, null, null);
	}

	@SuppressWarnings("null")
	static void assertAD(Document d,
	String id, String name, String min, String max, String deflt, int cardinality, String type, String description,
			String[] optionLabels, 
			String[] optionValues) throws XPathExpressionException {
		assertEquals(name, xpath.evaluate("//OCD/AD[@id='" + id + "']/@name", d, XPathConstants.STRING));
		assertEquals(id, xpath.evaluate("//OCD/AD[@id='" + id + "']/@id", d, XPathConstants.STRING));
		assertEquals(min == null ? "" : min,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@min", d, XPathConstants.STRING));
		assertEquals(max == null ? "" : max,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@max", d, XPathConstants.STRING));
		assertEquals(deflt == null ? "" : deflt,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@deflt", d, XPathConstants.STRING));
		if (cardinality == 0) {
			assertEquals("",
					xpath.evaluate("//OCD/AD[@id='" + id + "']/@cardinality", d, XPathConstants.STRING));
		} else {
			assertEquals(cardinality + "",
					xpath.evaluate("//OCD/AD[@id='" + id + "']/@cardinality", d, XPathConstants.STRING));
		}
		assertEquals(type, xpath.evaluate("//OCD/AD[@id='" + id + "']/@type", d, XPathConstants.STRING));
		assertEquals(description == null ? "" : description,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@description", d, XPathConstants.STRING));
		assertEquals(optionLabels == null, optionValues == null);
		if (optionLabels != null) {
			assertEquals(optionLabels.length, optionValues.length);
			
			//option count is correct
			assertEquals(Double.valueOf(optionLabels.length), xpath.evaluate("count(//OCD/AD[@id='" + id + "']/Option)", d, XPathConstants.NUMBER));
			for (int i = 0; i < optionLabels.length; i++) {
				String expr = "//OCD/AD[@id='" + id + "']/Option[@label='" + optionLabels[i] + "']/@value";
				assertEquals(optionValues[i], xpath.evaluate(expr, d, XPathConstants.STRING));
			}
			
		}
	}

	/**
	 * Test all the return types.
	 */
	@ObjectClassDefinition(description = "simple", name = "TestSimple")
	public static interface TestReturnTypes {
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
/*
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
*/
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

	public static void testReturnTypes() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", TestReturnTypes.class.getName());
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestReturnTypes.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		Document d = db.parse(r.openInputStream());
		assertEquals("http://www.osgi.org/xmlns/metatype/v1.3.0", d.getDocumentElement().getNamespaceURI());
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
/*
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
		*/
	}

	/**
	 * Test simple
	 * 
	 * @author aqute
	 */
	@ObjectClassDefinition(description = "simple", name = "TestSimple", pid={"simplePid"})
	public static interface TestSimple {
		@AttributeDefinition
		String simple();

		String[] notSoSimple();

		Collection<String> stringCollection();

		@AttributeDefinition(defaultValue = {"true"}, required=false)
		boolean enabled();
	}

	public static void testSimple() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", TestSimple.class.getName());
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestSimple.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		Document d = db.parse(r.openInputStream());

		assertEquals("TestSimple", xpath.evaluate("//OCD/@name", d));
		assertEquals("simple", xpath.evaluate("//OCD/@description", d));
		assertEquals("test.metatype.SpecMetatypeTest$TestSimple", xpath.evaluate("//OCD/@id", d));
		assertEquals("simplePid", xpath.evaluate("//Designate/@pid", d));
		assertEquals("test.metatype.SpecMetatypeTest$TestSimple", xpath.evaluate("//Object/@ocdref", d));
		assertEquals("simple", xpath.evaluate("//OCD/AD[@id='simple']/@id", d));
		assertEquals("Simple", xpath.evaluate("//OCD/AD[@id='simple']/@name", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='simple']/@type", d));
		assertEquals("false", xpath.evaluate("//OCD/AD[@id='enabled']/@required", d));
		assertEquals("true", xpath.evaluate("//OCD/AD[@id='enabled']/@default", d));
		assertEquals(Integer.MAX_VALUE + "", xpath.evaluate("//OCD/AD[@id='notSoSimple']/@cardinality", d));

	}


	@ObjectClassDefinition
	public static @interface DesignateOCD {
		
	}
	
	@Component(configurationPid="simplePid")
	@Designate(ocd = DesignateOCD.class, factory=true)
	public static class DesignateComponent {}
	
	public static void testDesignate() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", DesignateOCD.class.getName());
		b.setProperty("-dsannotations", DesignateComponent.class.getName());
		b.build();
		{
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$DesignateOCD.xml");
		assertEquals(b.getErrors().toString(), 0, b.getErrors().size());
		assertEquals(b.getWarnings().toString(), 0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		Document d = db.parse(r.openInputStream());
		assertEquals("Test metatype spec metatype test designate OCD", xpath.evaluate("//OCD/@name", d));
		assertEquals("test.metatype.SpecMetatypeTest$DesignateOCD", xpath.evaluate("//OCD/@id", d));
		}
		{
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$DesignateComponent.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		Document d = db.parse(r.openInputStream());
		assertEquals("simplePid", xpath.evaluate("//Designate/@factoryPid", d));
		assertEquals("test.metatype.SpecMetatypeTest$DesignateOCD", xpath.evaluate("//Object/@ocdref", d));
		}
	}
}
