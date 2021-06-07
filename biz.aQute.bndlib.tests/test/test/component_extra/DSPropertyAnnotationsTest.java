package test.component_extra;

import static aQute.bnd.test.BndTestCase.assertOk;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ComponentPropertyType;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.test.XmlTester;
import junit.framework.TestCase;

/*
 * Placed in a new package to avoid breaking lots of existing tests with the additional packages
 * we need to import.
 */
public class DSPropertyAnnotationsTest extends TestCase {

	private Jar jar;

	@Override
	public void setUp() throws IOException, Exception {
		super.setUp();

		Builder b = new Builder();
		b.setProperty("Private-Package", DSPropertyAnnotationsTest.class.getPackage()
			.getName());
		b.addClasspath(new File("bin_test"));

		jar = b.build();
		assertOk(b);
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface MyProp {
		String value();
	}

	@MyProp("myValue")
	@Component
	public static class SimpleDSPropertyAnnotated {}

	public void testPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + SimpleDSPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(SimpleDSPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("myValue", "scr:component/property[@name='my.prop']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='my.prop']/@type");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface MyPropWithDefault {
		String value() default "defaultValue";
	}

	@MyPropWithDefault("myValue")
	@Component
	public static class DSPropertyAnnotatedNotUsingDefault {}

	public void testPropertyAnnotationNotUsingDefault() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + DSPropertyAnnotatedNotUsingDefault.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(DSPropertyAnnotatedNotUsingDefault.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("myValue", "scr:component/property[@name='my.prop.with.default']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='my.prop.with.default']/@type");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface MyDefaultProp {
		String value() default "defaultValue";
	}

	@MyDefaultProp
	@Component
	public static class DefaultDSPropertyAnnotated {}

	public void testDefaultedPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + DefaultDSPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(DefaultDSPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("defaultValue", "scr:component/property[@name='my.default.prop']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='my.default.prop']/@type");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface MyIntProp {
		int value();
	}

	@MyIntProp(42)
	@Component
	public static class IntDSPropertyAnnotated {}

	public void testIntPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + IntDSPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(IntDSPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("42", "scr:component/property[@name='my.int.prop']/@value");
		xt.assertAttribute("Integer", "scr:component/property[@name='my.int.prop']/@type");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface MyClassProp {
		Class<?> value();
	}

	@MyClassProp(List.class)
	@Component
	public static class ClassDSPropertyAnnotated {}

	public void testClassPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + ClassDSPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(ClassDSPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute(List.class.getName(), "scr:component/property[@name='my.class.prop']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='my.class.prop']/@type");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface MyMultiProp {
		String a_string_prop();

		long a_long_prop();
	}

	@MyMultiProp(a_string_prop = "foo", a_long_prop = 1234)
	@Component
	public static class MultiDSPropertyAnnotated {}

	public void testMultiPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + MultiDSPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(MultiDSPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(2, "scr:component/property");
		xt.assertAttribute("foo", "scr:component/property[@name='a.string.prop']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='a.string.prop']/@type");
		xt.assertAttribute("1234", "scr:component/property[@name='a.long.prop']/@value");
		xt.assertAttribute("Long", "scr:component/property[@name='a.long.prop']/@type");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface PrefixedProp {
		public static final String PREFIX_ = "bnd.";

		String value() default "bar";
	}

	@PrefixedProp
	@Component
	public static class PrefixPropertyAnnotated {}

	public void testPrefixPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + PrefixPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(PrefixPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("bar", "scr:component/property[@name='bnd.prefixed.prop']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='bnd.prefixed.prop']/@type");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface ArrayProp {
		String[] value() default {
			"fizz", "buzz", "fizzbuzz"
		};
	}

	@ArrayProp
	@Component
	public static class ArrayPropertyAnnotated {}

	public void testArrayPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + ArrayPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(ArrayPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("String", "scr:component/property[@name='array.prop']/@type");
		xt.assertAttribute("fizz\nbuzz\nfizzbuzz", "scr:component/property[@name='array.prop']");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface IntArrayProp {
		int[] value();
	}

	@IntArrayProp({
		1, 2, 3
	})
	@Component
	public static class IntArrayPropertyAnnotated {}

	public void testIntArrayPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + IntArrayPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(IntArrayPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("Integer", "scr:component/property[@name='int.array.prop']/@type");
		xt.assertAttribute("1\n2\n3", "scr:component/property[@name='int.array.prop']");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface DefaultIntArrayProp {
		int[] value() default {
			1, 2, 3
		};
	}

	@DefaultIntArrayProp
	@Component
	public static class DefaultIntArrayPropertyAnnotated {}

	public void testDefaultIntArrayPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + DefaultIntArrayPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(DefaultIntArrayPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("Integer", "scr:component/property[@name='default.int.array.prop']/@type");
		xt.assertAttribute("1\n2\n3", "scr:component/property[@name='default.int.array.prop']");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface ClassArrayProp {
		Class<?>[] value();
	}

	@ClassArrayProp({
		List.class, Set.class, Queue.class
	})
	@Component
	public static class ClassArrayPropertyAnnotated {}

	public void testClassArrayPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + ClassArrayPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(ClassArrayPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("String", "scr:component/property[@name='class.array.prop']/@type");
		xt.assertAttribute(List.class.getName() + "\n" + Set.class.getName() + "\n" + Queue.class.getName(),
			"scr:component/property[@name='class.array.prop']");
	}

	@ClassArrayProp({})
	@Component
	public static class EmptyClassArrayPropertyAnnotated {}

	public void testEmptyClassArrayPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + EmptyClassArrayPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(EmptyClassArrayPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(0, "scr:component/property");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface DefaultClassArrayProp {
		Class<?>[] value() default {
			List.class, Set.class, Queue.class
		};
	}

	@DefaultClassArrayProp
	@Component
	public static class DefaultClassArrayPropertyAnnotated {}

	public void testDefaultClassArrayPropertyAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + DefaultClassArrayPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(DefaultClassArrayPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("String", "scr:component/property[@name='default.class.array.prop']/@type");
		xt.assertAttribute(List.class.getName() + "\n" + Set.class.getName() + "\n" + Queue.class.getName(),
			"scr:component/property[@name='default.class.array.prop']");
	}

	@ComponentPropertyType
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public static @interface MarkerProp {}

	@MarkerProp
	@Component
	public static class MarkerPropertyAnnotated {}

	public void testMarkerAnnotation() throws Exception {

		Resource r = jar.getResource("OSGI-INF/" + MarkerPropertyAnnotated.class.getName() + ".xml");

		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(MarkerPropertyAnnotated.class.getName(), "scr:component/implementation/@class");
		xt.assertCount(1, "scr:component/property");
		xt.assertAttribute("true", "scr:component/property[@name='marker.prop']/@value");
		xt.assertAttribute("Boolean", "scr:component/property[@name='marker.prop']/@type");
	}
}
