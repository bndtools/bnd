package test;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.namespace.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import junit.framework.*;

import org.w3c.dom.*;

import aQute.bnd.annotation.metatype.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;

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
	
	
	/** 
	 * Test enum handling
	 */
	
	@Metadata.OCD public static interface Enums {
		enum X { requireConfiguration, optionalConfiguration, ignoreConfiguration};
		
		X r();
		X i();
		X o();
	}

	public void testEnum() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test");
		b.setProperty("-metatype", "*");
		b.build();
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.MetatypeTest$Enums.xml");
		IO.copy(r.openInputStream(), System.out);

		Document d = db.parse(r.openInputStream());
		assertEquals("http://www.osgi.org/xmlns/metatype/v1.1.0", d.getDocumentElement()
				.getNamespaceURI());
		
		
		Properties p = new Properties();
		p.setProperty("r", "requireConfiguration");
		p.setProperty("i", "ignoreConfiguration");
		p.setProperty("o", "optionalConfiguration");
		Enums enums = Configurable.createConfigurable(Enums.class, p);
		assertEquals( Enums.X.requireConfiguration, enums.r());
		assertEquals( Enums.X.ignoreConfiguration, enums.i());
		assertEquals( Enums.X.optionalConfiguration, enums.o());
	}
	
	
	
	/**
	 * Test the OCD settings
	 */
	@Metadata.OCD() public static interface OCDEmpty {
	}

	@Metadata.OCD(description = "description") public static interface OCDDescription {
	}

	@Metadata.OCD(designate = "pid") public static interface OCDDesignatePidOnly {
	}

	@Metadata.OCD(designateFactory = "pid") public static interface OCDDesignatePidFactory {
	}

	@Metadata.OCD(id = "id") public static interface OCDId {
	}

	@Metadata.OCD(id = "id", designate = "pid") public static interface OCDIdWithPid {
	}

	@Metadata.OCD(localization = "localization") public static interface OCDLocalization {
	}

	@Metadata.OCD(name = "name") public static interface OCDName {
	}

	public void testOCD() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test");
		b.setProperty("-metatype", "*");
		b.build();
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.out.println(b.getJar().getResources().keySet());

		assertOCD(b, "test.MetatypeTest$OCDEmpty", "test.MetatypeTest$OCDEmpty",
				"Metatype test OCDEmpty", null, "test.MetatypeTest$OCDEmpty", false,
				null);
		assertOCD(b, "test.MetatypeTest$OCDName", "test.MetatypeTest$OCDName",
				"name", null, "test.MetatypeTest$OCDName", false,
				null);
		assertOCD(b, "test.MetatypeTest$OCDDescription", "test.MetatypeTest$OCDDescription",
				"Metatype test OCDDescription", "description", "test.MetatypeTest$OCDDescription", false,
				null);
		assertOCD(b, "test.MetatypeTest$OCDDesignatePidOnly", "test.MetatypeTest$OCDDesignatePidOnly",
				"Metatype test OCDDesignate pid only", null, "pid", false,
				null);
		assertOCD(b, "test.MetatypeTest$OCDDesignatePidFactory", "test.MetatypeTest$OCDDesignatePidFactory",
				"Metatype test OCDDesignate pid factory", null, "pid", true,
				null);
		assertOCD(b, "test.MetatypeTest$OCDId","id", 
				"Metatype test OCDId", null, "id", false,
				null);
		assertOCD(b, "test.MetatypeTest$OCDIdWithPid","id", 
				"Metatype test OCDId with pid", null, "pid", false,
				null);
		assertOCD(b, "test.MetatypeTest$OCDLocalization", "test.MetatypeTest$OCDLocalization",
				"Metatype test OCDLocalization", null, "test.MetatypeTest$OCDLocalization", false,
				"localization");
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

	@Metadata.OCD(description = "advariations") public static interface TestAD {
		@Metadata.AD String noSettings();

		@Metadata.AD(id = "id") String withId();

		@Metadata.AD(name = "name") String withName();

		@Metadata.AD(max = "1") String withMax();

		@Metadata.AD(min = "-1") String withMin();

		@Metadata.AD(deflt = "deflt") String withDefault();

		@Metadata.AD(cardinality = 0) String[] withC0();

		@Metadata.AD(cardinality = 1) String[] withC1();

		@Metadata.AD(cardinality = -1) Collection<String> withC_1();

		@Metadata.AD(cardinality = -1) String[] withC_1ButArray();

		@Metadata.AD(cardinality = 1) Collection<String> withC1ButCollection();

		@Metadata.AD(type = "String") int withInt();

		@Metadata.AD(type = "Integer") String withString();

		@Metadata.AD(description = "description") String a();

		@Metadata.AD(optionValues = { "a", "b" }) String valuesOnly();

		@Metadata.AD(optionValues = { "a", "b" }, optionLabels = { "A", "B" }) String labelsAndValues();

		@Metadata.AD(required = true) String required();

		@Metadata.AD(required = false) String notRequired();
	}

	public void testAD() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test");
		b.setProperty("-metatype", "*");
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.MetatypeTest$TestAD.xml");
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
		assertAD(d, "withMax", "With max", "withMax", null, "1", null, 0, "String", null, null, null);
		assertAD(d, "withMin", "With min", "withMin", "-1", null, null, 0, "String", null, null,
				null);
		assertAD(d, "withC1", "With c1", "withC1", null, null, null, 1, "String", null, null, null);
		assertAD(d, "withC0", "With c0", "withC0", null, null, null, 2147483647, "String", null,
				null, null);
		assertAD(d, "withC_1", "With c1", "withC_1", null, null, null, -1, "String", null, null,
				null);
		assertAD(d, "withC_1ButArray", "With c1 but array", "withC_1ButArray", null, null, null, -1,
				"String", null, null, null);
		assertAD(d, "withC1ButCollection", "With c1 but collection", "withC1ButCollection", null,
				null, null, 1, "String", null, null, null);
		assertAD(d, "withInt", "With int", "withInt", null, null, null, 0, "String", null, null,
				null);
		assertAD(d, "withString", "With string", "withString", null, null, null, 0, "Integer", null,
				null, null);
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
		assertEquals(id,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@id", d, XPathConstants.STRING));
		assertEquals(min == null ? "" : min,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@min", d, XPathConstants.STRING));
		assertEquals(max == null ? "" : max,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@max", d, XPathConstants.STRING));
		assertEquals(deflt == null ? "" : deflt,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@deflt", d, XPathConstants.STRING));
		assertEquals(cardinality + "", xpath.evaluate("//OCD/AD[@id='" + id
				+ "']/@cardinality", d, XPathConstants.STRING));
		assertEquals(type,
				xpath.evaluate("//OCD/AD[@id='" + id + "']/@type", d, XPathConstants.STRING));
		assertEquals(description == null ? "" : description, xpath.evaluate("//OCD/AD[@id='"
				+ id + "']/@description", d, XPathConstants.STRING));
	}

	/**
	 * Test all the return types.
	 */
	@Metadata.OCD(description = "simple", name = "TestSimple") public static interface TestReturnTypes {
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

		URL rURL();

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

		Collection<URL> rURLs();

		Boolean[] raBoolean();

		Byte[] raByte();

		Character[] raCharacter();

		Short[] raShort();

		Integer[] raInt();

		Long[] raLong();

		Float[] raFloat();

		Double[] raDouble();

		String[] raString();

		URL[] raURL();
	}

	public void testReturnTypes() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test");
		b.setProperty("-metatype", "*");
		b.build();
		Resource r = b.getJar().getResource(
				"OSGI-INF/metatype/test.MetatypeTest$TestReturnTypes.xml");
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
		assertEquals("Char", xpath.evaluate("//OCD/AD[@id='rpCharacter']/@type", d));
		assertEquals("Short", xpath.evaluate("//OCD/AD[@id='rpShort']/@type", d));
		assertEquals("Integer", xpath.evaluate("//OCD/AD[@id='rpInt']/@type", d));
		assertEquals("Long", xpath.evaluate("//OCD/AD[@id='rpLong']/@type", d));
		assertEquals("Float", xpath.evaluate("//OCD/AD[@id='rpFloat']/@type", d));
		assertEquals("Double", xpath.evaluate("//OCD/AD[@id='rpDouble']/@type", d));

		// Primitive Wrappers
		assertEquals("Boolean", xpath.evaluate("//OCD/AD[@id='rBoolean']/@type", d));
		assertEquals("Byte", xpath.evaluate("//OCD/AD[@id='rByte']/@type", d));
		assertEquals("Char", xpath.evaluate("//OCD/AD[@id='rCharacter']/@type", d));
		assertEquals("Short", xpath.evaluate("//OCD/AD[@id='rShort']/@type", d));
		assertEquals("Integer", xpath.evaluate("//OCD/AD[@id='rInt']/@type", d));
		assertEquals("Long", xpath.evaluate("//OCD/AD[@id='rLong']/@type", d));
		assertEquals("Float", xpath.evaluate("//OCD/AD[@id='rFloat']/@type", d));
		assertEquals("Double", xpath.evaluate("//OCD/AD[@id='rDouble']/@type", d));

		// Primitive Arrays
		assertEquals("Boolean", xpath.evaluate("//OCD/AD[@id='rpaBoolean']/@type", d));
		assertEquals("Byte", xpath.evaluate("//OCD/AD[@id='rpaByte']/@type", d));
		assertEquals("Char", xpath.evaluate("//OCD/AD[@id='rpaCharacter']/@type", d));
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
		assertEquals("Char", xpath.evaluate("//OCD/AD[@id='raCharacter']/@type", d));
		assertEquals("Short", xpath.evaluate("//OCD/AD[@id='raShort']/@type", d));
		assertEquals("Integer", xpath.evaluate("//OCD/AD[@id='raInt']/@type", d));
		assertEquals("Long", xpath.evaluate("//OCD/AD[@id='raLong']/@type", d));
		assertEquals("Float", xpath.evaluate("//OCD/AD[@id='raFloat']/@type", d));
		assertEquals("Double", xpath.evaluate("//OCD/AD[@id='raDouble']/@type", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='raString']/@type", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='raURL']/@type", d));

		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raBoolean']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raByte']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raCharacter']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raShort']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raInt']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raLong']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raFloat']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raDouble']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raString']/@cardinality", d));
		assertEquals("2147483647", xpath.evaluate("//OCD/AD[@id='raURL']/@cardinality", d));

		// Wrapper + Object collections
		assertEquals("Boolean", xpath.evaluate("//OCD/AD[@id='rBooleans']/@type", d));
		assertEquals("Byte", xpath.evaluate("//OCD/AD[@id='rBytes']/@type", d));
		assertEquals("Char", xpath.evaluate("//OCD/AD[@id='rCharacter']/@type", d));
		assertEquals("Short", xpath.evaluate("//OCD/AD[@id='rShorts']/@type", d));
		assertEquals("Integer", xpath.evaluate("//OCD/AD[@id='rInts']/@type", d));
		assertEquals("Long", xpath.evaluate("//OCD/AD[@id='rLongs']/@type", d));
		assertEquals("Float", xpath.evaluate("//OCD/AD[@id='rFloats']/@type", d));
		assertEquals("Double", xpath.evaluate("//OCD/AD[@id='rDoubles']/@type", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='rStrings']/@type", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='rURLs']/@type", d));

		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rBooleans']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rBytes']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rCharacters']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rShorts']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rInts']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rLongs']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rFloats']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rDoubles']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rStrings']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='rURLs']/@cardinality", d));
	}

	/**
	 * Test simple
	 * 
	 * @author aqute
	 * 
	 */
	@Metadata.OCD(description = "simple", name = "TestSimple") public static interface TestSimple {
		@Metadata.AD String simple();

		String[] notSoSimple();

		Collection<String> stringCollection();
	}

	public void testSimple() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test");
		b.setProperty("-metatype", "*");
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.MetatypeTest$TestSimple.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.out.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.out);

		Document d = db.parse(r.openInputStream());

		assertEquals("TestSimple", xpath.evaluate("//OCD/@name", d));
		assertEquals("simple", xpath.evaluate("//OCD/@description", d));
		assertEquals("test.MetatypeTest$TestSimple", xpath.evaluate("//OCD/@id", d));
		assertEquals("test.MetatypeTest$TestSimple", xpath.evaluate("//Designate/@pid", d));
		assertEquals("test.MetatypeTest$TestSimple", xpath.evaluate("//Object/@ocdref", d));
		assertEquals("simple", xpath.evaluate("//OCD/AD[@id='simple']/@id", d));
		assertEquals("Simple", xpath.evaluate("//OCD/AD[@id='simple']/@name", d));
		assertEquals("String", xpath.evaluate("//OCD/AD[@id='simple']/@type", d));
		assertEquals(Integer.MAX_VALUE + "",
				xpath.evaluate("//OCD/AD[@id='notSoSimple']/@cardinality", d));

	}

}
