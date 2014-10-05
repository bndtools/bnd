package test.metatype;

import java.io.*;
import java.net.*;
import java.util.*;

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

		String b$$$(); // b$

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
		String xb$$$(); // b$

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
		System.out.println(b.getErrors());
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
		assertAD(d, "b$", "B ");
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
		assertAD(d, "xb$", "a$");
		assertAD(d, "xa$$", "a$$");
		assertAD(d, "xa$.$", "a$.$");
		assertAD(d, "xa$_$", "a$_$");
		assertAD(d, "xa..", "a..");
		assertAD(d, "noid", "Noid");
		assertAD(d, "nullid", "§NULL§");
	}
	
	@ObjectClassDefinition
	public static interface ADCollision {

		String a$$(); // a$

		String a$$$(); // a$
	}
	
	public static void testADCollision() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", ADCollision.class.getName());
		b.build();
		System.out.println(b.getErrors());
		assertEquals(1, b.getErrors().size());
	}
	
	@ObjectClassDefinition(id="duplicate")
	public static interface DupOCDId1 {
		
	}

	@ObjectClassDefinition(id="duplicate")
	public static interface DupOCDId2 {
		
	}

	public static void testOCDCollision() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		String name = DupOCDId2.class.getName();
		
		b.setProperty("-metatypeannotations", name.substring(0, name.length() - "1".length()) + "*");
		b.build();
		assertEquals(1, b.getErrors().size());
	}
	
	@ObjectClassDefinition(pid={"1"})
	public static interface DupPid1 {
		
	}
	@ObjectClassDefinition(pid={"1"})
	public static interface DupPid2 {
		
	}
	@ObjectClassDefinition(factoryPid={"2"})
	public static interface DupPid3 {
		
	}
	@ObjectClassDefinition(factoryPid={"2"})
	public static interface DupPid4 {
		
	}
	@ObjectClassDefinition(pid={"3"}, factoryPid={"3"})
	public static interface DupPid5 {
		
	}
	@ObjectClassDefinition(pid={"4"})
	public static interface DupPid6 {
		
	}
	@ObjectClassDefinition(factoryPid={"4"})
	public static interface DupPid7 {
		
	}

	public static void testPidCollision() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		String name = DupPid2.class.getName();
		
		b.setProperty("-metatypeannotations", name.substring(0, name.length() - "1".length()) + "*");
		b.build();
		System.err.println(b.getErrors());
		assertEquals(4, b.getErrors().size());
	}
	
	/**
	 * Test the special conversions.
	 */

	public static class MyList<T> extends ArrayList<T> {
		private static final long	serialVersionUID	= 1L;

		public MyList() {
		}
		
	}
	
	interface L<T> extends List<T> {}
	
	public static class StringList<T> implements L<T> {

		@Override
		public void add(int arg0, T arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean addAll(int arg0, Collection< ? extends T> arg1) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public T get(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int indexOf(Object arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int lastIndexOf(Object arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public ListIterator<T> listIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ListIterator<T> listIterator(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T remove(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T set(int arg0, T arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<T> subList(int arg0, int arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean add(T arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean addAll(Collection< ? extends T> arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void clear() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean contains(Object arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean containsAll(Collection< ? > arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEmpty() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Iterator<T> iterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean remove(Object arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean removeAll(Collection< ? > arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean retainAll(Collection< ? > arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object[] toArray() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		@SuppressWarnings("hiding")
		public <T> T[] toArray(T[] arg0) {
			// TODO Auto-generated method stub
			return null;
		}
    }

	@ObjectClassDefinition
	static interface CollectionsTest {
		Collection<String> collection();

		List<String> list();

		Set<String> set();

//		Queue<String> queue();

		// Deque<String> deque();
//		Stack<String> stack();

		ArrayList<String> arrayList();

		LinkedList<String> linkedList();

		LinkedHashSet<String> linkedHashSet();

		MyList<String> myList();
		
		StringList<String> stringList();
	}

	public static void testCollections() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", CollectionsTest.class.getName());
		b.build();
		System.out.println(b.getErrors());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		System.err.println(b.getJar().getResources());
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$CollectionsTest.xml");
		assertNotNull(r);
        IO.copy(r.openInputStream(), System.err);

		Document d = db.parse(r.openInputStream());
		assertEquals("http://www.osgi.org/xmlns/metatype/v1.3.0", d.getDocumentElement().getNamespaceURI());
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='collection']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='list']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='set']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='arrayList']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='linkedList']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='linkedHashSet']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='myList']/@cardinality", d));
		assertEquals("-2147483648", xpath.evaluate("//OCD/AD[@id='stringList']/@cardinality", d));
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
		System.err.println(b.getErrors());
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
		String name = OCDEmpty.class.getName();
		
		b.setProperty("-metatypeannotations", name.substring(0, name.length() - "Empty".length()) + "*");
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
		b.setProperty("-metatypeannotations", TestADWithInheritanceChild.class.getName().substring(0, TestADWithInheritanceChild.class.getName().length() - "Child".length()) + "*" );
//		b.setProperty("-metatypeannotations-inherit", "true");
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestADWithInheritanceChild.xml");
		System.err.println(b.getErrors());
		assertEquals(0, b.getErrors().size());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);
		
		Document d = db.parse(r.openInputStream());
		
		assertAD(d, "fromChild", "From child", null, null, null, 0, "String", null, null, null);
		assertAD(d, "fromSuperOne", "From super one", null, null, null, 0, "String", null, null, null);
		assertAD(d, "fromSuperTwo", "From super two", null, null, null, 0, "String", null, null, null);
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

	public static void testReturnTypes() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", TestReturnTypes.class.getName());
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestReturnTypes.xml");
		System.err.println(b.getErrors());
		assertEquals(0, b.getErrors().size());
		System.err.println(b.getWarnings());
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
	public static @interface AnnotationDefaults {
		int integer() default 1;
		int[] integers() default {2,3};
		
		boolean bool() default true;
		boolean[] bools() default {true, false};
		
		Class<?> clazz() default String.class;
		Class<?>[] clazzs() default {Integer.class, Double.class};
		
		enum L {A, B, C}
		
		L l() default L.A;
		L[] ls() default{L.B, L.C};
		
		String string() default "foo";
		String[] strings() default {"bar", "baz"};
	}
	
	public static void testAnnotationDefaults() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", AnnotationDefaults.class.getName());
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$AnnotationDefaults.xml");
		System.err.println(b.getErrors());
		assertEquals(0, b.getErrors().size());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		Document d = db.parse(r.openInputStream());
		assertEquals("1", xpath.evaluate("//OCD/AD[@id='integer']/@default", d));
		assertEquals("2,3", xpath.evaluate("//OCD/AD[@id='integers']/@default", d));
		
		assertEquals("true", xpath.evaluate("//OCD/AD[@id='bool']/@default", d));
		assertEquals("true,false", xpath.evaluate("//OCD/AD[@id='bools']/@default", d));
		
		assertEquals(String.class.getName(), xpath.evaluate("//OCD/AD[@id='clazz']/@default", d));
		assertEquals(Integer.class.getName() + "," + Double.class.getName(), xpath.evaluate("//OCD/AD[@id='clazzs']/@default", d));
		
		assertEquals("A", xpath.evaluate("//OCD/AD[@id='l']/@default", d));
		assertEquals("B,C", xpath.evaluate("//OCD/AD[@id='ls']/@default", d));
		
		assertEquals("foo", xpath.evaluate("//OCD/AD[@id='string']/@default", d));
		assertEquals("bar,baz", xpath.evaluate("//OCD/AD[@id='strings']/@default", d));
		
	}
	
	@ObjectClassDefinition
	public static abstract class Abstract {
		
	}
	
	public static void testAbstract() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", Abstract.class.getName());
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$Abstract.xml");
		assertEquals(1, b.getErrors().size());
		assertNull(r);
	}
	
	@ObjectClassDefinition
	public static @interface NestedInner {
		
	}
	
	@ObjectClassDefinition
	public static @interface NestedOuter {
		NestedInner inner();
	}
	
	public static void testNested() throws Exception {
		{
			Builder b = new Builder();
			b.addClasspath(new File("bin"));
			b.setProperty("Export-Package", "test.metatype");
			b.setProperty("-metatypeannotations", NestedInner.class.getName() + "," + NestedOuter.class.getName());
			b.build();
			Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$NestedOuter.xml");
			assertEquals(1, b.getErrors().size());
		}
		{
			Builder b = new Builder();
			b.addClasspath(new File("bin"));
			b.setProperty("Export-Package", "test.metatype");
			b.setProperty("-metatypeannotations", NestedInner.class.getName() + "," + NestedOuter.class.getName());
			b.setProperty("-metatypeannotations-flags", "nested");
			b.build();
			Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$NestedOuter.xml");
			assertEquals(0, b.getErrors().size());
			assertEquals(0, b.getWarnings().size());
			System.err.println(b.getJar().getResources().keySet());
			assertNotNull(r);
			IO.copy(r.openInputStream(), System.err);

			Document d = db.parse(r.openInputStream());
			assertEquals("String", xpath.evaluate("//OCD/AD[@id='inner']/@type", d));
		}
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
