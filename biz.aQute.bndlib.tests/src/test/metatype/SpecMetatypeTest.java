package test.metatype;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.xpath.*;

import junit.framework.*;

import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;

import aQute.bnd.metatype.*;
import aQute.bnd.osgi.*;
import aQute.bnd.test.*;
import aQute.lib.io.*;

@SuppressWarnings("resource")
public class SpecMetatypeTest extends TestCase {
	
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
	
	static void assertAD(XmlTester xt, String id, String name) throws XPathExpressionException {
		assertAD(xt, id, name, null, null, null, 0, "String", null, null, null);
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
		XmlTester xt = xmlTester13(r);

		assertAD(xt, "secret", "Secret");
		assertAD(xt, ".secret", "Secret");
		assertAD(xt, "_secret", "Secret");
		assertAD(xt, "new", "New");
		assertAD(xt, "$new", "New");
		assertAD(xt, "a.b.c", "A b c");
		assertAD(xt, "a_b_c", "A b c");
		assertAD(xt, ".a_b", "A b");
		assertAD(xt, "$$$$a_b", "A b");
		assertAD(xt, "$$$$a.b", "A b");
		assertAD(xt, "a", "A ");
		assertAD(xt, "a$", "A ");
		assertAD(xt, "b$", "B ");
		assertAD(xt, "a$$", "A ");
		assertAD(xt, "a$.$", "A ");
		assertAD(xt, "a$_$", "A ");
		assertAD(xt, "a..", "A ");
		assertAD(xt, "xsecret", "secret");
		assertAD(xt, "x.secret", ".secret");
		assertAD(xt, "x_secret", "_secret");
		assertAD(xt, "xnew", "new");
		assertAD(xt, "x$new", "$new");
		assertAD(xt, "xa.b.c", "a.b.c");
		assertAD(xt, "xa_b_c", "a_b_c");
		assertAD(xt, "x.a_b", ".a_b");
		assertAD(xt, "x$$$$a_b", "$$$$a_b");
		assertAD(xt, "x$$$$a.b", "$$$$a.b");
		assertAD(xt, "xa", "a");
		assertAD(xt, "xa$", "a$");
		assertAD(xt, "xb$", "a$");
		assertAD(xt, "xa$$", "a$$");
		assertAD(xt, "xa$.$", "a$.$");
		assertAD(xt, "xa$_$", "a$_$");
		assertAD(xt, "xa..", "a..");
		assertAD(xt, "noid", "Noid");
		assertAD(xt, "nullid", "§NULL§");
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

		XmlTester xt = xmlTester13(r);

		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='collection']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='list']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='set']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='arrayList']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='linkedList']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='linkedHashSet']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='myList']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='stringList']/@cardinality");
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
	static void assertAD(XmlTester xt, String id, String name, String[] optionLabels, String[] optionValues) throws XPathExpressionException {
		assertAD(xt, id, name, null, null, null, 0, "String", null, optionLabels, optionValues);
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

		XmlTester xt = xmlTester13(r);

		assertAD(xt, "r", "R", optionLabels, optionValues);
		assertAD(xt, "i", "I", optionLabels, optionValues);
		assertAD(xt, "o", "O", optionLabels, optionValues);
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
		XmlTester xt = xmlTester13(r);
		xt.assertExactAttribute(id, "//OCD/@id");
		xt.assertExactAttribute(name, "//OCD/@name");
		String expected = localization == null ? "OSGI-INF/I10n/" + cname : localization;
		xt.assertExactAttribute(localization == null ? "OSGI-INF/l10n/" + cname : localization,
				"//@localization");
		xt.assertExactAttribute(description == null ? "" : description,
				"//OCD/@description");

		if (designate != null) {
			if (factory)
				xt.assertExactAttribute(designate, "//Designate/@factoryPid");
			else
				xt.assertExactAttribute(designate, "//Designate/@pid");
		}

		xt.assertExactAttribute(id, "//Object/@ocdref");
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

		XmlTester xt = xmlTester13(r);

		assertAD(xt, "noSettings", "No settings", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "withId", "id", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "withName", "name", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "withMax", "With max", null, "1", null, 0, "String", null, null, null);
		assertAD(xt, "withMin", "With min", "-1", null, null, 0, "String", null, null, null);
		assertAD(xt, "withC1", "With c1", null, null, null, 1, "String", null, null, null);
		assertAD(xt, "withC0", "With c0", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "withC.1", "With c 1", null, null, null, -1, "String", null, null, null);
		assertAD(xt, "withC.1ButArray", "With c 1 but array", null, null, null, -1, "String", null, null,
				null);
		assertAD(xt, "withC1ButCollection", "With c1 but collection", null, null, null, 1, "String",
				null, null, null);
		assertAD(xt, "withInt", "With int", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "withString", "With string", null, null, null, 0, "Integer", null, null, null);
		assertAD(xt, "a", "A", null, null, null, 0, "String", "description_xxx\"xxx'xxx", null, null);
		assertAD(xt, "valuesOnly", "Values only", null, null, null, 0, "String", null, new String[] {
				"a", "b"
		}, new String[] {
				"a", "b"
		});
		assertAD(xt, "labelsAndValues", "Labels and values", null, null, null, 0, "String", null, new String[] {
				"a", "b"
		},
				new String[] {
						"A", "B"
				});
	}

	private static XmlTester xmlTester12(Resource r) throws Exception {
		return xmlTester(r, MetatypeVersion.VERSION_1_2);
	}

	private static XmlTester xmlTester13(Resource r) throws Exception {
		return xmlTester(r, MetatypeVersion.VERSION_1_3);
	}

	private static XmlTester xmlTester(Resource r, MetatypeVersion version) throws Exception {
		XmlTester xt = new XmlTester(r.openInputStream(), "metatype", version.getNamespace());
		xt.assertNamespace(version.getNamespace());
		return xt;
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
		
		XmlTester xt = xmlTester13(r);
		
		assertAD(xt, "fromChild", "From child", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "fromSuperOne", "From super one", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "fromSuperTwo", "From super two", null, null, null, 0, "String", null, null, null);
	}

	@SuppressWarnings("null")
	static void assertAD(XmlTester xt,
	String id, String name, String min, String max, String deflt, int cardinality, String type, String description,
			String[] optionLabels, 
			String[] optionValues) throws XPathExpressionException {
		xt.assertExactAttribute(name, "//OCD/AD[@id='" + id + "']/@name");
		xt.assertExactAttribute(id, "//OCD/AD[@id='" + id + "']/@id");
		xt.assertExactAttribute(min == null ? "" : min,
				"//OCD/AD[@id='" + id + "']/@min");
		xt.assertExactAttribute(max == null ? "" : max,
				"//OCD/AD[@id='" + id + "']/@max");
		xt.assertExactAttribute(deflt == null ? "" : deflt,
				"//OCD/AD[@id='" + id + "']/@deflt");
		if (cardinality == 0) {
			xt.assertExactAttribute("",
					"//OCD/AD[@id='" + id + "']/@cardinality");
		} else {
			xt.assertExactAttribute(cardinality + "",
					"//OCD/AD[@id='" + id + "']/@cardinality");
		}
		xt.assertExactAttribute(type, "//OCD/AD[@id='" + id + "']/@type");
		xt.assertExactAttribute(description == null ? "" : description,
				"//OCD/AD[@id='" + id + "']/@description");
		assertEquals(optionLabels == null, optionValues == null);
		if (optionLabels != null) {
			assertEquals(optionLabels.length, optionValues.length);
			
			//option count is correct
			xt.assertNumber(Double.valueOf(optionLabels.length), "count(//OCD/AD[@id='" + id + "']/Option)");
			for (int i = 0; i < optionLabels.length; i++) {
				String expr = "//OCD/AD[@id='" + id + "']/Option[@label='" + optionLabels[i] + "']/@value";
				xt.assertExactAttribute(optionValues[i], expr);
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

		XmlTester xt = xmlTester13(r);
		// Primitives
		xt.assertExactAttribute("Boolean", "//OCD/AD[@id='rpBoolean']/@type");
		xt.assertExactAttribute("Byte", "//OCD/AD[@id='rpByte']/@type");
		xt.assertExactAttribute("Character", "//OCD/AD[@id='rpCharacter']/@type");
		xt.assertExactAttribute("Short", "//OCD/AD[@id='rpShort']/@type");
		xt.assertExactAttribute("Integer", "//OCD/AD[@id='rpInt']/@type");
		xt.assertExactAttribute("Long", "//OCD/AD[@id='rpLong']/@type");
		xt.assertExactAttribute("Float", "//OCD/AD[@id='rpFloat']/@type");
		xt.assertExactAttribute("Double", "//OCD/AD[@id='rpDouble']/@type");

		// Primitive Wrappers
		xt.assertExactAttribute("Boolean", "//OCD/AD[@id='rBoolean']/@type");
		xt.assertExactAttribute("Byte", "//OCD/AD[@id='rByte']/@type");
		xt.assertExactAttribute("Character", "//OCD/AD[@id='rCharacter']/@type");
		xt.assertExactAttribute("Short", "//OCD/AD[@id='rShort']/@type");
		xt.assertExactAttribute("Integer", "//OCD/AD[@id='rInt']/@type");
		xt.assertExactAttribute("Long", "//OCD/AD[@id='rLong']/@type");
		xt.assertExactAttribute("Float", "//OCD/AD[@id='rFloat']/@type");
		xt.assertExactAttribute("Double", "//OCD/AD[@id='rDouble']/@type");

		// Primitive Arrays
		xt.assertExactAttribute("Boolean", "//OCD/AD[@id='rpaBoolean']/@type");
		xt.assertExactAttribute("Byte", "//OCD/AD[@id='rpaByte']/@type");
		xt.assertExactAttribute("Character", "//OCD/AD[@id='rpaCharacter']/@type");
		xt.assertExactAttribute("Short", "//OCD/AD[@id='rpaShort']/@type");
		xt.assertExactAttribute("Integer", "//OCD/AD[@id='rpaInt']/@type");
		xt.assertExactAttribute("Long", "//OCD/AD[@id='rpaLong']/@type");
		xt.assertExactAttribute("Float", "//OCD/AD[@id='rpaFloat']/@type");
		xt.assertExactAttribute("Double", "//OCD/AD[@id='rpaDouble']/@type");

		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='rpaBoolean']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='rpaByte']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='rpaCharacter']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='rpaShort']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='rpaInt']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='rpaLong']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='rpaFloat']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='rpaDouble']/@cardinality");

		// Wrapper + Object arrays
		xt.assertExactAttribute("Boolean", "//OCD/AD[@id='raBoolean']/@type");
		xt.assertExactAttribute("Byte", "//OCD/AD[@id='raByte']/@type");
		xt.assertExactAttribute("Character", "//OCD/AD[@id='raCharacter']/@type");
		xt.assertExactAttribute("Short", "//OCD/AD[@id='raShort']/@type");
		xt.assertExactAttribute("Integer", "//OCD/AD[@id='raInt']/@type");
		xt.assertExactAttribute("Long", "//OCD/AD[@id='raLong']/@type");
		xt.assertExactAttribute("Float", "//OCD/AD[@id='raFloat']/@type");
		xt.assertExactAttribute("Double", "//OCD/AD[@id='raDouble']/@type");
		xt.assertExactAttribute("String", "//OCD/AD[@id='raString']/@type");
		xt.assertExactAttribute("String", "//OCD/AD[@id='raURI']/@type");

		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raBoolean']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raByte']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raCharacter']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raShort']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raInt']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raLong']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raFloat']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raDouble']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raString']/@cardinality");
		xt.assertExactAttribute("2147483647", "//OCD/AD[@id='raURI']/@cardinality");

		// Wrapper + Object collections
		xt.assertExactAttribute("Boolean", "//OCD/AD[@id='rBooleans']/@type");
		xt.assertExactAttribute("Byte", "//OCD/AD[@id='rBytes']/@type");
		xt.assertExactAttribute("Character", "//OCD/AD[@id='rCharacter']/@type");
		xt.assertExactAttribute("Short", "//OCD/AD[@id='rShorts']/@type");
		xt.assertExactAttribute("Integer", "//OCD/AD[@id='rInts']/@type");
		xt.assertExactAttribute("Long", "//OCD/AD[@id='rLongs']/@type");
		xt.assertExactAttribute("Float", "//OCD/AD[@id='rFloats']/@type");
		xt.assertExactAttribute("Double", "//OCD/AD[@id='rDoubles']/@type");
		xt.assertExactAttribute("String", "//OCD/AD[@id='rStrings']/@type");
		xt.assertExactAttribute("String", "//OCD/AD[@id='rURIs']/@type");

		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rBooleans']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rBytes']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rCharacters']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rShorts']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rInts']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rLongs']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rFloats']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rDoubles']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rStrings']/@cardinality");
		xt.assertExactAttribute("-2147483648", "//OCD/AD[@id='rURIs']/@cardinality");
		
	}
	
	@ObjectClassDefinition
	public static interface TestReturn12 {
		
		char chara();
		
		Character charWrapper();
		
	}
	
	public static void testReturn12() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", TestReturn12.class.getName());
		b.setProperty("-metatypeannotations-flags", "preferV12");
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestReturn12.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester12(r);

		xt.assertExactAttribute("Char", "//OCD/AD[@id='chara']/@type");
		xt.assertExactAttribute("Char", "//OCD/AD[@id='charWrapper']/@type");
		
	}
	
	@ObjectClassDefinition
	public static interface NoADs {}
	
	//at least one AD is required for v 1.2, so NoADs will force v 1.3 even if you request 1.2.
	public static void testNoAds() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", NoADs.class.getName());
        b.setProperty("-metatypeannotations-flags", "preferV12");
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$NoADs.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester13(r);
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
		testSimple(MetatypeVersion.VERSION_1_3);
		testSimple(MetatypeVersion.VERSION_1_2);
	}

	private static void testSimple(MetatypeVersion version) throws IOException, Exception, XPathExpressionException {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty("-metatypeannotations", TestSimple.class.getName());
		if (version == MetatypeVersion.VERSION_1_2) {
			b.setProperty("-metatypeannotations-flags", "preferV12");
		}
		b.build();
		Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestSimple.xml");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester(r, version);

		xt.assertExactAttribute("TestSimple", "//OCD/@name");
		xt.assertExactAttribute("simple", "//OCD/@description");
		xt.assertExactAttribute("test.metatype.SpecMetatypeTest$TestSimple", "//OCD/@id");
		xt.assertExactAttribute("simplePid", "//Designate/@pid");
		xt.assertExactAttribute("test.metatype.SpecMetatypeTest$TestSimple", "//Object/@ocdref");
		xt.assertExactAttribute("simple", "//OCD/AD[@id='simple']/@id");
		xt.assertExactAttribute("Simple", "//OCD/AD[@id='simple']/@name");
		xt.assertExactAttribute("String", "//OCD/AD[@id='simple']/@type");
		xt.assertExactAttribute("false", "//OCD/AD[@id='enabled']/@required");
		xt.assertExactAttribute("true", "//OCD/AD[@id='enabled']/@default");
		xt.assertExactAttribute(Integer.MAX_VALUE + "", "//OCD/AD[@id='notSoSimple']/@cardinality");
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

		XmlTester xt = xmlTester13(r);
		xt.assertExactAttribute("1", "//OCD/AD[@id='integer']/@default");
		xt.assertExactAttribute("2,3", "//OCD/AD[@id='integers']/@default");
		
		xt.assertExactAttribute("true", "//OCD/AD[@id='bool']/@default");
		xt.assertExactAttribute("true,false", "//OCD/AD[@id='bools']/@default");
		
		xt.assertExactAttribute(String.class.getName(), "//OCD/AD[@id='clazz']/@default");
		xt.assertExactAttribute(Integer.class.getName() + "," + Double.class.getName(), "//OCD/AD[@id='clazzs']/@default");
		
		xt.assertExactAttribute("A", "//OCD/AD[@id='l']/@default");
		xt.assertExactAttribute("B,C", "//OCD/AD[@id='ls']/@default");
		
		xt.assertExactAttribute("foo", "//OCD/AD[@id='string']/@default");
		xt.assertExactAttribute("bar,baz", "//OCD/AD[@id='strings']/@default");
		
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

			XmlTester xt = xmlTester13(r);
			xt.assertExactAttribute("String", "//OCD/AD[@id='inner']/@type");
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

			XmlTester xt = xmlTester13(r);
			xt.assertExactAttribute("Test metatype spec metatype test designate OCD", "//OCD/@name");
			xt.assertExactAttribute("test.metatype.SpecMetatypeTest$DesignateOCD", "//OCD/@id");
		}
		{
			Resource r = b.getJar().getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$DesignateComponent.xml");
			assertEquals(0, b.getErrors().size());
			assertEquals(0, b.getWarnings().size());
			System.err.println(b.getJar().getResources().keySet());
			assertNotNull(r);
			IO.copy(r.openInputStream(), System.err);

			//TODO should all designates be at v 1.2?
			XmlTester xt = xmlTester12(r);
			xt.assertExactAttribute("simplePid", "//Designate/@factoryPid");
			xt.assertExactAttribute("test.metatype.SpecMetatypeTest$DesignateOCD", "//Object/@ocdref");
		}
	}
}
