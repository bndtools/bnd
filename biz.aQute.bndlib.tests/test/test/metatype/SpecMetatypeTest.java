package test.metatype;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.annotation.xml.XMLAttribute;
import aQute.bnd.metatype.MetatypeVersion;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Resource;
import aQute.bnd.test.XmlTester;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings({
	"resource", "restriction"
})
public class SpecMetatypeTest extends TestCase {

	public enum Foo {
		A,
		B
	}

	/**
	 * Test method naming options with '.' and reserved names
	 */

	@ObjectClassDefinition
	public interface Naming {
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

		@AttributeDefinition(name = "§NULL§") // Meta.NULL
		String nullid();
	}

	private void assertAD(XmlTester xt, String id, String name) throws XPathExpressionException {
		assertAD(xt, id, name, null, null, null, 0, "String", null, null, null);
	}

	public void testNaming() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, Naming.class.getName());
		b.build();
		System.out.println(b.getErrors());
		assertEquals(0, b.getErrors()
			.size());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getWarnings()
			.size());

		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$Naming.xml");
		IO.copy(r.openInputStream(), System.err);
		XmlTester xt = xmlTester12(r);

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

	/**
	 * Test method naming options with '.' and reserved names
	 */

	@ObjectClassDefinition
	public static @interface Naming14 {
		String PREFIX_ = "test.";

		String a$_$b() default "foo"; // test.a-b

		@AttributeDefinition(name = "a-b")
		String xa$_$b() default "foo"; // test.xa-b

		String value() default "foo"; // test.naming14
	}

	private void assertAD(XmlTester xt, String id, String name, String deflt) throws XPathExpressionException {
		assertAD(xt, id, name, null, null, deflt, 0, "String", null, null, null);
	}

	public void testNaming14() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, Naming14.class.getName());
		b.build();
		System.out.println(b.getErrors());
		assertEquals(0, b.getErrors()
			.size());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getWarnings()
			.size());

		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$Naming14.xml");
		IO.copy(r.openInputStream(), System.err);
		XmlTester xt = xmlTester14(r);

		assertAD(xt, "test.a-b", "A b", "foo");
		assertAD(xt, "test.xa-b", "a-b", "foo");
		assertAD(xt, "test.naming14", "Value", "foo");
	}

	@ObjectClassDefinition
	public interface ADCollision {

		String a$$(); // a$

		String a$$$(); // a$
	}

	public void testADCollision() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, ADCollision.class.getName());
		b.build();
		System.out.println(b.getErrors());
		assertEquals(1, b.getErrors()
			.size());
	}

	@ObjectClassDefinition(id = "duplicate")
	public interface DupOCDId1 {

	}

	@ObjectClassDefinition(id = "duplicate")
	public interface DupOCDId2 {

	}

	public void testOCDCollision() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		String name = DupOCDId2.class.getName();

		b.setProperty(Constants.METATYPE_ANNOTATIONS, name.substring(0, name.length() - "1".length()) + "*");
		b.build();
		assertEquals(1, b.getErrors()
			.size());
	}

	@ObjectClassDefinition(pid = {
		"1"
	})
	public interface DupPid1 {

	}

	@ObjectClassDefinition(pid = {
		"1"
	})
	public interface DupPid2 {

	}

	@ObjectClassDefinition(factoryPid = {
		"2"
	})
	public interface DupPid3 {

	}

	@ObjectClassDefinition(factoryPid = {
		"2"
	})
	public interface DupPid4 {

	}

	@ObjectClassDefinition(pid = {
		"3"
	}, factoryPid = {
		"3"
	})
	public interface DupPid5 {

	}

	@ObjectClassDefinition(pid = {
		"4"
	})
	public interface DupPid6 {

	}

	@ObjectClassDefinition(factoryPid = {
		"4"
	})
	public interface DupPid7 {

	}

	public void testPidCollision() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		String name = DupPid2.class.getName();

		b.setProperty(Constants.METATYPE_ANNOTATIONS, name.substring(0, name.length() - "1".length()) + "*");
		b.build();
		System.err.println(b.getErrors());
		assertEquals(4, b.getErrors()
			.size());
	}

	/**
	 * Test the special conversions.
	 */

	public static class MyList<T> extends ArrayList<T> {
		private static final long serialVersionUID = 1L;

		public MyList() {}

	}

	interface L<T> extends List<T> {}

	public static class StringList<T> implements L<T> {

		@Override
		public void add(int arg0, T arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean addAll(int arg0, Collection<? extends T> arg1) {
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
		public boolean addAll(Collection<? extends T> arg0) {
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
		public boolean containsAll(Collection<?> arg0) {
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
		public boolean removeAll(Collection<?> arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean retainAll(Collection<?> arg0) {
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
	interface CollectionsTest {
		Collection<String> collection();

		List<String> list();

		Set<String> set();

		// Queue<String> queue();

		// Deque<String> deque();
		// Stack<String> stack();

		ArrayList<String> arrayList();

		LinkedList<String> linkedList();

		LinkedHashSet<String> linkedHashSet();

		MyList<String> myList();

		StringList<String> stringList();
	}

	public void testCollections() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, CollectionsTest.class.getName());
		b.build();
		System.out.println(b.getErrors());
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());

		System.err.println(b.getJar()
			.getResources());
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$CollectionsTest.xml");
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester12(r);

		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='collection']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='list']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='set']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='arrayList']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='linkedList']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='linkedHashSet']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='myList']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='stringList']/@cardinality");
	}

	/**
	 * Test enum handling
	 */

	@ObjectClassDefinition
	public interface Enums {
		enum X {
			requireConfiguration,
			optionalConfiguration,
			ignoreConfiguration
		}

		X r();

		X i();

		X o();
	}

	private static final String[]	optionLabels	= {
		"requireConfiguration", "optionalConfiguration", "ignoreConfiguration"
	};
	private static final String[]	optionValues	= optionLabels;

	private void assertAD(XmlTester xt, String id, String name, String[] optionLabels, String[] optionValues)
		throws XPathExpressionException {
		assertAD(xt, id, name, null, null, null, 0, "String", null, optionLabels, optionValues);
	}

	public void testEnum() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, Enums.class.getName() + "*");
		b.build();
		System.err.println(b.getErrors());
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());

		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$Enums.xml");
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester12(r);

		assertAD(xt, "r", "R", optionLabels, optionValues);
		assertAD(xt, "i", "I", optionLabels, optionValues);
		assertAD(xt, "o", "O", optionLabels, optionValues);
	}

	/**
	 * Test the OCD settings
	 */
	@ObjectClassDefinition(pid = {
		"ocdEmptyPid"
	})
	public interface OCDEmpty {}

	@ObjectClassDefinition(description = "description", pid = {
		"ocdDescriptionPid"
	})
	public interface OCDDescription {}

	@ObjectClassDefinition(pid = {
		"ocdDesignatePidOnlyPid"
	})
	public interface OCDDesignatePidOnly {}

	@ObjectClassDefinition(factoryPid = {
		"ocdDesignatePidFactoryFactoryPid"
	})
	public interface OCDDesignatePidFactory {}

	@ObjectClassDefinition(id = "id", pid = {
		"ocdIdPid"
	})
	public interface OCDId {}

	@ObjectClassDefinition(id = "id2", pid = {
		"ocdId2Pid"
	})
	public interface OCDIdWithPid {}

	@ObjectClassDefinition(localization = "localization", pid = {
		"ocdLocalizationPid"
	})
	public interface OCDLocalization {}

	@ObjectClassDefinition(name = "name", pid = {
		"ocdNamePid"
	})
	public interface OCDName {}

	public void testOCD() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		String name = OCDEmpty.class.getName();

		b.setProperty(Constants.METATYPE_ANNOTATIONS, name.substring(0, name.length() - "Empty".length()) + "*");
		b.build();
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());
		System.err.println(b.getJar()
			.getResources()
			.keySet());

		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDEmpty", "test.metatype.SpecMetatypeTest$OCDEmpty",
			"Test metatype spec metatype test OCDEmpty", null, "ocdEmptyPid", false, null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDName", "test.metatype.SpecMetatypeTest$OCDName", "name", null,
			"ocdNamePid", false, null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDDescription", "test.metatype.SpecMetatypeTest$OCDDescription",
			"Test metatype spec metatype test OCDDescription", "description", "ocdDescriptionPid", false, null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDDesignatePidOnly",
			"test.metatype.SpecMetatypeTest$OCDDesignatePidOnly",
			"Test metatype spec metatype test OCDDesignate pid only", null, "ocdDesignatePidOnlyPid", false, null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDDesignatePidFactory",
			"test.metatype.SpecMetatypeTest$OCDDesignatePidFactory",
			"Test metatype spec metatype test OCDDesignate pid factory", null, "ocdDesignatePidFactoryFactoryPid", true,
			null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDId", "id", "Id", // 5.2
																			// name
																			// is
																			// derived
																			// from
																			// id
			null, "ocdIdPid", false, null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDIdWithPid", "id2", "Id2", null, "ocdId2Pid", false, null);
		assertOCD(b, "test.metatype.SpecMetatypeTest$OCDLocalization", "test.metatype.SpecMetatypeTest$OCDLocalization",
			"Test metatype spec metatype test OCDLocalization", null, "ocdLocalizationPid", false, "localization");
	}

	private void assertOCD(Builder b, String cname, String id, String name, String description, String designate,
		boolean factory, String localization) throws Exception {
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/" + id + ".xml");
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);
		XmlTester xt = xmlTester13(r);
		xt.assertExactAttribute(id, "metatype:MetaData/OCD/@id");
		xt.assertExactAttribute(name, "metatype:MetaData/OCD/@name");
		String expected = localization == null ? "OSGI-INF/I10n/" + cname : localization;
		xt.assertExactAttribute(localization == null ? "OSGI-INF/l10n/" + cname : localization,
			"metatype:MetaData/@localization");
		xt.assertExactAttribute(description == null ? "" : description, "metatype:MetaData/OCD/@description");

		if (designate != null) {
			if (factory)
				xt.assertExactAttribute(designate, "metatype:MetaData/Designate/@factoryPid");
			else
				xt.assertExactAttribute(designate, "metatype:MetaData/Designate/@pid");
		}

		xt.assertExactAttribute(id, "metatype:MetaData/Designate/Object/@ocdref");
	}

	/**
	 * Test the AD settings.
	 */

	@ObjectClassDefinition(description = "advariations")
	public interface TestAD {
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

		@AttributeDefinition(defaultValue = {
			"deflt"
		})
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

		@AttributeDefinition(options = {
			@Option(label = "a", value = "a"), @Option(label = "b", value = "b")
		})
		String valuesOnly();

		@AttributeDefinition(options = {
			@Option(label = "a", value = "A"), @Option(label = "b", value = "B")
		})
		String labelsAndValues();

		@AttributeDefinition(required = true)
		String required();

		@AttributeDefinition(required = false)
		String notRequired();
	}

	public void testAD() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, TestAD.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestAD.xml");
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());
		System.err.println(b.getJar()
			.getResources()
			.keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester12(r);

		assertAD(xt, "noSettings", "No settings", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "withId", "id", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "withName", "name", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "withMax", "With max", null, "1", null, 0, "String", null, null, null);
		assertAD(xt, "withMin", "With min", "-1", null, null, 0, "String", null, null, null);
		assertAD(xt, "withC1", "With c1", null, null, null, 1, "String", null, null, null);
		assertAD(xt, "withC0", "With c0", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "withC.1", "With c 1", null, null, null, -1, "String", null, null, null);
		assertAD(xt, "withC.1ButArray", "With c 1 but array", null, null, null, -1, "String", null, null, null);
		assertAD(xt, "withC1ButCollection", "With c1 but collection", null, null, null, 1, "String", null, null, null);
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
		}, new String[] {
			"A", "B"
		});
	}

	private XmlTester xmlTester12(Resource r) throws Exception {
		return xmlTester(r, MetatypeVersion.VERSION_1_2);
	}

	private XmlTester xmlTester13(Resource r) throws Exception {
		return xmlTester(r, MetatypeVersion.VERSION_1_3);
	}

	private XmlTester xmlTester14(Resource r) throws Exception {
		return xmlTester(r, MetatypeVersion.VERSION_1_4);
	}

	private XmlTester xmlTester(Resource r, MetatypeVersion version) throws Exception {
		XmlTester xt = new XmlTester(r.openInputStream(), "metatype", version.getNamespace());
		xt.assertNamespace(version.getNamespace());
		return xt;
	}

	/**
	 * Test the AD inheritance.
	 */

	@ObjectClassDefinition(description = "adinheritance-super-one")
	public interface TestADWithInheritanceSuperOne {
		@AttributeDefinition
		String fromSuperOne();
	}

	@ObjectClassDefinition(description = "adinheritance-super")
	public interface TestADWithInheritanceSuperTwo {
		@AttributeDefinition
		String fromSuperTwo();
	}

	@ObjectClassDefinition(description = "adinheritance-child")
	public interface TestADWithInheritanceChild extends TestADWithInheritanceSuperOne, TestADWithInheritanceSuperTwo {
		@AttributeDefinition
		String fromChild();
	}

	public void testADWithInheritance() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, TestADWithInheritanceChild.class.getName()
			.substring(0, TestADWithInheritanceChild.class.getName()
				.length() - "Child".length())
			+ "*");
		// b.setProperty("-metatypeannotations-inherit", "true");
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestADWithInheritanceChild.xml");
		System.err.println(b.getErrors());
		assertEquals(0, b.getErrors()
			.size());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getWarnings()
			.size());
		System.err.println(b.getJar()
			.getResources()
			.keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester12(r);

		assertAD(xt, "fromChild", "From child", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "fromSuperOne", "From super one", null, null, null, 0, "String", null, null, null);
		assertAD(xt, "fromSuperTwo", "From super two", null, null, null, 0, "String", null, null, null);
	}

	@SuppressWarnings("null")
	private void assertAD(XmlTester xt, String id, String name, String min, String max, String deflt, int cardinality,
		String type, String description, String[] optionLabels, String[] optionValues) throws XPathExpressionException {
		xt.assertExactAttribute(name, "metatype:MetaData/OCD/AD[@id='" + id + "']/@name");
		xt.assertExactAttribute(id, "metatype:MetaData/OCD/AD[@id='" + id + "']/@id");
		xt.assertExactAttribute(min == null ? "" : min, "metatype:MetaData/OCD/AD[@id='" + id + "']/@min");
		xt.assertExactAttribute(max == null ? "" : max, "metatype:MetaData/OCD/AD[@id='" + id + "']/@max");
		xt.assertExactAttribute(deflt == null ? "" : deflt, "metatype:MetaData/OCD/AD[@id='" + id + "']/@default");
		if (cardinality == 0) {
			xt.assertExactAttribute("", "metatype:MetaData/OCD/AD[@id='" + id + "']/@cardinality");
		} else {
			xt.assertExactAttribute(cardinality + "", "metatype:MetaData/OCD/AD[@id='" + id + "']/@cardinality");
		}
		xt.assertExactAttribute(type, "metatype:MetaData/OCD/AD[@id='" + id + "']/@type");
		xt.assertExactAttribute(description == null ? "" : description,
			"metatype:MetaData/OCD/AD[@id='" + id + "']/@description");
		assertEquals(optionLabels == null, optionValues == null);
		if (optionLabels != null) {
			assertEquals(optionLabels.length, optionValues.length);

			// option count is correct
			xt.assertNumber(Double.valueOf(optionLabels.length),
				"count(metatype:MetaData/OCD/AD[@id='" + id + "']/Option)");
			for (int i = 0; i < optionLabels.length; i++) {
				String expr = "metatype:MetaData/OCD/AD[@id='" + id + "']/Option[@label='" + optionLabels[i]
					+ "']/@value";
				xt.assertExactAttribute(optionValues[i], expr);
			}

		}
	}

	/**
	 * Test all the return types.
	 */
	@ObjectClassDefinition(description = "simple", name = "TestSimple")
	public interface TestReturnTypes {
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
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, TestReturnTypes.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestReturnTypes.xml");
		System.err.println(b.getErrors());
		assertEquals(0, b.getErrors()
			.size());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getWarnings()
			.size());
		System.err.println(b.getJar()
			.getResources()
			.keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester12(r);
		// Primitives
		xt.assertExactAttribute("Boolean", "metatype:MetaData/OCD/AD[@id='rpBoolean']/@type");
		xt.assertExactAttribute("Byte", "metatype:MetaData/OCD/AD[@id='rpByte']/@type");
		xt.assertExactAttribute("Char", "metatype:MetaData/OCD/AD[@id='rpCharacter']/@type");
		xt.assertExactAttribute("Short", "metatype:MetaData/OCD/AD[@id='rpShort']/@type");
		xt.assertExactAttribute("Integer", "metatype:MetaData/OCD/AD[@id='rpInt']/@type");
		xt.assertExactAttribute("Long", "metatype:MetaData/OCD/AD[@id='rpLong']/@type");
		xt.assertExactAttribute("Float", "metatype:MetaData/OCD/AD[@id='rpFloat']/@type");
		xt.assertExactAttribute("Double", "metatype:MetaData/OCD/AD[@id='rpDouble']/@type");

		// Primitive Wrappers
		xt.assertExactAttribute("Boolean", "metatype:MetaData/OCD/AD[@id='rBoolean']/@type");
		xt.assertExactAttribute("Byte", "metatype:MetaData/OCD/AD[@id='rByte']/@type");
		xt.assertExactAttribute("Char", "metatype:MetaData/OCD/AD[@id='rCharacter']/@type");
		xt.assertExactAttribute("Short", "metatype:MetaData/OCD/AD[@id='rShort']/@type");
		xt.assertExactAttribute("Integer", "metatype:MetaData/OCD/AD[@id='rInt']/@type");
		xt.assertExactAttribute("Long", "metatype:MetaData/OCD/AD[@id='rLong']/@type");
		xt.assertExactAttribute("Float", "metatype:MetaData/OCD/AD[@id='rFloat']/@type");
		xt.assertExactAttribute("Double", "metatype:MetaData/OCD/AD[@id='rDouble']/@type");

		// Primitive Arrays
		xt.assertExactAttribute("Boolean", "metatype:MetaData/OCD/AD[@id='rpaBoolean']/@type");
		xt.assertExactAttribute("Byte", "metatype:MetaData/OCD/AD[@id='rpaByte']/@type");
		xt.assertExactAttribute("Char", "metatype:MetaData/OCD/AD[@id='rpaCharacter']/@type");
		xt.assertExactAttribute("Short", "metatype:MetaData/OCD/AD[@id='rpaShort']/@type");
		xt.assertExactAttribute("Integer", "metatype:MetaData/OCD/AD[@id='rpaInt']/@type");
		xt.assertExactAttribute("Long", "metatype:MetaData/OCD/AD[@id='rpaLong']/@type");
		xt.assertExactAttribute("Float", "metatype:MetaData/OCD/AD[@id='rpaFloat']/@type");
		xt.assertExactAttribute("Double", "metatype:MetaData/OCD/AD[@id='rpaDouble']/@type");

		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='rpaBoolean']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='rpaByte']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='rpaCharacter']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='rpaShort']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='rpaInt']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='rpaLong']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='rpaFloat']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='rpaDouble']/@cardinality");

		// Wrapper + Object arrays
		xt.assertExactAttribute("Boolean", "metatype:MetaData/OCD/AD[@id='raBoolean']/@type");
		xt.assertExactAttribute("Byte", "metatype:MetaData/OCD/AD[@id='raByte']/@type");
		xt.assertExactAttribute("Char", "metatype:MetaData/OCD/AD[@id='raCharacter']/@type");
		xt.assertExactAttribute("Short", "metatype:MetaData/OCD/AD[@id='raShort']/@type");
		xt.assertExactAttribute("Integer", "metatype:MetaData/OCD/AD[@id='raInt']/@type");
		xt.assertExactAttribute("Long", "metatype:MetaData/OCD/AD[@id='raLong']/@type");
		xt.assertExactAttribute("Float", "metatype:MetaData/OCD/AD[@id='raFloat']/@type");
		xt.assertExactAttribute("Double", "metatype:MetaData/OCD/AD[@id='raDouble']/@type");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='raString']/@type");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='raURI']/@type");

		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raBoolean']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raByte']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raCharacter']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raShort']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raInt']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raLong']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raFloat']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raDouble']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raString']/@cardinality");
		xt.assertExactAttribute("2147483647", "metatype:MetaData/OCD/AD[@id='raURI']/@cardinality");

		// Wrapper + Object collections
		xt.assertExactAttribute("Boolean", "metatype:MetaData/OCD/AD[@id='rBooleans']/@type");
		xt.assertExactAttribute("Byte", "metatype:MetaData/OCD/AD[@id='rBytes']/@type");
		xt.assertExactAttribute("Char", "metatype:MetaData/OCD/AD[@id='rCharacter']/@type");
		xt.assertExactAttribute("Short", "metatype:MetaData/OCD/AD[@id='rShorts']/@type");
		xt.assertExactAttribute("Integer", "metatype:MetaData/OCD/AD[@id='rInts']/@type");
		xt.assertExactAttribute("Long", "metatype:MetaData/OCD/AD[@id='rLongs']/@type");
		xt.assertExactAttribute("Float", "metatype:MetaData/OCD/AD[@id='rFloats']/@type");
		xt.assertExactAttribute("Double", "metatype:MetaData/OCD/AD[@id='rDoubles']/@type");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='rStrings']/@type");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='rURIs']/@type");

		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rBooleans']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rBytes']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rCharacters']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rShorts']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rInts']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rLongs']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rFloats']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rDoubles']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rStrings']/@cardinality");
		xt.assertExactAttribute("-2147483648", "metatype:MetaData/OCD/AD[@id='rURIs']/@cardinality");

	}

	/**
	 * Test simple
	 *
	 * @author aqute
	 */
	@ObjectClassDefinition(description = "simple", name = "TestSimple", pid = {
		"simplePid", "$"
	})
	public interface TestSimple {
		@AttributeDefinition
		String simple();

		String[] notSoSimple();

		Collection<String> stringCollection();

		@AttributeDefinition(defaultValue = {
			"true"
		}, required = false)
		boolean enabled();
	}

	public void testSimple() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, TestSimple.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestSimple.xml");
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester12(r);

		testSimple(b, xt);
	}

	public void testSimple13() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, TestSimple.class.getName());
		b.setProperty(Constants.METATYPE_ANNOTATIONS_OPTIONS, "version;minimum=1.3.0");
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestSimple.xml");
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester13(r);

		testSimple(b, xt);
	}

	private void testSimple(Builder b, XmlTester xt) throws XPathExpressionException {
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());
		System.err.println(b.getJar()
			.getResources()
			.keySet());
		for (String res : b.getJar()
			.getResources()
			.keySet()) {
			if (res.endsWith(".xml"))
				System.err.println(res);
		}

		xt.assertExactAttribute("TestSimple", "metatype:MetaData/OCD/@name");
		xt.assertExactAttribute("simple", "metatype:MetaData/OCD/@description");
		xt.assertExactAttribute("test.metatype.SpecMetatypeTest$TestSimple", "metatype:MetaData/OCD/@id");
		xt.assertExactAttribute("simplePid", "metatype:MetaData/Designate[1]/@pid");
		xt.assertExactAttribute(TestSimple.class.getName(), "metatype:MetaData/Designate[2]/@pid");
		xt.assertExactAttribute(TestSimple.class.getName(), "metatype:MetaData/Designate/Object/@ocdref");
		xt.assertExactAttribute("simple", "metatype:MetaData/OCD/AD[@id='simple']/@id");
		xt.assertExactAttribute("Simple", "metatype:MetaData/OCD/AD[@id='simple']/@name");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='simple']/@type");
		xt.assertExactAttribute("false", "metatype:MetaData/OCD/AD[@id='enabled']/@required");
		xt.assertExactAttribute("true", "metatype:MetaData/OCD/AD[@id='enabled']/@default");
		xt.assertExactAttribute(Integer.MAX_VALUE + "", "metatype:MetaData/OCD/AD[@id='notSoSimple']/@cardinality");
	}

	@ObjectClassDefinition
	public static @interface AnnotationDefaults {
		int integer() default 1;

		int[] integers() default {
			2, 3
		};

		boolean bool() default true;

		boolean[] bools() default {
			true, false
		};

		Class<?> clazz() default String.class;

		Class<?>[] clazzs() default {
			Integer.class, Double.class
		};

		enum L {
			A,
			B,
			C
		}

		L l() default L.A;

		L[] ls() default {
			L.B, L.C
		};

		String string() default "foo";

		String[] strings() default {
			"bar", "baz"
		};

		char character() default 'a';

		char[] characters() default {
			'b', 'c'
		};
	}

	public void testAnnotationDefaults() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, AnnotationDefaults.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$AnnotationDefaults.xml");
		System.err.println(b.getErrors());
		assertEquals(0, b.getErrors()
			.size());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getWarnings()
			.size());
		System.err.println(b.getJar()
			.getResources()
			.keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester12(r);
		xt.assertExactAttribute("1", "metatype:MetaData/OCD/AD[@id='integer']/@default");
		xt.assertExactAttribute("2,3", "metatype:MetaData/OCD/AD[@id='integers']/@default");
		xt.assertExactAttribute("Integer", "metatype:MetaData/OCD/AD[@id='integer']/@type");
		xt.assertExactAttribute("Integer", "metatype:MetaData/OCD/AD[@id='integers']/@type");

		xt.assertExactAttribute("true", "metatype:MetaData/OCD/AD[@id='bool']/@default");
		xt.assertExactAttribute("true,false", "metatype:MetaData/OCD/AD[@id='bools']/@default");
		xt.assertExactAttribute("Boolean", "metatype:MetaData/OCD/AD[@id='bool']/@type");
		xt.assertExactAttribute("Boolean", "metatype:MetaData/OCD/AD[@id='bools']/@type");

		xt.assertExactAttribute(String.class.getName(), "metatype:MetaData/OCD/AD[@id='clazz']/@default");
		xt.assertExactAttribute(Integer.class.getName() + "," + Double.class.getName(),
			"metatype:MetaData/OCD/AD[@id='clazzs']/@default");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='clazz']/@type");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='clazzs']/@type");

		xt.assertExactAttribute("A", "metatype:MetaData/OCD/AD[@id='l']/@default");
		xt.assertExactAttribute("B,C", "metatype:MetaData/OCD/AD[@id='ls']/@default");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='l']/@type");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='ls']/@type");

		xt.assertExactAttribute("foo", "metatype:MetaData/OCD/AD[@id='string']/@default");
		xt.assertExactAttribute("bar,baz", "metatype:MetaData/OCD/AD[@id='strings']/@default");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='string']/@type");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='strings']/@type");

		xt.assertExactAttribute("a", "metatype:MetaData/OCD/AD[@id='character']/@default");
		xt.assertExactAttribute("b,c", "metatype:MetaData/OCD/AD[@id='characters']/@default");
		xt.assertExactAttribute("Char", "metatype:MetaData/OCD/AD[@id='character']/@type");
		xt.assertExactAttribute("Char", "metatype:MetaData/OCD/AD[@id='characters']/@type");

	}

	@ObjectClassDefinition
	public static abstract class Abstract {

	}

	public void testAbstract() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, Abstract.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$Abstract.xml");
		assertEquals(1, b.getErrors()
			.size());
		assertNull(r);
	}

	@ObjectClassDefinition
	public static @interface NestedInner {

	}

	@ObjectClassDefinition
	public static @interface NestedOuter {
		NestedInner inner();
	}

	public void testNested() throws Exception {
		{
			Builder b = new Builder();
			b.addClasspath(new File("bin_test"));
			b.setProperty("Export-Package", "test.metatype");
			b.setProperty(Constants.METATYPE_ANNOTATIONS,
				NestedInner.class.getName() + "," + NestedOuter.class.getName());
			b.build();
			Resource r = b.getJar()
				.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$NestedOuter.xml");
			assertEquals(1, b.getErrors()
				.size());
		}
		{
			Builder b = new Builder();
			b.addClasspath(new File("bin_test"));
			b.setProperty("Export-Package", "test.metatype");
			b.setProperty(Constants.METATYPE_ANNOTATIONS,
				NestedInner.class.getName() + "," + NestedOuter.class.getName());
			b.setProperty(Constants.METATYPE_ANNOTATIONS_OPTIONS, "nested");
			b.build();
			Resource r = b.getJar()
				.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$NestedOuter.xml");
			assertEquals(0, b.getErrors()
				.size());
			assertEquals(0, b.getWarnings()
				.size());
			System.err.println(b.getJar()
				.getResources()
				.keySet());
			assertNotNull(r);
			IO.copy(r.openInputStream(), System.err);

			XmlTester xt = xmlTester12(r);
			xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='inner']/@type");
		}
	}

	@ObjectClassDefinition
	public static @interface DesignateOCD {

	}

	@Component(configurationPid = "simplePid")
	@Designate(ocd = DesignateOCD.class, factory = true)
	public static class DesignateComponent {}

	@Component(name = "simplePidName")
	@Designate(ocd = DesignateOCD.class, factory = true)
	public static class DesignateComponent2 {}

	@Component
	@Designate(ocd = DesignateOCD.class, factory = true)
	public static class DesignateComponent3 {}

	@Component(configurationPid = "$")
	@Designate(ocd = DesignateOCD.class, factory = true)
	public static class DesignateComponent4 {}

	public void testDesignate() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, "test.metatype.SpecMetatypeTest$Designate*");
		b.setProperty(Constants.DSANNOTATIONS, "test.metatype.SpecMetatypeTest$Designate*");
		b.build();
		{
			Resource r = b.getJar()
				.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$DesignateOCD.xml");
			assertEquals(b.getErrors()
				.toString(), 0,
				b.getErrors()
					.size());
			assertEquals(b.getWarnings()
				.toString(), 0,
				b.getWarnings()
					.size());
			System.err.println(b.getJar()
				.getResources()
				.keySet());
			assertNotNull(r);
			IO.copy(r.openInputStream(), System.err);

			XmlTester xt = xmlTester13(r);
			xt.assertExactAttribute("Test metatype spec metatype test designate OCD", "metatype:MetaData/OCD/@name");
			xt.assertExactAttribute("test.metatype.SpecMetatypeTest$DesignateOCD", "metatype:MetaData/OCD/@id");

			xt.assertExactAttribute("simplePid", "metatype:MetaData/Designate[1]/@factoryPid");
			xt.assertExactAttribute("test.metatype.SpecMetatypeTest$DesignateOCD",
				"metatype:MetaData/Designate[1]/Object/@ocdref");

			xt.assertExactAttribute("simplePidName", "metatype:MetaData/Designate[2]/@factoryPid");
			xt.assertExactAttribute("test.metatype.SpecMetatypeTest$DesignateOCD",
				"metatype:MetaData/Designate[2]/Object/@ocdref");

			xt.assertExactAttribute(DesignateComponent3.class.getName(), "metatype:MetaData/Designate[3]/@factoryPid");
			xt.assertExactAttribute("test.metatype.SpecMetatypeTest$DesignateOCD",
				"metatype:MetaData/Designate[3]/Object/@ocdref");

			xt.assertExactAttribute(DesignateComponent4.class.getName(), "metatype:MetaData/Designate[4]/@factoryPid");
			xt.assertExactAttribute("test.metatype.SpecMetatypeTest$DesignateOCD",
				"metatype:MetaData/Designate[4]/Object/@ocdref");
		}
	}

	@XMLAttribute(namespace = "org.foo.extensions.v1", prefix = "foo", embedIn = "*")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface OCDTestExtensions {
		boolean booleanAttr() default true;

		String stringAttr();

		Foo fooAttr();

		String[] stringArrayAttr();

		int[] intArrayAttr();
	}

	@XMLAttribute(namespace = "org.foo.extensions.v1", prefix = "foo", embedIn = "*")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	@interface ADTestExtensions {
		boolean booleanAttr2() default true;

		String stringAttr2();

		Foo fooAttr2();

		Class<?> classAttr2() default Object.class;

	}

	@ObjectClassDefinition
	@OCDTestExtensions(stringAttr = "ocd", fooAttr = Foo.A, stringArrayAttr = {
		"foo", "bar"
	}, intArrayAttr = {
		1, 2, 3
	})
	public interface TestExtensions {
		@AttributeDefinition
		String simple();

		@ADTestExtensions(stringAttr2 = "ad", fooAttr2 = Foo.B, classAttr2 = String.class)
		String[] notSoSimple();

		Collection<String> stringCollection();

		@ADTestExtensions(stringAttr2 = "ad2", fooAttr2 = Foo.A)
		@AttributeDefinition(defaultValue = {
			"true"
		}, required = false)
		boolean enabled();
	}

	public void testExtensions() throws Exception {
		MetatypeVersion version = MetatypeVersion.VERSION_1_2;
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, TestExtensions.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$TestExtensions.xml");
		assertEquals(0, b.getErrors()
			.size());
		assertEquals("warnings: " + b.getWarnings(), 0, b.getWarnings()
			.size());

		System.err.println(b.getJar()
			.getResources()
			.keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "metatype", version.getNamespace(), "foo",
			"org.foo.extensions.v1");
		xt.assertNamespace(version.getNamespace());
		xt.assertExactAttribute("test.metatype.SpecMetatypeTest$TestExtensions", "metatype:MetaData/OCD/@id");
		xt.assertExactAttribute("simple", "metatype:MetaData/OCD/AD[@id='simple']/@id");
		xt.assertExactAttribute("Simple", "metatype:MetaData/OCD/AD[@id='simple']/@name");
		xt.assertExactAttribute("String", "metatype:MetaData/OCD/AD[@id='simple']/@type");
		xt.assertExactAttribute("false", "metatype:MetaData/OCD/AD[@id='enabled']/@required");
		xt.assertExactAttribute("true", "metatype:MetaData/OCD/AD[@id='enabled']/@default");
		xt.assertExactAttribute(Integer.MAX_VALUE + "", "metatype:MetaData/OCD/AD[@id='notSoSimple']/@cardinality");

		xt.assertCount(8, "metatype:MetaData/OCD/@*");
		xt.assertExactAttribute("ocd", "metatype:MetaData/OCD/@foo:stringAttr");
		xt.assertExactAttribute("A", "metatype:MetaData/OCD/@foo:fooAttr");
		xt.assertExactAttribute("foo bar", "metatype:MetaData/OCD/@foo:stringArrayAttr");
		xt.assertExactAttribute("1 2 3", "metatype:MetaData/OCD/@foo:intArrayAttr");
		xt.assertExactAttribute("true", "metatype:MetaData/OCD/@foo:booleanAttr");

		xt.assertCount(3, "metatype:MetaData/OCD/AD[@id='simple']/@*");

		xt.assertCount(8, "metatype:MetaData/OCD/AD[@id='notSoSimple']/@*");
		xt.assertExactAttribute("ad", "metatype:MetaData/OCD/AD[@id='notSoSimple']/@foo:stringAttr2");
		xt.assertExactAttribute("B", "metatype:MetaData/OCD/AD[@id='notSoSimple']/@foo:fooAttr2");
		xt.assertExactAttribute(String.class.getName(), "metatype:MetaData/OCD/AD[@id='notSoSimple']/@foo:classAttr2");
		xt.assertExactAttribute("true", "metatype:MetaData/OCD/AD[@id='notSoSimple']/@foo:booleanAttr2");

		xt.assertCount(4, "metatype:MetaData/OCD/AD[@id='stringCollection']/@*");

		xt.assertCount(9, "metatype:MetaData/OCD/AD[@id='enabled']/@*");
		xt.assertExactAttribute("ad2", "metatype:MetaData/OCD/AD[@id='enabled']/@foo:stringAttr2");
		xt.assertExactAttribute("A", "metatype:MetaData/OCD/AD[@id='enabled']/@foo:fooAttr2");
		xt.assertExactAttribute(Object.class.getName(), "metatype:MetaData/OCD/AD[@id='enabled']/@foo:classAttr2");
		xt.assertExactAttribute("true", "metatype:MetaData/OCD/AD[@id='enabled']/@foo:booleanAttr2");

	}

	@XMLAttribute(namespace = "org.foo.extensions.v1", prefix = "foo", embedIn = "*", mapping = {
		"value=simple"
	})
	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.TYPE, ElementType.METHOD
	})
	@interface Simple {
		String value() default "default";
	}

	@Simple
	@ObjectClassDefinition
	@interface SimpleConfig {
		@Simple("value")
		@AttributeDefinition
		String[] property();
	}

	public void testSimpleExtensions() throws Exception {
		MetatypeVersion version = MetatypeVersion.VERSION_1_2;
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, SimpleConfig.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$SimpleConfig.xml");
		assertEquals(0, b.getErrors()
			.size());
		assertEquals("warnings: " + b.getWarnings(), 0, b.getWarnings()
			.size());

		System.err.println(b.getJar()
			.getResources()
			.keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "metatype", version.getNamespace(), "foo",
			"org.foo.extensions.v1");
		xt.assertNamespace(version.getNamespace());
		xt.assertExactAttribute("test.metatype.SpecMetatypeTest$SimpleConfig", "metatype:MetaData/OCD/@id");

		xt.assertCount(4, "metatype:MetaData/OCD/@*");
		xt.assertExactAttribute("default", "metatype:MetaData/OCD/@foo:simple");

		xt.assertCount(5, "metatype:MetaData/OCD/AD[@id='property']/@*");
		xt.assertExactAttribute("value", "metatype:MetaData/OCD/AD[@id='property']/@foo:simple");
	}

	@XMLAttribute(namespace = "org.foo.extensions.v1", prefix = "foo", embedIn = "*", mapping = {
		"a=first", "b=second"
	})
	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.TYPE, ElementType.METHOD
	})
	@interface Mapping {
		String a() default "A";

		String b() default "B";
	}

	@Mapping
	@ObjectClassDefinition
	@interface MappingConfig {
		@Mapping(a = "one", b = "two")
		@AttributeDefinition
		String[] property();
	}

	public void testMappingExtensions() throws Exception {
		MetatypeVersion version = MetatypeVersion.VERSION_1_2;
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, MappingConfig.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$MappingConfig.xml");
		assertEquals(0, b.getErrors()
			.size());
		assertEquals("warnings: " + b.getWarnings(), 0, b.getWarnings()
			.size());

		System.err.println(b.getJar()
			.getResources()
			.keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "metatype", version.getNamespace(), "foo",
			"org.foo.extensions.v1");
		xt.assertNamespace(version.getNamespace());
		xt.assertExactAttribute("test.metatype.SpecMetatypeTest$MappingConfig", "metatype:MetaData/OCD/@id");

		xt.assertCount(5, "metatype:MetaData/OCD/@*");
		xt.assertExactAttribute("A", "metatype:MetaData/OCD/@foo:first");
		xt.assertExactAttribute("B", "metatype:MetaData/OCD/@foo:second");

		xt.assertCount(6, "metatype:MetaData/OCD/AD[@id='property']/@*");
		xt.assertExactAttribute("one", "metatype:MetaData/OCD/AD[@id='property']/@foo:first");
		xt.assertExactAttribute("two", "metatype:MetaData/OCD/AD[@id='property']/@foo:second");
	}

	@ObjectClassDefinition
	@interface Escapes {
		@AttributeDefinition(defaultValue = {
			" , \\", "a,b", "c,d", "'apostrophe'", "\"quote\"&amp;"
		})
		String[] escapes();
	}

	public void testEscapes() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, Escapes.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$Escapes.xml");
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());
		System.err.println(b.getJar()
			.getResources()
			.keySet());
		assertNotNull(r);
		IO.copy(r.openInputStream(), System.err);

		XmlTester xt = xmlTester12(r);

		assertAD(xt, "escapes", "Escapes", null, null, "\\ \\,\\ \\\\,a\\,b,c\\,d,'apostrophe',\"quote\"&amp;",
			2147483647, "String", null, null, null);

	}

	@Mapping
	@Meta.OCD
	interface C {
		@Meta.AD(required = false)
		Integer port();
	}

	// cf issue 1130
	public void testBndAnnoCompatible() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin_test"));
		b.setProperty("Export-Package", "test.metatype");
		b.setProperty(Constants.METATYPE_ANNOTATIONS, C.class.getName());
		b.build();
		Resource r = b.getJar()
			.getResource("OSGI-INF/metatype/test.metatype.SpecMetatypeTest$C.xml");
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());
		assertNull(r);
	}

}
