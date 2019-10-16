package test.component;

import static aQute.bnd.test.BndTestCase.assertOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.xpath.XPathExpressionException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.CollectionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import aQute.bnd.annotation.xml.XMLAttribute;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.test.XmlTester;
import aQute.bnd.version.Version;
import aQute.lib.filter.Filter;
import aQute.lib.io.IO;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Test for use of DS components specified using spec DS annotations.
 */
@SuppressWarnings({
	"resource", "restriction", "serial"
})
public class DSAnnotationTest extends TestCase {

	public static final String	FELIX_1_2				= "http://felix.apache.org/xmlns/scr/v1.2.0-felix";

	private static String[]		SERIALIZABLE_RUNNABLE	= {
		Serializable.class.getName(), Runnable.class.getName()
	};
	private static String[]		OBJECT					= {
		Object.class.getName()
	};

	/**
	 * Property test
	 */

	@Component()
	public static class ValidNSVersion {

	}

	public void testExceedsVersion() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*ValidNSVersion");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;maximum=1.2.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));
		b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
		Jar jar = b.build();
		assertTrue(b.check(
			"component 1.3.0 version test.component.DSAnnotationTest.ValidNSVersion exceeds -dsannotations-options version;maximum version 1.2.0 because base"));
	}

	@Component()
	public static class RequiresV1_3 {

		@Reference(service = String.class)
		void setX(Map<String, Object> map) {

		}
	}

	public void testRequires1_3() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*RequiresV1_3");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.2.0;maximum=1.2.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));
		b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
		Jar jar = b.build();
		assertTrue(b.check("component 1.3.0 version test.component.DSAnnotationTest\\$RequiresV1_3 exceeds"));

		System.out.println(jar.getResources()
			.keySet());

		Resource resource = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$RequiresV1_3.xml");
		assertThat(resource).isNotNull();
		String s = IO.collect(resource.openInputStream());
		System.out.println(s);
	}

	public void testValidNamespaceVersion() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*ValidNSVersion");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));
		b.addClasspath(new File("jar/osgi.jar")); // v1.0.0
		Jar jar = b.build();

		if (!b.check())
			fail();
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, null);

		Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$ValidNSVersion.xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
	}

	public void testDuplicateExtender() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.ds14.*");
			b.setProperty("-includepackage", "test.component.ds14");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();
			Domain domain = Domain.domain(jar.getManifest());
			Parameters exportPackages = domain.getExportPackage();
			assertThat(exportPackages).isEmpty();
			Parameters importPackages = domain.getImportPackage();
			assertThat(importPackages).doesNotContainKeys("test.component.ds14");
			Parameters reqs = domain.getRequireCapability();
			System.out.println(reqs);
			assertThat(reqs).containsOnlyKeys("osgi.extender", "osgi.ee");
			assertThat(reqs.get("osgi.extender")).containsOnlyKeys("filter:");
			checkExtenderVersion(reqs, ComponentConstants.COMPONENT_SPECIFICATION_VERSION);
		}
	}

	// #2876
	public void testExportComponentImplPackage() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.ds14.*");
			b.setProperty("Export-Package", "test.component.ds14");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();
			Domain domain = Domain.domain(jar.getManifest());
			Parameters exportPackages = domain.getExportPackage();
			assertThat(exportPackages).containsOnlyKeys("test.component.ds14");
			Parameters importPackages = domain.getImportPackage();
			assertThat(importPackages).containsKeys("test.component.ds14");
		}
	}

	/**
	 * Property test
	 */

	@Component(xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0", property = {
		"x:Integer=3.0", "a=1", "a=2", "b=1", "boolean:Boolean=true", "byte:Byte=1", "char:Character=1",
		"short:Short=3", "integer:Integer=3", "long:Long=3", "float:Float=3.0", "double:Double=3e7", "string:String=%",
		"wrongInteger:Integer=blabla", "\n\r\t \u0343\u0344\u0345\u0346\n:Integer=3"
	})
	public static class PropertiesTestx {

	}

	public void testProperties() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*x");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		if (!b.check("Not a valid number blabla for Integer", "Not a valid number 3.0 for Integer"))
			fail();
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, null);

		//
		// Test all the defaults
		//

		Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$PropertiesTestx.xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.1.0");
		xt.assertAttribute("1", "scr:component/property[@name='b']/@value");
		xt.assertAttribute("", "scr:component/property[@name='a']/@value");
		xt.assertAttribute("Byte", "scr:component/property[@name='byte']/@type");
		xt.assertAttribute("Boolean", "scr:component/property[@name='boolean']/@type");
		xt.assertAttribute("Character", "scr:component/property[@name='char']/@type");
		xt.assertAttribute(Integer.toString('1'), "scr:component/property[@name='char']/@value");
		xt.assertAttribute("Short", "scr:component/property[@name='short']/@type");
		xt.assertAttribute("Integer", "scr:component/property[@name='integer']/@type");
		xt.assertAttribute("Long", "scr:component/property[@name='long']/@type");
		xt.assertAttribute("Float", "scr:component/property[@name='float']/@type");
		xt.assertAttribute("Double", "scr:component/property[@name='double']/@type");
		xt.assertAttribute("Integer", "scr:component/property[@name='\u0343\u0344\u0345\u0346']/@type");
	}

	/**
	 * Check that a DS 1.0 compotible class with annotations ends up with the DS
	 * 1.0 (no) namespace
	 */
	@Component()
	public static class DS10_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		protected void xsetLogService(@SuppressWarnings("unused") LogService log) {

		}

		protected void unxsetLogService(@SuppressWarnings("unused") LogService log) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component
	public static class DS11_1_very_basic {

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		protected void setLogService(@SuppressWarnings("unused") LogService log) {}

		protected void unsetLogService(@SuppressWarnings("unused") LogService log) {}
	}

	@Component
	public static class DS11_2_very_basic {

		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		protected void setLogService(@SuppressWarnings("unused") LogService log) {}

		protected void unsetLogService(@SuppressWarnings("unused") LogService log) {}
	}

	@Component
	public static class DS11_3_very_basic {

		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		void setLogService(@SuppressWarnings("unused") LogService log) {}

		protected void unsetLogService(@SuppressWarnings("unused") LogService log) {}
	}

	@Component
	public static class DS11_4_very_basic {

		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		protected void setLogService(@SuppressWarnings("unused") LogService log) {}

		void unsetLogService(@SuppressWarnings("unused") LogService log) {}
	}

	@Component
	public static class DS11_5_very_basic {

		@Activate
		public void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		protected void setLogService(@SuppressWarnings("unused") LogService log) {}

		protected void unsetLogService(@SuppressWarnings("unused") LogService log) {}
	}

	@Component
	public static class DS11_6_very_basic {

		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		public void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		protected void setLogService(@SuppressWarnings("unused") LogService log) {}

		protected void unsetLogService(@SuppressWarnings("unused") LogService log) {}
	}

	@Component
	public static class DS11_7_very_basic {

		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		public void setLogService(@SuppressWarnings("unused") LogService log) {}

		protected void unsetLogService(@SuppressWarnings("unused") LogService log) {}
	}

	@Component
	public static class DS11_8_very_basic {

		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		protected void setLogService(@SuppressWarnings("unused") LogService log) {}

		public void unsetLogService(@SuppressWarnings("unused") LogService log) {}
	}

	/**
	 * Check that a DS 1.1 compotible class ends up with the DS 1.1 namespace
	 * and appropriate activate/deactivate attributes
	 */
	@Component()
	public static class DS11_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") BundleContext ctx) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") int cause) {}

		@Reference
		void xsetLogService(@SuppressWarnings("unused") LogService log) {

		}

		void unxsetLogService(@SuppressWarnings("unused") LogService log) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that a DS 1.1 bind method causes a DS 1.1 namespace
	 */
	@Component()
	public static class DS11_ref1_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		void xsetLogService(@SuppressWarnings("unused") LogService log, @SuppressWarnings({
			"unused", "rawtypes"
		}) Map map) {

		}

		void unxsetLogService(@SuppressWarnings("unused") LogService log) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that a DS 1.1 unbind method causes a DS 1.1 namespace
	 */
	@Component()
	public static class DS11_ref2_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		void xsetLogService(@SuppressWarnings("unused") LogService log) {

		}

		void unxsetLogService(@SuppressWarnings("unused") LogService log, @SuppressWarnings("unused") Map<?, ?> map) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * The basic test. This test will take an all default component and a
	 * component that has all values set. Since the activate/deactivate methods
	 * have non-default names and there is a modified methods, this is a DS 1.1
	 * component.
	 */
	@Component()
	public static class Defaults_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void open() {}

		@Deactivate
		void close() {}

		@Modified
		void modified() {}

		@Reference
		void xsetLogService(@SuppressWarnings("unused") LogService log) {

		}

		void unxsetLogService(@SuppressWarnings("unused") LogService log) {

		}

		// void modifiedLogService(LogService log) {
		//
		// }

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component(service = Object.class, configurationPolicy = ConfigurationPolicy.IGNORE, enabled = false, factory = "factory", immediate = false, name = "name", property = {
		"a=1", "a=2", "b=3"
	}, properties = "resource.props", servicefactory = false, configurationPid = "configuration-pid", xmlns = "xmlns")
	public static class Explicit_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void open() {}

		@Deactivate
		void close() {}

		@Modified
		void changed() {}

		@Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, name = "foo", policy = ReferencePolicy.DYNAMIC, service = LogService.class, target = "(objectclass=*)", unbind = "unset", updated = "updatedLogService", policyOption = ReferencePolicyOption.GREEDY)
		void setLogService(@SuppressWarnings("unused") Object log) {

		}

		void unset(@SuppressWarnings("unused") Object log) {

		}

		void unset() {

		}

		void unsetLogService(@SuppressWarnings("unused") LogService log) {

		}

		void updatedLogService(@SuppressWarnings("unused") Object log) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	public void testBasic() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*_basic");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.0.0");
		b.setProperty("Private-Package", "test.component");
		b.setProperty("-includeresource.resourceprops", "resource.props;literal=\"\"");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);

		{
			//
			// Test all the DS 1.0 defaults
			//

			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$DS10_basic.xml");
			System.err.println(Processor.join(jar.getResources()
				.keySet(), "\n"));
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream());

			// Test the defaults
			xt.assertAttribute("test.component.DSAnnotationTest$DS10_basic", "component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("test.component.DSAnnotationTest$DS10_basic", "component/@name");

			xt.assertAttribute("", "component/@configuration-policy");
			xt.assertAttribute("", "component/@immediate");
			xt.assertAttribute("", "component/@enabled");
			xt.assertAttribute("", "component/@factory");
			xt.assertAttribute("", "component/service/@servicefactory");
			xt.assertAttribute("", "component/@configuration-pid");
			xt.assertAttribute("", "component/@activate");
			xt.assertAttribute("", "component/@deactivate");
			xt.assertAttribute("", "component/@modified");
			xt.assertAttribute("java.io.Serializable", "component/service/provide[1]/@interface");
			xt.assertAttribute("java.lang.Runnable", "component/service/provide[2]/@interface");

			xt.assertAttribute("0", "count(component/properties)");
			xt.assertAttribute("0", "count(component/property)");

			xt.assertAttribute("xsetLogService", "component/reference[1]/@name");
			xt.assertAttribute("", "component/reference[1]/@target");
			xt.assertAttribute("xsetLogService", "component/reference[1]/@bind");
			xt.assertAttribute("unxsetLogService", "component/reference[1]/@unbind");
			xt.assertAttribute("", "component/reference[1]/@cardinality");
			xt.assertAttribute("", "component/reference[1]/@policy");
			xt.assertAttribute("", "component/reference[1]/@target");
			xt.assertAttribute("", "component/reference[1]/@policy-option");
		}
		{
			//
			// Test package methods >> DS 1.1
			//
			verifyDS11VeryBasic(jar, DS11_1_very_basic.class);
			verifyDS11VeryBasic(jar, DS11_2_very_basic.class);
			verifyDS11VeryBasic(jar, DS11_3_very_basic.class);
			verifyDS11VeryBasic(jar, DS11_4_very_basic.class);
			verifyDS11VeryBasic(jar, DS11_5_very_basic.class);
			verifyDS11VeryBasic(jar, DS11_6_very_basic.class);
			verifyDS11VeryBasic(jar, DS11_7_very_basic.class);
			verifyDS11VeryBasic(jar, DS11_8_very_basic.class);
		}
		{
			//
			// Test the DS 1.1 defaults
			//

			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$DS11_basic.xml");
			System.err.println(Processor.join(jar.getResources()
				.keySet(), "\n"));
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.1.0"); // #136
																												// was
																												// http://www.osgi.org/xmlns/scr/1.1.0

			// Test the defaults
			xt.assertAttribute("test.component.DSAnnotationTest$DS11_basic", "scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("test.component.DSAnnotationTest$DS11_basic", "scr:component/@name");

			xt.assertAttribute("", "scr:component/@configuration-policy");
			xt.assertAttribute("", "scr:component/@immediate");
			xt.assertAttribute("", "scr:component/@enabled");
			xt.assertAttribute("", "scr:component/@factory");
			xt.assertAttribute("", "scr:component/service/@servicefactory");
			xt.assertAttribute("", "scr:component/@configuration-pid");
			xt.assertAttribute("activate", "scr:component/@activate");
			xt.assertAttribute("deactivate", "scr:component/@deactivate");
			xt.assertAttribute("", "scr:component/@modified");
			xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
			xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

			xt.assertAttribute("0", "count(scr:component/properties)");
			xt.assertAttribute("0", "count(scr:component/property)");

			xt.assertAttribute("xsetLogService", "scr:component/reference[1]/@name");
			xt.assertAttribute("", "scr:component/reference[1]/@target");
			xt.assertAttribute("xsetLogService", "scr:component/reference[1]/@bind");
			xt.assertAttribute("unxsetLogService", "scr:component/reference[1]/@unbind");
			xt.assertAttribute("", "scr:component/reference[1]/@cardinality");
			xt.assertAttribute("", "scr:component/reference[1]/@policy");
			xt.assertAttribute("", "scr:component/reference[1]/@target");
			xt.assertAttribute("", "scr:component/reference[1]/@policy-option");
		}
		{
			//
			// Test a DS 1.1 bind method results in the DS 1.1 namespace
			//

			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$DS11_ref1_basic.xml");
			System.err.println(Processor.join(jar.getResources()
				.keySet(), "\n"));
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.1.0"); // #136
																												// was
																												// http://www.osgi.org/xmlns/scr/1.1.0

			// Test the defaults
			xt.assertAttribute("test.component.DSAnnotationTest$DS11_ref1_basic",
				"scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("test.component.DSAnnotationTest$DS11_ref1_basic", "scr:component/@name");

			xt.assertAttribute("", "scr:component/@configuration-policy");
			xt.assertAttribute("", "scr:component/@immediate");
			xt.assertAttribute("", "scr:component/@enabled");
			xt.assertAttribute("", "scr:component/@factory");
			xt.assertAttribute("", "scr:component/service/@servicefactory");
			xt.assertAttribute("", "scr:component/@configuration-pid");
			xt.assertAttribute("activate", "scr:component/@activate");
			xt.assertAttribute("deactivate", "scr:component/@deactivate");
			xt.assertAttribute("", "scr:component/@modified");
			xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
			xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

			xt.assertAttribute("0", "count(scr:component/properties)");
			xt.assertAttribute("0", "count(scr:component/property)");

			xt.assertAttribute("xsetLogService", "scr:component/reference[1]/@name");
			xt.assertAttribute("", "scr:component/reference[1]/@target");
			xt.assertAttribute("xsetLogService", "scr:component/reference[1]/@bind");
			xt.assertAttribute("unxsetLogService", "scr:component/reference[1]/@unbind");
			xt.assertAttribute("", "scr:component/reference[1]/@cardinality");
			xt.assertAttribute("", "scr:component/reference[1]/@policy");
			xt.assertAttribute("", "scr:component/reference[1]/@target");
			xt.assertAttribute("", "scr:component/reference[1]/@policy-option");
		}
		{
			//
			// Test a DS 1.1 unbind method results in the DS 1.1 namespace
			//

			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$DS11_ref2_basic.xml");
			System.err.println(Processor.join(jar.getResources()
				.keySet(), "\n"));
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.1.0"); // #136
																												// was
																												// http://www.osgi.org/xmlns/scr/1.1.0

			// Test the defaults
			xt.assertAttribute("test.component.DSAnnotationTest$DS11_ref2_basic",
				"scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("test.component.DSAnnotationTest$DS11_ref2_basic", "scr:component/@name");

			xt.assertAttribute("", "scr:component/@configuration-policy");
			xt.assertAttribute("", "scr:component/@immediate");
			xt.assertAttribute("", "scr:component/@enabled");
			xt.assertAttribute("", "scr:component/@factory");
			xt.assertAttribute("", "scr:component/service/@servicefactory");
			xt.assertAttribute("", "scr:component/@configuration-pid");
			xt.assertAttribute("activate", "scr:component/@activate");
			xt.assertAttribute("deactivate", "scr:component/@deactivate");
			xt.assertAttribute("", "scr:component/@modified");
			xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
			xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

			xt.assertAttribute("0", "count(scr:component/properties)");
			xt.assertAttribute("0", "count(scr:component/property)");

			xt.assertAttribute("xsetLogService", "scr:component/reference[1]/@name");
			xt.assertAttribute("", "scr:component/reference[1]/@target");
			xt.assertAttribute("xsetLogService", "scr:component/reference[1]/@bind");
			xt.assertAttribute("unxsetLogService", "scr:component/reference[1]/@unbind");
			xt.assertAttribute("", "scr:component/reference[1]/@cardinality");
			xt.assertAttribute("", "scr:component/reference[1]/@policy");
			xt.assertAttribute("", "scr:component/reference[1]/@target");
			xt.assertAttribute("", "scr:component/reference[1]/@policy-option");
		}
		{
			//
			// Test all the defaults
			//

			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$Defaults_basic.xml");
			System.err.println(Processor.join(jar.getResources()
				.keySet(), "\n"));
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.1.0"); // #136
																												// was
																												// http://www.osgi.org/xmlns/scr/1.1.0

			// Test the defaults
			xt.assertAttribute("test.component.DSAnnotationTest$Defaults_basic", "scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("test.component.DSAnnotationTest$Defaults_basic", "scr:component/@name");

			xt.assertAttribute("", "scr:component/@configuration-policy");
			xt.assertAttribute("", "scr:component/@immediate");
			xt.assertAttribute("", "scr:component/@enabled");
			xt.assertAttribute("", "scr:component/@factory");
			xt.assertAttribute("", "scr:component/service/@servicefactory");
			xt.assertAttribute("", "scr:component/@configuration-pid");
			xt.assertAttribute("open", "scr:component/@activate");
			xt.assertAttribute("close", "scr:component/@deactivate");
			xt.assertAttribute("modified", "scr:component/@modified");
			xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
			xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

			xt.assertAttribute("0", "count(scr:component/properties)");
			xt.assertAttribute("0", "count(scr:component/property)");

			xt.assertAttribute("xsetLogService", "scr:component/reference[1]/@name");
			xt.assertAttribute("", "scr:component/reference[1]/@target");
			xt.assertAttribute("xsetLogService", "scr:component/reference[1]/@bind");
			xt.assertAttribute("unxsetLogService", "scr:component/reference[1]/@unbind");
			xt.assertAttribute("", "scr:component/reference[1]/@cardinality");
			xt.assertAttribute("", "scr:component/reference[1]/@policy");
			xt.assertAttribute("", "scr:component/reference[1]/@target");
			xt.assertAttribute("", "scr:component/reference[1]/@policy-option");
		}
		{
			//
			// Test explicit
			//

			Resource r = jar.getResource("OSGI-INF/name.xml");
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "xmlns");

			// Test the defaults
			xt.assertAttribute("test.component.DSAnnotationTest$Explicit_basic", "scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("name", "scr:component/@name");

			xt.assertAttribute("ignore", "scr:component/@configuration-policy");
			xt.assertAttribute("configuration-pid", "scr:component/@configuration-pid");
			xt.assertAttribute("false", "scr:component/@immediate");
			xt.assertAttribute("false", "scr:component/@enabled");
			xt.assertAttribute("factory", "scr:component/@factory");
			xt.assertAttribute("false", "scr:component/service/@servicefactory");
			xt.assertAttribute("open", "scr:component/@activate");
			xt.assertAttribute("close", "scr:component/@deactivate");
			xt.assertAttribute("changed", "scr:component/@modified");
			xt.assertAttribute("java.lang.Object", "scr:component/service/provide[1]/@interface");
			xt.assertAttribute("1", "count(scr:component/service/provide)");

			xt.assertAttribute("1", "count(scr:component/properties)");
			xt.assertAttribute("2", "count(scr:component/property)");

			xt.assertAttribute("(objectclass=*)", "scr:component/reference[1]/@target");
			xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
			xt.assertAttribute("unset", "scr:component/reference[1]/@unbind");
			xt.assertAttribute("updatedLogService", "scr:component/reference[1]/@updated");
			xt.assertAttribute("1..n", "scr:component/reference[1]/@cardinality");
			xt.assertAttribute("dynamic", "scr:component/reference[1]/@policy");
			xt.assertAttribute("(objectclass=*)", "scr:component/reference[1]/@target");

			xt.assertAttribute("2", "count(scr:component/property)");
			xt.assertAttribute("1", "count(scr:component/properties)");
			xt.assertAttribute("resource.props", "scr:component/properties[1]/@entry");
			xt.assertAttribute("greedy", "scr:component/reference[1]/@policy-option");
		}
		Attributes a = getAttr(jar);
		checkProvides(a, SERIALIZABLE_RUNNABLE, OBJECT);
		// n.b. we should merge the 2 logService requires, so when we fix that
		// we'll need to update this test.
		// one is plain, other has cardinality multiple.
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, LogService.class.getName(),
			LogService.class.getName());
	}

	private void verifyDS11VeryBasic(Jar jar, Class<?> clazz) throws Exception, XPathExpressionException {
		String className = clazz.getName();

		Resource r = jar.getResource("OSGI-INF/" + className + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.1.0"); // #136
																											// was
																											// http://www.osgi.org/xmlns/scr/1.1.0

		// Test the defaults
		xt.assertAttribute(className, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(className, "scr:component/@name");

		xt.assertAttribute("", "scr:component/@configuration-policy");
		xt.assertAttribute("", "scr:component/@immediate");
		xt.assertAttribute("", "scr:component/@enabled");
		xt.assertAttribute("", "scr:component/@factory");
		xt.assertAttribute("", "scr:component/service/@servicefactory");
		xt.assertAttribute("", "scr:component/@configuration-pid");
		xt.assertAttribute("activate", "scr:component/@activate");
		xt.assertAttribute("deactivate", "scr:component/@deactivate");
		xt.assertAttribute("", "scr:component/@modified");

		xt.assertAttribute("0", "count(scr:component/properties)");
		xt.assertAttribute("0", "count(scr:component/property)");

		xt.assertAttribute("LogService", "scr:component/reference[1]/@name");
		xt.assertAttribute("", "scr:component/reference[1]/@target");
		xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
		xt.assertAttribute("unsetLogService", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("", "scr:component/reference[1]/@cardinality");
		xt.assertAttribute("", "scr:component/reference[1]/@policy");
		xt.assertAttribute("", "scr:component/reference[1]/@target");
		xt.assertAttribute("", "scr:component/reference[1]/@policy-option");
	}

	/**
	 * Check that a Felix 1.2 compotible class ends up with the Felix 1.2
	 * namespace and appropriate activate/deactivate attributes
	 */
	@Component(xmlns = FELIX_1_2)
	public static class activate_basicFelix12 implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		Map<String, Object> activate(@SuppressWarnings("unused") ComponentContext cc,
			@SuppressWarnings("unused") BundleContext ctx) {
			return null;
		}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") int cause) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		void setLogService(@SuppressWarnings("unused") LogService log) {}

		void unsetLogService(@SuppressWarnings("unused") LogService log) {}

		void updatedLogService(@SuppressWarnings("unused") LogService log) {}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component(xmlns = FELIX_1_2)
	public static class deactivate_basicFelix12 implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") BundleContext ctx) {}

		@Deactivate
		Map<String, Object> deactivate(@SuppressWarnings("unused") ComponentContext cc,
			@SuppressWarnings("unused") int cause) {
			return null;
		}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		void setLogService(@SuppressWarnings("unused") LogService log) {}

		void unsetLogService(@SuppressWarnings("unused") LogService log) {}

		void updatedLogService(@SuppressWarnings("unused") LogService log) {}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component(xmlns = FELIX_1_2)
	public static class modified_basicFelix12 implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") BundleContext ctx) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") int cause) {}

		@Modified
		Map<String, Object> modified(@SuppressWarnings("unused") ComponentContext cc) {
			return null;
		}

		@Reference
		void setLogService(@SuppressWarnings("unused") LogService log) {}

		void unsetLogService(@SuppressWarnings("unused") LogService log) {}

		void updatedLogService(@SuppressWarnings("unused") LogService log) {}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component(xmlns = FELIX_1_2)
	public static class bind_basicFelix12 implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") BundleContext ctx) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") int cause) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		Map<String, Object> setLogService(@SuppressWarnings("unused") LogService log) {
			return null;
		}

		void unsetLogService(@SuppressWarnings("unused") LogService log) {}

		void updatedLogService(@SuppressWarnings("unused") LogService log) {}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component(xmlns = FELIX_1_2)
	public static class unbind_basicFelix12 implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") BundleContext ctx) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") int cause) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		void setLogService(@SuppressWarnings("unused") LogService log) {}

		Map<String, Object> unsetLogService(@SuppressWarnings("unused") LogService log) {
			return null;
		}

		void updatedLogService(@SuppressWarnings("unused") LogService log) {}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component(xmlns = FELIX_1_2)
	public static class updated_basicFelix12 implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") BundleContext ctx) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") int cause) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		void setLogService(@SuppressWarnings("unused") LogService log) {}

		void unsetLogService(@SuppressWarnings("unused") LogService log) {}

		Map<String, Object> updatedLogService(@SuppressWarnings("unused") LogService log) {
			return null;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	public void testBasicFelix12() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*_basicFelix12");
		b.setProperty("-ds-felix-extensions", "true");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a, SERIALIZABLE_RUNNABLE);
		checkRequires(a, null, LogService.class.getName());

		// Test Felix12 activate gives Felix 1.2 namespace
		checkDSFelix12(jar, "test.component.DSAnnotationTest$activate_basicFelix12");
		// Test Felix12 deactivate gives Felix 1.2 namespace
		checkDSFelix12(jar, "test.component.DSAnnotationTest$deactivate_basicFelix12");
		// Test Felix12 modified gives Felix 1.2 namespace
		checkDSFelix12(jar, "test.component.DSAnnotationTest$modified_basicFelix12");
		// Test Felix12 bind gives Felix 1.2 namespace
		checkDSFelix12(jar, "test.component.DSAnnotationTest$bind_basicFelix12");
		// Test Felix12 bind gives Felix 1.2 namespace
		checkDSFelix12(jar, "test.component.DSAnnotationTest$unbind_basicFelix12");
		// Test Felix12 updated gives Felix 1.2 namespace
		checkDSFelix12(jar, "test.component.DSAnnotationTest$updated_basicFelix12");
	}

	private void checkDSFelix12(Jar jar, String name) throws Exception, XPathExpressionException {
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", FELIX_1_2);
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertAttribute("", "scr:component/@configuration-policy");
		xt.assertAttribute("", "scr:component/@immediate");
		xt.assertAttribute("", "scr:component/@enabled");
		xt.assertAttribute("", "scr:component/@factory");
		xt.assertAttribute("", "scr:component/service/@servicefactory");
		xt.assertAttribute("", "scr:component/@configuration-pid");
		xt.assertAttribute("activate", "scr:component/@activate");
		xt.assertAttribute("deactivate", "scr:component/@deactivate");
		xt.assertAttribute("modified", "scr:component/@modified");
		xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
		xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

		xt.assertAttribute("0", "count(scr:component/properties)");
		xt.assertAttribute("0", "count(scr:component/property)");

		xt.assertAttribute("LogService", "scr:component/reference[1]/@name");
		xt.assertAttribute("", "scr:component/reference[1]/@target");
		xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
		xt.assertAttribute("unsetLogService", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("updatedLogService", "scr:component/reference[1]/@updated");
		xt.assertAttribute("", "scr:component/reference[1]/@cardinality");
		xt.assertAttribute("", "scr:component/reference[1]/@policy");
		xt.assertAttribute("", "scr:component/reference[1]/@target");
		xt.assertAttribute("", "scr:component/reference[1]/@policy-option");
	}

	/**
	 * Tests all the different enum values. This also tests the ordering.
	 */
	@Component(name = "enums")
	public static class Enums {

		@Reference
		void setA(@SuppressWarnings("unused") LogService l) {}

		@Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
		void setB(@SuppressWarnings("unused") LogService l) {}

		void unsetB(@SuppressWarnings("unused") LogService l) {}

		@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.RELUCTANT)
		void setE(@SuppressWarnings("unused") LogService l) {}

		void unsetE(@SuppressWarnings("unused") LogService l) {}

		@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.RELUCTANT)
		void setC(@SuppressWarnings("unused") LogService l) {}

		@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
		void setD(@SuppressWarnings("unused") LogService l) {}

		void unsetD(@SuppressWarnings("unused") LogService l) {}

	}

	public void testEnums() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*Enums");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.0.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, null, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/enums.xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.2.0");

		xt.assertAttribute("A", "scr:component/reference[1]/@name");
		xt.assertAttribute("", "scr:component/reference[1]/@cardinality");
		xt.assertAttribute("", "scr:component/reference[1]/@policy");
		xt.assertAttribute("", "scr:component/reference[1]/@policy-option");

		xt.assertAttribute("B", "scr:component/reference[2]/@name");
		xt.assertAttribute("1..n", "scr:component/reference[2]/@cardinality");
		xt.assertAttribute("dynamic", "scr:component/reference[2]/@policy");
		xt.assertAttribute("greedy", "scr:component/reference[2]/@policy-option");

		xt.assertAttribute("C", "scr:component/reference[3]/@name");
		xt.assertAttribute("1..1", "scr:component/reference[3]/@cardinality");
		xt.assertAttribute("static", "scr:component/reference[3]/@policy");
		xt.assertAttribute("reluctant", "scr:component/reference[3]/@policy-option");

		xt.assertAttribute("D", "scr:component/reference[4]/@name");
		xt.assertAttribute("0..n", "scr:component/reference[4]/@cardinality");
		xt.assertAttribute("dynamic", "scr:component/reference[4]/@policy");
		xt.assertAttribute("greedy", "scr:component/reference[4]/@policy-option");

		xt.assertAttribute("E", "scr:component/reference[5]/@name");
		xt.assertAttribute("0..1", "scr:component/reference[5]/@cardinality");
		xt.assertAttribute("dynamic", "scr:component/reference[5]/@policy");
		xt.assertAttribute("reluctant", "scr:component/reference[5]/@policy-option");
	}

	/**
	 * Test the - for the unbind and updated parameter.
	 */
	@Component(name = "methods")
	public static class Methods {

		@Reference(unbind = "-", updated = "-")
		void setA(@SuppressWarnings("unused") LogService l) {}

		void updatedA(@SuppressWarnings("unused") LogService l) {}

		void unsetA(@SuppressWarnings("unused") LogService l) {}

		@Reference(unbind = "_B", updated = "__B")
		void setB(@SuppressWarnings("unused") LogService l) {}

		void _B(@SuppressWarnings("unused") LogService l) {}

		void __B(@SuppressWarnings("unused") LogService l) {}

		void updatedB(@SuppressWarnings("unused") LogService l) {}

		void unsetB(@SuppressWarnings("unused") LogService l) {}

		@Reference
		void setC(@SuppressWarnings("unused") LogService l) {}

		void updatedC(@SuppressWarnings("unused") LogService l) {}

		void unsetC(@SuppressWarnings("unused") LogService l) {}

		@Reference
		void setD(@SuppressWarnings("unused") LogService l) {}

	}

	public void testMethods() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*Methods");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.0.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, null, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/methods.xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.2.0");

		// use - to make sure no unbind and updated method is set
		xt.assertAttribute("A", "scr:component/reference[1]/@name");
		xt.assertAttribute("setA", "scr:component/reference[1]/@bind");
		xt.assertAttribute("", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("", "scr:component/reference[1]/@updated");

		// override the names for the methods
		xt.assertAttribute("B", "scr:component/reference[2]/@name");
		xt.assertAttribute("setB", "scr:component/reference[2]/@bind");
		xt.assertAttribute("_B", "scr:component/reference[2]/@unbind");
		xt.assertAttribute("__B", "scr:component/reference[2]/@updated");

		xt.assertAttribute("C", "scr:component/reference[3]/@name");
		xt.assertAttribute("setC", "scr:component/reference[3]/@bind");
		xt.assertAttribute("unsetC", "scr:component/reference[3]/@unbind");
		xt.assertAttribute("updatedC", "scr:component/reference[3]/@updated");

		xt.assertAttribute("D", "scr:component/reference[4]/@name");
		xt.assertAttribute("setD", "scr:component/reference[4]/@bind");
		xt.assertAttribute("", "scr:component/reference[4]/@unbind");
		xt.assertAttribute("", "scr:component/reference[4]/@updated");
	}

	/**
	 * Test inheritance (this is not official)
	 */

	public static class Top {

		@Reference
		void setLogService(@SuppressWarnings("unused") LogService l) {}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<?> ref) {

		}

		@Reference
		protected void setPrivateLogService(@SuppressWarnings("unused") LogService l) {

		}

		@SuppressWarnings("unused")
		private void updatedPrivateLogService(ServiceReference<?> ref) {

		}
	}

	@Component(name = "bottom")
	public static class Bottom extends Top {
		void unsetLogService(@SuppressWarnings("unused") LogService l,
			@SuppressWarnings("unused") Map<Object, Object> map) {

		}

		void unsetPrivateLogService(@SuppressWarnings("unused") ServiceReference<?> ref) {

		}
	}

	public void testInheritance() throws Exception {
		testInheritance("-dsannotations-inherit", "true", null);
	}

	public void testInheritanceFlag() throws Exception {
		testInheritance(Constants.DSANNOTATIONS_OPTIONS, "inherit", null);
	}

	public void testInheritanceExtenderFlag() throws Exception {
		testInheritance(Constants.DSANNOTATIONS_OPTIONS, "inherit,extender",
			ComponentConstants.COMPONENT_SPECIFICATION_VERSION);
	}

	public void testInheritance(String key, String value, String extender) throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*Bottom");
		b.setProperty(key, value);
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, extender, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/bottom.xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");

		xt.assertAttribute("LogService", "scr:component/reference[1]/@name");
		xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
		xt.assertAttribute("unsetLogService", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("updatedLogService", "scr:component/reference[1]/@updated");

		xt.assertAttribute("PrivateLogService", "scr:component/reference[2]/@name");
		xt.assertAttribute("setPrivateLogService", "scr:component/reference[2]/@bind");
		xt.assertAttribute("unsetPrivateLogService", "scr:component/reference[2]/@unbind");
		// is private in super class
		xt.assertAttribute("", "scr:component/reference[2]/@updated");

	}

	public void testBadFlag() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*Bottom");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "foo");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertEquals(1, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());
	}

	/**
	 * Test the different prototypes ...
	 */

	@Component(name = "prototypes")
	public static class Prototypes {
		@SuppressWarnings("unused")
		@Activate
		private void activate() {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext ctx) {

		}

		@Modified
		void modified(@SuppressWarnings("unused") BundleContext context) {

		}

		@SuppressWarnings("unused")
		@Reference
		private void setLogService(LogService l) {

		}

		protected void unsetLogService(@SuppressWarnings("unused") LogService l,
			@SuppressWarnings("unused") Map<Object, Object> map) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<?> ref) {

		}

	}

	public void testPrototypes() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*Prototypes");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.0.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, null, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/prototypes.xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.2.0");

		xt.assertAttribute("LogService", "scr:component/reference[1]/@name");
		xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
		xt.assertAttribute("unsetLogService", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("updatedLogService", "scr:component/reference[1]/@updated");

	}

	/**
	 * Test the different prototypes ...
	 */

	@Component(name = "prototypes")
	public static class CheckBinds {
		@SuppressWarnings("unused")
		@Activate
		private void activate() {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext ctx) {

		}

		@Modified
		void modified(@SuppressWarnings("unused") BundleContext context) {

		}

		@SuppressWarnings("unused")
		@Reference
		private void bindLogService(LogService l) {

		}

		protected void unbindLogService(@SuppressWarnings("unused") LogService l,
			@SuppressWarnings("unused") Map<Object, Object> map) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<?> ref) {

		}

	}

	public void testBinds() throws Exception {
		testBinds(null);
	}

	public void testBindsExtender() throws Exception {
		testBinds(ComponentConstants.COMPONENT_SPECIFICATION_VERSION);
	}

	public void testBinds(String extender) throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*CheckBinds");
		if (extender != null)
			b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "extender");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS + ".min", "version;minimum=1.2.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, extender, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/prototypes.xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.2.0");

		xt.assertAttribute("LogService", "scr:component/reference[1]/@name");
		xt.assertAttribute("bindLogService", "scr:component/reference[1]/@bind");
		xt.assertAttribute("unbindLogService", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("updatedLogService", "scr:component/reference[1]/@updated");

	}

	@Component
	public static class CheckBinds13 {

		@Reference
		private void bindService(LogService l) {}

		protected void unbindService(LogService l) {}

		void updatedService(LogService l) {}

		@Reference
		private void bindSR(ServiceReference<LogService> l, LogService l2) {}

		protected void unbindSR(ServiceReference<LogService> l, LogService l2) {}

		void updatedSR(ServiceReference<LogService> l, LogService l2) {}

		@Reference(service = LogService.class)
		private void bindProps(Map<String, Object> l) {}

		protected void unbindProps(Map<String, Object> l) {}

		void updatedProps(Map<String, Object> l) {}

		@Reference
		private void bindSO(ComponentServiceObjects<LogService> l) {}

		protected void unbindSO(ComponentServiceObjects<LogService> l) {}

		void updatedSO(ComponentServiceObjects<LogService> l) {}

		@Reference
		private void bindServiceSR(LogService l, ServiceReference<LogService> l2) {}

		protected void unbindServiceSR(ServiceReference<LogService> l2, LogService l) {}

		void updatedServiceSR(LogService l, ServiceReference<LogService> l2) {}

		@Reference
		private void bindPropsSR(ServiceReference<LogService> l, Map<String, Object> l2) {}

		protected void unbindPropsSR(ServiceReference<LogService> l, Map<String, Object> l2) {}

		void updatedPropsSR(ServiceReference<LogService> l, Map<String, Object> l2) {}

		@Reference
		private void bindPropsSO(ComponentServiceObjects<LogService> l2, Map<String, Object> l) {}

		protected void unbindPropsSO(ComponentServiceObjects<LogService> l2, Map<String, Object> l) {}

		void updatedPropsSO(ComponentServiceObjects<LogService> l, Map<String, Object> l2) {}

	}

	public void testBinds13() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*CheckBinds13");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/" + CheckBinds13.class.getName() + ".xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");

		int size = 7;
		xt.assertCount(size, "scr:component/reference");
		for (int i = 1; i <= size; i++) {
			xt.assertAttribute(LogService.class.getName(), "scr:component/reference[" + i + "]/@interface");
		}
	}

	@Component(name = "NoUnbindDynamic")
	public static class NoUnbindDynamic {
		@SuppressWarnings("unused")
		@Reference(policy = ReferencePolicy.DYNAMIC)
		private void bindLogService(LogService l) {}
	}

	public void testNoUnbindDynamic() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*NoUnbindDynamic");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertEquals(1, b.getErrors()
			.size());
		assertTrue(b.getErrors()
			.get(0)
			.endsWith("dynamic but has no unbind method."));
	}

	@Component(name = "testConfigPolicy", configurationPolicy = ConfigurationPolicy.IGNORE)
	public static class TestConfigPolicy {}

	public void testConfigPolicySetsNamespace() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*TestConfigPolicy");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.0.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, null);

		Resource r = jar.getResource("OSGI-INF/testConfigPolicy.xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.1.0");
		xt.assertNamespace("http://www.osgi.org/xmlns/scr/v1.1.0");
	}

	@Component()
	public static class issue347 implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		/*
		 * #347 - When using DS 1.2 annotations @Reference on private methods
		 * and no @Activate/@Deactivate annotations then bnd generates
		 * component.xml without namespace (1.0)
		 */

		@SuppressWarnings("unused")
		@Reference
		private void setLogService(LogService log) {}

		@SuppressWarnings("unused")
		private void unsetLogService(LogService log) {}

		@SuppressWarnings("unused")
		private void updatedLogService(LogService log) {}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	public void testIssue347() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*issue347");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.0.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a, SERIALIZABLE_RUNNABLE);
		checkRequires(a, null, LogService.class.getName());

		{
			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$issue347.xml");
			System.err.println(Processor.join(jar.getResources()
				.keySet(), "\n"));
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.2.0");
			// Test the defaults
			xt.assertAttribute("test.component.DSAnnotationTest$issue347", "scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("test.component.DSAnnotationTest$issue347", "scr:component/@name");

			xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
			xt.assertAttribute("unsetLogService", "scr:component/reference[1]/@unbind");
			xt.assertAttribute("updatedLogService", "scr:component/reference[1]/@updated");

		}
	}

	@Component(reference = {
		@Reference(name = "log", service = LogService.class, cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, target = "(service.id=1)"),
		@Reference(name = "logField", service = LogService.class, cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, target = "(service.id=1)", field = "logField", fieldOption = FieldOption.REPLACE),
		@Reference(name = "logMethod", service = LogService.class, cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, target = "(service.id=1)", bind = "setLogMethod", unbind = "unsetLogMethod", updated = "updatedLogMethod"),
	})
	public static class ref_on_comp implements Serializable, Runnable {
		private static final long	serialVersionUID	= 1L;

		@SuppressWarnings("unused")
		private List<LogService>	logField;

		protected void setLogMethod(LogService logService) {}

		protected void updatedLogMethod(LogService logService) {}

		protected void unsetLogMethod(LogService logService) {}

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Override
		public void run() {}
	}

	public void testReferenceInComponent() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*ref_on_comp");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a, SERIALIZABLE_RUNNABLE);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$ref_on_comp.xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute("test.component.DSAnnotationTest$ref_on_comp", "scr:component/implementation/@class");

		xt.assertAttribute("log", "scr:component/reference[1]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[1]/@interface");
		xt.assertAttribute("1..n", "scr:component/reference[1]/@cardinality");

		xt.assertAttribute("logField", "scr:component/reference[2]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[2]/@interface");
		xt.assertAttribute("1..n", "scr:component/reference[2]/@cardinality");
		xt.assertAttribute("replace", "scr:component/reference[2]/@field-option");
		// TODO field-component-type

		xt.assertAttribute("logMethod", "scr:component/reference[3]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[3]/@interface");
		xt.assertAttribute("1..1", "scr:component/reference[3]/@cardinality");
		xt.assertAttribute("setLogMethod", "scr:component/reference[3]/@bind");
		xt.assertAttribute("unsetLogMethod", "scr:component/reference[3]/@unbind");
		xt.assertAttribute("updatedLogMethod", "scr:component/reference[3]/@updated");

	}

	public @interface NoDefaults {
		String string();
	}

	/**
	 * Check that a DS 1.3 activate method causes a DS 1.3 namespace
	 */
	@Component()
	public static class DS13_activate_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") NoDefaults anno) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, int reason) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference(service = LogService.class)
		void setLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void unsetLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that a DS 1.3 deactivate method causes a DS 1.3 namespace
	 */
	@Component()
	public static class DS13_deactivate_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") NoDefaults anno,
			Integer reason) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference(service = LogService.class)
		void setLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void unsetLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that a DS 1.3 modified method causes a DS 1.3 namespace
	 */
	@Component()
	public static class DS13_modified_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") NoDefaults anno) {}

		@Reference(service = LogService.class)
		void setLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void unsetLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that a DS 1.3 bind method causes a DS 1.3 namespace
	 */
	@Component()
	public static class DS13_ref_bind_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference(service = LogService.class)
		void setLogService(@SuppressWarnings("unused") Map<String, Object> map) {

		}

		void unsetLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that a DS 1.3 unbind method causes a DS 1.3 namespace
	 */
	@Component()
	public static class DS13_ref_unbind_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference(service = LogService.class)
		void setLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void unsetLogService(@SuppressWarnings("unused") Map<String, Object> map) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that a DS 1.3 updated method causes a DS 1.3 namespace
	 */
	@Component()
	public static class DS13_ref_updated_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference(service = LogService.class)
		void setLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void unsetLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void updatedLogService(@SuppressWarnings("unused") Map<String, Object> map) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that a DS 1.3 prototype scope causes a DS 1.3 namespace
	 */
	@Component(scope = ServiceScope.PROTOTYPE)
	public static class DS13_scope_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference(service = LogService.class)
		void setLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void unsetLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that multiple configuration pids causes a DS 1.3 namespace
	 */
	@Component(configurationPid = {
		"pid1", "pid2"
	})
	public static class DS13_pids_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference(service = LogService.class)
		void setLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void unsetLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * Check that '$' placeholder is translated to component name and causes a
	 * DS 1.3 namespace
	 */
	@Component(configurationPid = {
		"$", "pid2"
	})
	public static class DS13_dollar_pids_basic implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference(service = LogService.class)
		void setLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void unsetLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		void updatedLogService(@SuppressWarnings("unused") ServiceReference<LogService> sr) {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	public void testBasic13() throws Exception {
		Jar jar = setupBasic13(null, LogService.class.getName(), SERIALIZABLE_RUNNABLE);

		// Test 1.3 signature methods give 1.3 namespace
		checkDS13(jar, "test.component.DSAnnotationTest$DS13_activate_basic", "", "");
		checkDS13(jar, "test.component.DSAnnotationTest$DS13_deactivate_basic", "", "");
		checkDS13(jar, "test.component.DSAnnotationTest$DS13_modified_basic", "", "");
		checkDS13(jar, "test.component.DSAnnotationTest$DS13_ref_bind_basic", "", "");
		checkDS13(jar, "test.component.DSAnnotationTest$DS13_ref_unbind_basic", "", "");
		checkDS13(jar, "test.component.DSAnnotationTest$DS13_ref_updated_basic", "", "");
		checkDS13(jar, "test.component.DSAnnotationTest$DS13_scope_basic", "", "prototype");
		checkDS13(jar, "test.component.DSAnnotationTest$DS13_pids_basic", "pid1 pid2", "");
		checkDS13(jar, "test.component.DSAnnotationTest$DS13_dollar_pids_basic",
			"test.component.DSAnnotationTest$DS13_dollar_pids_basic pid2", "");
		// suppress cap/req
		setupBasic13("nocapabilities", LogService.class.getName());
		setupBasic13("norequirements", null, SERIALIZABLE_RUNNABLE);
		setupBasic13("nocapabilities,norequirements", null);

	}

	private Jar setupBasic13(String options, String requires, String... provides) throws IOException, Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$DS13_*");
		if (options != null) {
			b.setProperty(Constants.DSANNOTATIONS_OPTIONS, options);
		}
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		if (provides == null || provides.length == 0) {
			checkProvides(a);
		} else {
			checkProvides(a, provides);
		}
		if (requires == null) {
			checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION);
		} else {
			checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, requires);
		}
		return jar;
	}

	private void checkDS13(Jar jar, String name, String pids, String scope) throws Exception, XPathExpressionException {
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertAttribute("", "scr:component/@configuration-policy");
		xt.assertAttribute("", "scr:component/@immediate");
		xt.assertAttribute("", "scr:component/@enabled");
		xt.assertAttribute("", "scr:component/@factory");
		xt.assertAttribute(scope, "scr:component/service/@scope");
		xt.assertAttribute(pids, "scr:component/@configuration-pid");
		xt.assertAttribute("activate", "scr:component/@activate");
		xt.assertAttribute("deactivate", "scr:component/@deactivate");
		xt.assertAttribute("modified", "scr:component/@modified");
		xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
		xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

		xt.assertAttribute("0", "count(scr:component/properties)");
		xt.assertAttribute("0", "count(scr:component/property)");

		xt.assertAttribute("LogService", "scr:component/reference[1]/@name");
		xt.assertAttribute("", "scr:component/reference[1]/@target");
		xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
		xt.assertAttribute("unsetLogService", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("updatedLogService", "scr:component/reference[1]/@updated");
		xt.assertAttribute("", "scr:component/reference[1]/@cardinality");
		xt.assertAttribute("", "scr:component/reference[1]/@policy");
		xt.assertAttribute("", "scr:component/reference[1]/@target");
		xt.assertAttribute("", "scr:component/reference[1]/@policy-option");
	}

	/**
	 * Check that factoryProperty and factoryProperies elements are processed.
	 */
	@Component(factory = "ds14", factoryProperty = {
		"x:Integer=3.0", "a=1", "a=2", "b=1", "boolean:Boolean=true", "byte:Byte=1", "char:Character=1",
		"short:Short=3", "integer:Integer=3", "long:Long=3", "float:Float=3.0", "double:Double=3e7", "string:String=%",
		"wrongInteger:Integer=blabla", "\n\r\t \u0343\u0344\u0345\u0346\n:Integer=3"
	}, factoryProperties = "factory.properties")
	public static class FactoryProperties {}

	public void testFactoryProperties() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$FactoryProperties");
			b.setProperty("Private-Package", "test.component");
			b.setProperty("-includeresource.factoryprops", "factory.properties;literal=\"\"");
			b.addClasspath(new File("bin_test"));

			Jar jar = b.build();
			if (!b.check("Not a valid number blabla for Integer", "Not a valid number 3.0 for Integer"))
				fail();
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, "1.4.0");

			//
			// Test all the defaults
			//

			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$FactoryProperties.xml");
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.4.0");
			xt.assertAttribute("0", "count(scr:component/properties)");
			xt.assertAttribute("0", "count(scr:component/property)");
			xt.assertAttribute("14", "count(scr:component/factory-property)");
			xt.assertAttribute("1", "count(scr:component/factory-properties)");

			xt.assertAttribute("", "scr:component/factory-property[@name='a']/@value");
			xt.assertAttribute("1\n2", "scr:component/factory-property[@name='a']/text()");
			xt.assertAttribute("1", "scr:component/factory-property[@name='b']/@value");
			xt.assertAttribute("Byte", "scr:component/factory-property[@name='byte']/@type");
			xt.assertAttribute("Boolean", "scr:component/factory-property[@name='boolean']/@type");
			xt.assertAttribute("Character", "scr:component/factory-property[@name='char']/@type");
			xt.assertAttribute(Integer.toString('1'), "scr:component/factory-property[@name='char']/@value");
			xt.assertAttribute("Short", "scr:component/factory-property[@name='short']/@type");
			xt.assertAttribute("Integer", "scr:component/factory-property[@name='integer']/@type");
			xt.assertAttribute("Long", "scr:component/factory-property[@name='long']/@type");
			xt.assertAttribute("Float", "scr:component/factory-property[@name='float']/@type");
			xt.assertAttribute("Double", "scr:component/factory-property[@name='double']/@type");
			xt.assertAttribute("Integer", "scr:component/factory-property[@name='\u0343\u0344\u0345\u0346']/@type");

			xt.assertAttribute("factory.properties", "scr:component/factory-properties[1]/@entry");
		}
	}

	public void testNoHeaderDups() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$DS13_*");
		b.setProperty("Private-Package", "test.component");
		b.setProperty("Provide-Capability",
			"osgi.service;objectClass:List<String>=\"java.io.Serializable,java.lang.Runnable\"");
		b.setProperty("Require-Capability",
			"osgi.service;filter:=\"(objectClass=org.osgi.service.log.LogService)\";effective:=active");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a, SERIALIZABLE_RUNNABLE);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, LogService.class.getName());
	}

	public enum Foo {
		A,
		B
	}

	public @interface ConfigTypes {
		String myString() default "foo";

		String[] myStringArray() default {
			"foo", "bar"
		};

		String[] mySingleStringArray() default "baz";

		int myInt() default 1;

		int[] myIntArray() default {
			2, 3
		};

		int[] mySingleIntArray() default 4;

		Class<?> myClass() default ConfigTypes.class;

		Class<?>[] myClassArray() default {
			ConfigTypes.class, ConfigTypes.class
		};

		Class<?>[] mySingleClassArray() default ConfigTypes.class;

		Foo myEnum() default Foo.A;

		Foo[] myEnumArray() default {
			Foo.A, Foo.B
		};

		Foo[] mySingleEnumArray() default Foo.B;

		float myFloat() default 1.0f;

		char myChar() default 'a';

		byte myByte() default 2;

		short myShort() default 298;

		double myDouble() default 2.1D;

		long myLong() default 9876543210L;
	}

	@Component()
	public static class DS13anno_configTypes_activate implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc,
			@SuppressWarnings("unused") ConfigTypes config1) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component()
	public static class DS13anno_configTypes_modified implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc,
			@SuppressWarnings("unused") ConfigTypes config1) {}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component()
	public static class DS13anno_configTypes_deactivate implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc,
			@SuppressWarnings("unused") ConfigTypes config1) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	public void testAnnoConfig13() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$DS13anno_*");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a, SERIALIZABLE_RUNNABLE);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION);

		// // Test 1.3 signature methods give 1.3 namespace
		checkDS13Anno(jar, DS13anno_configTypes_activate.class.getName(), "");
		checkDS13Anno(jar, DS13anno_configTypes_deactivate.class.getName(), "");
		checkDS13Anno(jar, DS13anno_configTypes_modified.class.getName(), "");
	}

	private void checkDS13Anno(Jar jar, String name, String pids) throws Exception, XPathExpressionException {
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertAttribute("", "scr:component/@configuration-policy");
		xt.assertAttribute("", "scr:component/@immediate");
		xt.assertAttribute("", "scr:component/@enabled");
		xt.assertAttribute("", "scr:component/@factory");
		xt.assertAttribute("", "scr:component/service/@scope");
		xt.assertAttribute(pids, "scr:component/@configuration-pid");
		xt.assertAttribute("activate", "scr:component/@activate");
		xt.assertAttribute("deactivate", "scr:component/@deactivate");
		xt.assertAttribute("modified", "scr:component/@modified");
		xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
		xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

		xt.assertAttribute("0", "count(scr:component/properties)");
		xt.assertAttribute("18", "count(scr:component/property)");

		xt.assertAttribute("foo", "scr:component/property[@name='myString']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='myString']/@type");

		xt.assertTrimmedAttribute("foo\\nbar", "scr:component/property[@name='myStringArray']");
		xt.assertAttribute("String", "scr:component/property[@name='myStringArray']/@type");

		xt.assertTrimmedAttribute("baz", "scr:component/property[@name='mySingleStringArray']");
		xt.assertAttribute("String", "scr:component/property[@name='mySingleStringArray']/@type");

		xt.assertAttribute("1", "scr:component/property[@name='myInt']/@value");
		xt.assertAttribute("Integer", "scr:component/property[@name='myInt']/@type");

		xt.assertTrimmedAttribute("2\\n3", "scr:component/property[@name='myIntArray']");
		xt.assertAttribute("Integer", "scr:component/property[@name='myIntArray']/@type");

		xt.assertTrimmedAttribute("4", "scr:component/property[@name='mySingleIntArray']");
		xt.assertAttribute("Integer", "scr:component/property[@name='mySingleIntArray']/@type");

		xt.assertAttribute("test.component.DSAnnotationTest$ConfigTypes",
			"scr:component/property[@name='myClass']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='myClass']/@type");

		xt.assertTrimmedAttribute(
			"test.component.DSAnnotationTest$ConfigTypes\\ntest.component.DSAnnotationTest$ConfigTypes",
			"scr:component/property[@name='myClassArray']");
		xt.assertAttribute("String", "scr:component/property[@name='myClassArray']/@type");

		xt.assertTrimmedAttribute("test.component.DSAnnotationTest$ConfigTypes",
			"scr:component/property[@name='mySingleClassArray']");
		xt.assertAttribute("String", "scr:component/property[@name='mySingleClassArray']/@type");

		xt.assertAttribute("A", "scr:component/property[@name='myEnum']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='myEnum']/@type");

		xt.assertTrimmedAttribute("A\\nB", "scr:component/property[@name='myEnumArray']");
		xt.assertAttribute("String", "scr:component/property[@name='myEnumArray']/@type");

		xt.assertTrimmedAttribute("B", "scr:component/property[@name='mySingleEnumArray']");
		xt.assertAttribute("String", "scr:component/property[@name='mySingleEnumArray']/@type");

		xt.assertAttribute("1.0", "scr:component/property[@name='myFloat']/@value");
		xt.assertAttribute("Float", "scr:component/property[@name='myFloat']/@type");

		xt.assertAttribute(Integer.toString('a'), "scr:component/property[@name='myChar']/@value");
		xt.assertAttribute("Character", "scr:component/property[@name='myChar']/@type");

		xt.assertAttribute("2", "scr:component/property[@name='myByte']/@value");
		xt.assertAttribute("Byte", "scr:component/property[@name='myByte']/@type");

		xt.assertAttribute("298", "scr:component/property[@name='myShort']/@value");
		xt.assertAttribute("Short", "scr:component/property[@name='myShort']/@type");

		xt.assertAttribute("9876543210", "scr:component/property[@name='myLong']/@value");
		xt.assertAttribute("Long", "scr:component/property[@name='myLong']/@type");

		xt.assertAttribute("2.1", "scr:component/property[@name='myDouble']/@value");
		xt.assertAttribute("Double", "scr:component/property[@name='myDouble']/@type");

	}

	public @interface ConfigNames {
		String myString1() default "foo";

		String my$String2() default "foo";

		String my$$String3() default "foo";

		String my_String4() default "foo";

		String my__String5() default "foo";

		String my$$__String6() default "foo";
	}

	@Component()
	public static class DS13annoNames_config implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc,
			@SuppressWarnings("unused") ConfigNames configNames) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Override
		public void run() {}
	}

	public void testAnnoConfigNames13() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$DS13annoNames_config*");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a, SERIALIZABLE_RUNNABLE);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION);

		checkDS13AnnoConfigNames(jar, DS13annoNames_config.class.getName());
	}

	private void checkDS13AnnoConfigNames(Jar jar, String name) throws Exception, XPathExpressionException {
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertAttribute("", "scr:component/@configuration-policy");
		xt.assertAttribute("", "scr:component/@immediate");
		xt.assertAttribute("", "scr:component/@enabled");
		xt.assertAttribute("", "scr:component/@factory");
		xt.assertAttribute("", "scr:component/service/@scope");
		xt.assertAttribute("", "scr:component/@configuration-pid");
		xt.assertAttribute("activate", "scr:component/@activate");
		xt.assertAttribute("deactivate", "scr:component/@deactivate");
		xt.assertAttribute("modified", "scr:component/@modified");
		xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
		xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

		xt.assertAttribute("6", "count(scr:component/property)");

		xt.assertAttribute("foo", "scr:component/property[@name='myString1']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='myString1']/@type");

		xt.assertAttribute("foo", "scr:component/property[@name='myString2']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='myString2']/@type");

		xt.assertAttribute("foo", "scr:component/property[@name='my$String3']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='my$String3']/@type");

		xt.assertAttribute("foo", "scr:component/property[@name='my.String4']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='my.String4']/@type");

		xt.assertAttribute("foo", "scr:component/property[@name='my_String5']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='my_String5']/@type");

		xt.assertAttribute("foo", "scr:component/property[@name='my$_String6']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='my$_String6']/@type");
	}

	public @interface ConfigNames14 {
		String PREFIX_ = "test.";

		String my$_$String7() default "foo";

		String value() default "foo";
	}

	@Component()
	public static class DS14annoNames_config implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc,
			@SuppressWarnings("unused") ConfigNames14 configNames) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Override
		public void run() {}
	}

	public void testAnnoConfigNames14() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$DS14annoNames_config*");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a, SERIALIZABLE_RUNNABLE);
		checkRequires(a, "1.4.0");

		checkDS14AnnoConfigNames(jar, DS14annoNames_config.class.getName());
	}

	private void checkDS14AnnoConfigNames(Jar jar, String name) throws Exception, XPathExpressionException {
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.4.0");
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertAttribute("", "scr:component/@configuration-policy");
		xt.assertAttribute("", "scr:component/@immediate");
		xt.assertAttribute("", "scr:component/@enabled");
		xt.assertAttribute("", "scr:component/@factory");
		xt.assertAttribute("", "scr:component/service/@scope");
		xt.assertAttribute("", "scr:component/@configuration-pid");
		xt.assertAttribute("activate", "scr:component/@activate");
		xt.assertAttribute("deactivate", "scr:component/@deactivate");
		xt.assertAttribute("modified", "scr:component/@modified");
		xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
		xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

		xt.assertAttribute("2", "count(scr:component/property)");

		xt.assertAttribute("foo", "scr:component/property[@name='test.my-String7']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='test.my-String7']/@type");

		xt.assertAttribute("foo", "scr:component/property[@name='test.config.names14']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='test.config.names14']/@type");

	}

	@Component
	public static class DS14_activation_objects implements Serializable, Runnable {
		private static final long	serialVersionUID	= 1L;

		@Activate
		private ComponentContext	cc;
		@Activate
		private BundleContext		bc;
		@Activate
		private Map<String, Object>	props;
		@Activate
		private ConfigNames14		configNames;

		@Override
		public void run() {}
	}

	public void testActivationObjects14() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$DS14_activation_objects*");
			b.setProperty("Private-Package", "test.component");
			b.addClasspath(new File("bin_test"));

			Jar jar = b.build();
			assertOk(b);
			Attributes a = getAttr(jar);
			checkProvides(a, SERIALIZABLE_RUNNABLE);
			checkRequires(a, "1.4.0");
			checkDS14ActivationObjects(jar, DS14_activation_objects.class.getName());

		}
	}

	private void checkDS14ActivationObjects(Jar jar, String name) throws Exception, XPathExpressionException {
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.4.0");
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertAttribute("", "scr:component/@configuration-policy");
		xt.assertAttribute("", "scr:component/@immediate");
		xt.assertAttribute("", "scr:component/@enabled");
		xt.assertAttribute("", "scr:component/@factory");
		xt.assertAttribute("", "scr:component/service/@scope");
		xt.assertAttribute("", "scr:component/@configuration-pid");
		xt.assertAttribute("cc bc props configNames", "scr:component/@activation-fields");
		xt.assertAttribute("", "scr:component/@activate");
		xt.assertAttribute("", "scr:component/@deactivate");
		xt.assertAttribute("", "scr:component/@modified");
		xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
		xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

		xt.assertAttribute("2", "count(scr:component/property)");

		xt.assertAttribute("foo", "scr:component/property[@name='test.my-String7']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='test.my-String7']/@type");

		xt.assertAttribute("foo", "scr:component/property[@name='test.config.names14']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='test.config.names14']/@type");

	}

	public @interface ConfigA {
		String a() default "a";

		String one() default "a";

		String two() default "a";
	}

	public @interface ConfigB {
		String b() default "b";

		String one() default "b";
	}

	@Component(property = {
		"two:String=c"
	})
	public static class DS13annoOverride_a_a implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigA a,
			@SuppressWarnings("unused") ConfigB b) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Override
		public void run() {}
	}

	@Component(property = {
		"two:String=c"
	})
	public static class DS13annoOverride_a_d implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigB b) {}

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigA a) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Override
		public void run() {}
	}

	@Component(property = {
		"two:String=c"
	})
	public static class DS13annoOverride_a_m implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigB b) {}

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigA a) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Override
		public void run() {}
	}

	@Component(property = {
		"two:String=c"
	})
	public static class DS13annoOverride_d_m implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigB b) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigA a) {}

		@Override
		public void run() {}
	}

	public void testAnnoConfigOverrides13() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$DS13annoOverride_*");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a, SERIALIZABLE_RUNNABLE);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION);

		checkDS13AnnoOverride(jar, DS13annoOverride_a_a.class.getName());
		checkDS13AnnoOverride(jar, DS13annoOverride_a_d.class.getName());
		checkDS13AnnoOverride(jar, DS13annoOverride_a_m.class.getName());
		checkDS13AnnoOverride(jar, DS13annoOverride_d_m.class.getName());
	}

	private void checkDS13AnnoOverride(Jar jar, String name) throws Exception, XPathExpressionException {
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertAttribute("", "scr:component/@configuration-policy");
		xt.assertAttribute("", "scr:component/@immediate");
		xt.assertAttribute("", "scr:component/@enabled");
		xt.assertAttribute("", "scr:component/@factory");
		xt.assertAttribute("", "scr:component/service/@scope");
		xt.assertAttribute("", "scr:component/@configuration-pid");
		xt.assertAttribute("activate", "scr:component/@activate");
		xt.assertAttribute("deactivate", "scr:component/@deactivate");
		xt.assertAttribute("modified", "scr:component/@modified");
		xt.assertAttribute("java.io.Serializable", "scr:component/service/provide[1]/@interface");
		xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

		xt.assertAttribute("4", "count(scr:component/property)");

		xt.assertAttribute("a", "scr:component/property[@name='a']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='a']/@type");

		xt.assertAttribute("b", "scr:component/property[@name='b']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='b']/@type");

		xt.assertAttribute("b", "scr:component/property[@name='one']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='one']/@type");

		xt.assertAttribute("c", "scr:component/property[@name='two']/@value");
		xt.assertAttribute("String", "scr:component/property[@name='two']/@type");
	}

	@Component
	public static class TestFieldInjection {
		@Reference
		private LogService												serviceField;

		@Reference
		private ServiceReference<LogService>							srField;

		@Reference
		private ComponentServiceObjects<LogService>						soField;

		@Reference(service = LogService.class)
		private Map<String, Object>										propsField;

		@Reference
		private Map.Entry<Map<String, Object>, LogService>				tupleField;

		@Reference
		private Map.Entry<Map<String, ?>, LogService>					tupleField2;

		@Reference
		private Map.Entry<Map<String, ? extends Object>, LogService>	tupleField3;

	}

	public void testFieldInjection() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest*TestFieldInjection");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkProvides(a);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/" + TestFieldInjection.class.getName() + ".xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		xt.assertNamespace("http://www.osgi.org/xmlns/scr/v1.3.0");

		xt.assertAttribute("propsField", "scr:component/reference[1]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[1]/@interface");
		xt.assertAttribute("propsField", "scr:component/reference[1]/@field");

		xt.assertAttribute("serviceField", "scr:component/reference[2]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[2]/@interface");
		xt.assertAttribute("serviceField", "scr:component/reference[2]/@field");

		xt.assertAttribute("soField", "scr:component/reference[3]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[3]/@interface");
		xt.assertAttribute("soField", "scr:component/reference[3]/@field");

		xt.assertAttribute("srField", "scr:component/reference[4]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[4]/@interface");
		xt.assertAttribute("srField", "scr:component/reference[4]/@field");

		xt.assertAttribute("tupleField", "scr:component/reference[5]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[5]/@interface");
		xt.assertAttribute("tupleField", "scr:component/reference[5]/@field");

		xt.assertAttribute("tupleField2", "scr:component/reference[6]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[6]/@interface");
		xt.assertAttribute("tupleField2", "scr:component/reference[6]/@field");

		xt.assertAttribute("tupleField3", "scr:component/reference[7]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[7]/@interface");
		xt.assertAttribute("tupleField3", "scr:component/reference[7]/@field");
	}

	@Component
	public static class TestFieldCollectionType implements Serializable, Runnable {
		private static final long										serialVersionUID	= 1L;

		@Reference
		// (service = LogService.class)
		private Collection<ServiceReference<LogService>>				srField;

		@Reference
		// (service=LogService.class)
		private Collection<ComponentServiceObjects<LogService>>			soField;

		@Reference(service = LogService.class)
		private Collection<Map<String, Object>>							propsField;

		@Reference
		// (service=LogService.class)
		private Collection<LogService>									serviceField;

		@Reference
		// (service = LogService.class)
		private Collection<Map.Entry<Map<String, Object>, LogService>>	tupleField;

		@Reference(service = Map.class, collectionType = CollectionType.SERVICE)
		private Collection<Map<String, Object>>							mapSvc;

		@Reference(policy = ReferencePolicy.DYNAMIC, fieldOption = FieldOption.UPDATE)
		private CopyOnWriteArrayList<LogService>						updateField			= new CopyOnWriteArrayList<>();

		@Override
		public void run() {}
	}

	public void testFieldCollectionType() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$TestFieldCollectionType");
			b.setProperty("Private-Package", "test.component");
			b.addClasspath(new File("bin_test"));

			Jar jar = b.build();
			assertOk(b);
			Attributes a = getAttr(jar);
			checkProvides(a, SERIALIZABLE_RUNNABLE);
			checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, LogService.class.getName(),
				Map.class.getName());

			Resource r = jar.getResource("OSGI-INF/" + TestFieldCollectionType.class.getName() + ".xml");
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
			xt.assertNamespace("http://www.osgi.org/xmlns/scr/v1.3.0");

			xt.assertAttribute(LogService.class.getName(), "scr:component/reference[@name='propsField']/@interface");
			xt.assertAttribute("propsField", "scr:component/reference[@name='propsField']/@field");
			xt.assertAttribute("properties", "scr:component/reference[@name='propsField']/@field-collection-type");

			xt.assertAttribute(LogService.class.getName(), "scr:component/reference[@name='serviceField']/@interface");
			xt.assertAttribute("serviceField", "scr:component/reference[@name='serviceField']/@field");
			xt.assertAttribute("service", "scr:component/reference[@name='serviceField']/@field-collection-type");

			xt.assertAttribute(LogService.class.getName(), "scr:component/reference[@name='soField']/@interface");
			xt.assertAttribute("soField", "scr:component/reference[@name='soField']/@field");
			xt.assertAttribute("serviceobjects", "scr:component/reference[@name='soField']/@field-collection-type");

			xt.assertAttribute(LogService.class.getName(), "scr:component/reference[@name='srField']/@interface");
			xt.assertAttribute("srField", "scr:component/reference[@name='srField']/@field");
			xt.assertAttribute("reference", "scr:component/reference[@name='srField']/@field-collection-type");

			xt.assertAttribute(LogService.class.getName(), "scr:component/reference[@name='tupleField']/@interface");
			xt.assertAttribute("tupleField", "scr:component/reference[@name='tupleField']/@field");
			xt.assertAttribute("tuple", "scr:component/reference[@name='tupleField']/@field-collection-type");

			xt.assertAttribute(Map.class.getName(), "scr:component/reference[@name='mapSvc']/@interface");
			xt.assertAttribute("mapSvc", "scr:component/reference[@name='mapSvc']/@field");
			xt.assertAttribute("service", "scr:component/reference[@name='mapSvc']/@field-collection-type");

			xt.assertAttribute(LogService.class.getName(), "scr:component/reference[@name='updateField']/@interface");
			xt.assertAttribute("updateField", "scr:component/reference[@name='updateField']/@field");
			xt.assertAttribute("update", "scr:component/reference[@name='updateField']/@field-option");
			xt.assertAttribute("service", "scr:component/reference[@name='updateField']/@field-collection-type");
		}
	}

	private void checkProvides(Attributes a, String[]... objectClass) {
		String p = a.getValue(Constants.PROVIDE_CAPABILITY);
		System.err.println(Constants.PROVIDE_CAPABILITY + ":" + p);
		Parameters header = new Parameters(p);
		List<Attrs> attrs = getAll(header, "osgi.service");
		assertEquals(objectClass.length, attrs.size());
		for (String[] o : objectClass) {
			boolean found = false;
			for (Attrs at : attrs) {
				if (Arrays.asList(o)
					.equals(at.getTyped("objectClass"))) {
					assertEquals(1, at.size());
					found = true;
				}
			}
			assertTrue("objectClass not found: " + o, found);
		}
	}

	private void checkRequires(Attributes a, String extender, String... objectClass) {
		String p = a.getValue(Constants.REQUIRE_CAPABILITY);
		System.err.println(Constants.REQUIRE_CAPABILITY + ":" + p);
		Parameters header = new Parameters(p);
		List<Attrs> attrs = getAll(header, "osgi.service");
		assertEquals("osgi.service attributes: " + attrs, objectClass.length, attrs.size());
		for (String o : objectClass) {
			boolean found = false;
			for (Attrs at : attrs) {
				if (("(objectClass=" + o + ")").equals(at.get("filter:"))) {
					assertEquals("no effective:=\"active\"", "active", at.get("effective:"));
					found = true;
				}
			}
			assertTrue("objectClass not found: " + o, found);
		}

		checkExtenderVersion(header, extender);
	}

	private void checkExtenderVersion(Parameters header, String extender) {
		if (extender != null) {
			Attrs attr = header.get("osgi.extender");
			assertThat(attr).as("osgi.extender namespace")
				.isNotNull();
			String filterString = attr.get("filter:");
			assertThat(filterString).as("osgi.extender namespace filter directive")
				.isNotNull();
			Filter filter = new Filter(filterString);
			Map<String, Object> map = new HashMap<>();
			map.put("osgi.extender", "osgi.component");
			map.put("version", Version.parseVersion(extender));
			boolean matches = false;
			try {
				matches = filter.matchMap(map);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			assertThat(matches).as("filter %s matches version %s", filter, extender)
				.isTrue();
		}
	}

	private Attributes getAttr(Jar jar) throws Exception {
		Manifest m = jar.getManifest();
		return m.getMainAttributes();
	}

	private List<Attrs> getAll(Parameters p, String key) {
		List<Attrs> l = new ArrayList<>();
		for (; p.containsKey(key); key += aQute.bnd.osgi.Constants.DUPLICATE_MARKER) {
			l.add(p.get(key));
		}
		return l;
	}

	@Component
	public static class DesignateNone {
		@Activate
		void activate(Map<String, Object> props) {}
	}

	@ObjectClassDefinition
	@interface config {}

	@Component
	@Designate(ocd = config.class)
	public static class DesignateSingleton {
		@Activate
		void activate(Map<String, Object> props) {}
	}

	@Component
	@Designate(ocd = config.class, factory = true)
	public static class DesignateFactory {
		@Activate
		void activate(Map<String, Object> props) {}
	}

	@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
	public static class DesignateNoneRequire {
		@Activate
		void activate(Map<String, Object> props) {}
	}

	@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
	@Designate(ocd = config.class)
	public static class DesignateSingletonRequire {
		@Activate
		void activate(Map<String, Object> props) {}
	}

	@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL)
	@Designate(ocd = config.class, factory = true)
	public static class DesignateFactoryOptional {
		@Activate
		void activate(Map<String, Object> props) {}
	}

	public void testDesignate() throws Exception {
		Builder b = new Builder();
		b.setProperty("-dsannotations", "test.component.DSAnnotationTest*Designate*");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.0.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);

		checkConfigurationPolicy(jar, DesignateNone.class, "");
		checkConfigurationPolicy(jar, DesignateSingleton.class, "");
		checkConfigurationPolicy(jar, DesignateFactory.class, "require");
		checkConfigurationPolicy(jar, DesignateNoneRequire.class, "require");
		checkConfigurationPolicy(jar, DesignateSingletonRequire.class, "require");
		checkConfigurationPolicy(jar, DesignateFactoryOptional.class, "optional");
	}

	void checkConfigurationPolicy(Jar jar, Class<?> clazz, String option) throws Exception, XPathExpressionException {
		Resource r = jar.getResource("OSGI-INF/" + clazz.getName() + ".xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.1.0");
		xt.assertNamespace("http://www.osgi.org/xmlns/scr/v1.1.0");

		xt.assertAttribute(option, "scr:component/@configuration-policy");
	}

	@XMLAttribute(namespace = "org.foo.extensions.v1", prefix = "foo", embedIn = "*")
	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.TYPE
	})
	@interface TestExtensions {
		boolean booleanAttr() default true;

		String stringAttr();

		Foo fooAttr();
	}

	@XMLAttribute(namespace = "org.foo.extensions.v1", prefix = "foo", embedIn = "http://www.osgi.org/xmlns/scr/*")
	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.FIELD, ElementType.METHOD
	})
	@interface TestRefExtensions {
		boolean booleanAttr2() default true;

		String stringAttr2();

		Foo fooAttr2();
	}

	@TestExtensions(stringAttr = "bar", fooAttr = Foo.A)
	@Component
	public static class ExtraAttributes implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigA a,
			@SuppressWarnings("unused") ConfigB b) {}

		@TestRefExtensions(stringAttr2 = "ignore", fooAttr2 = Foo.A)
		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@TestRefExtensions(stringAttr2 = "baz", fooAttr2 = Foo.B, booleanAttr2 = false)
		@Reference
		protected LogService lsa;

		@TestRefExtensions(stringAttr2 = "bax", fooAttr2 = Foo.A)
		@Reference
		protected void setLogServiceB(LogService ls) {}

		@Reference
		protected void setLogServicec(LogService ls) {}

		@TestRefExtensions(stringAttr2 = "ignore", fooAttr2 = Foo.A)
		@Override
		public void run() {}
	}

	public void testExtraAttributes() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$ExtraAttributes*");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);

		String name = ExtraAttributes.class.getName();
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0", "foo",
			"org.foo.extensions.v1");
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertCount(7, "scr:component/@*");
		xt.assertAttribute("bar", "scr:component/@foo:stringAttr");
		xt.assertAttribute("A", "scr:component/@foo:fooAttr");
		xt.assertAttribute("true", "scr:component/@foo:booleanAttr");

		xt.assertCount(3, "scr:component/reference");
		xt.assertCount(6, "scr:component/reference[1]/@*");
		xt.assertAttribute("bax", "scr:component/reference[1]/@foo:stringAttr2");
		xt.assertAttribute("A", "scr:component/reference[1]/@foo:fooAttr2");
		xt.assertAttribute("true", "scr:component/reference[1]/@foo:booleanAttr2");

		xt.assertCount(3, "scr:component/reference[2]/@*");
		xt.assertCount(6, "scr:component/reference[3]/@*");
		xt.assertAttribute("baz", "scr:component/reference[3]/@foo:stringAttr2");
		xt.assertAttribute("B", "scr:component/reference[3]/@foo:fooAttr2");
		xt.assertAttribute("false", "scr:component/reference[3]/@foo:booleanAttr2");
	}

	@TestExtensions(stringAttr = "bar", fooAttr = Foo.A)
	@Component
	public static class ExtraAttributes10 {
		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@TestRefExtensions(stringAttr2 = "ignore", fooAttr2 = Foo.A)
		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

	}

	/**
	 * test that an extension attribute will ensure at least a 1.1 namespace, so
	 * the extension namespace is meaningful
	 *
	 * @throws Exception
	 */
	public void testExtraAttributes10() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$ExtraAttributes*");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.0.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);

		String name = ExtraAttributes10.class.getName();
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.1.0", "foo",
			"org.foo.extensions.v1");
		// This wll check that the spec namespace is 1.1
		xt.assertAttribute(name, "scr:component/implementation/@class");

	}

	@XMLAttribute(namespace = "org.foo.extensions.v2", prefix = "foo")
	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.TYPE
	})
	@interface TestExtensions3 {
		boolean booleanAttr3() default false;

		String stringAttr3();

		Foo fooAttr3();
	}

	@TestExtensions(stringAttr = "bar", fooAttr = Foo.A)
	@TestExtensions3(stringAttr3 = "bar3", fooAttr3 = Foo.B)
	@Component
	public static class PrefixCollisionExtraAttributes implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigA a,
			@SuppressWarnings("unused") ConfigB b) {}

		@TestRefExtensions(stringAttr2 = "ignore", fooAttr2 = Foo.A)
		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@TestRefExtensions(stringAttr2 = "baz", fooAttr2 = Foo.B, booleanAttr2 = false)
		@Reference
		protected LogService lsa;

		@TestRefExtensions(stringAttr2 = "bax", fooAttr2 = Foo.A)
		@Reference
		protected void setLogServiceB(LogService ls) {}

		@Reference
		protected void setLogServicec(LogService ls) {}

		@TestRefExtensions(stringAttr2 = "ignore", fooAttr2 = Foo.A)
		@Override
		public void run() {}
	}

	public void testPrefixCollisionExtraAttributes() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$PrefixCollisionExtraAttributes*");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);

		String name = PrefixCollisionExtraAttributes.class.getName();
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0", "foo",
			"org.foo.extensions.v1", "foo1", "org.foo.extensions.v2");
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertCount(10, "scr:component/@*");
		xt.assertAttribute("bar", "scr:component/@foo:stringAttr");
		xt.assertAttribute("A", "scr:component/@foo:fooAttr");
		xt.assertAttribute("true", "scr:component/@foo:booleanAttr");
		xt.assertAttribute("bar3", "scr:component/@foo1:stringAttr3");
		xt.assertAttribute("B", "scr:component/@foo1:fooAttr3");
		xt.assertAttribute("false", "scr:component/@foo1:booleanAttr3");

		xt.assertCount(3, "scr:component/reference");
		xt.assertCount(6, "scr:component/reference[1]/@*");
		xt.assertAttribute("bax", "scr:component/reference[1]/@foo:stringAttr2");
		xt.assertAttribute("A", "scr:component/reference[1]/@foo:fooAttr2");
		xt.assertAttribute("true", "scr:component/reference[1]/@foo:booleanAttr2");

		xt.assertCount(3, "scr:component/reference[2]/@*");
		xt.assertCount(6, "scr:component/reference[3]/@*");
		xt.assertAttribute("baz", "scr:component/reference[3]/@foo:stringAttr2");
		xt.assertAttribute("B", "scr:component/reference[3]/@foo:fooAttr2");
		xt.assertAttribute("false", "scr:component/reference[3]/@foo:booleanAttr2");
	}

	@XMLAttribute(namespace = "org.foo.extensions.v4")
	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.TYPE
	})
	@interface TestExtensions4 {
		boolean booleanAttr4() default true;

		String stringAttr4();

		Foo fooAttr4();
	}

	@XMLAttribute(namespace = "org.foo.extensions.v5")
	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.TYPE
	})
	@interface TestExtensions5 {
		boolean booleanAttr5() default true;

		String stringAttr5();

		Foo fooAttr5();
	}

	@TestExtensions4(stringAttr4 = "bar", fooAttr4 = Foo.A)
	@TestExtensions5(stringAttr5 = "bar3", fooAttr5 = Foo.B)
	@Component
	public static class DefaultPrefixCollisionExtraAttributes implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;

		@Activate
		void activate(@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") ConfigA a,
			@SuppressWarnings("unused") ConfigB b) {}

		@TestRefExtensions(stringAttr2 = "ignore", fooAttr2 = Foo.A)
		@Deactivate
		void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Modified
		void modified(@SuppressWarnings("unused") ComponentContext cc) {}

		@Override
		public void run() {}
	}

	public void testPrefixCollisionExtraAttributesDefaultPrefix() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS,
			"test.component.DSAnnotationTest$DefaultPrefixCollisionExtraAttributes*");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);

		String name = DefaultPrefixCollisionExtraAttributes.class.getName();
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0", "ns",
			"org.foo.extensions.v4", "ns1", "org.foo.extensions.v5");
		// Test the defaults
		xt.assertAttribute(name, "scr:component/implementation/@class");

		// Default must be the implementation class
		xt.assertAttribute(name, "scr:component/@name");

		xt.assertCount(10, "scr:component/@*");
		xt.assertAttribute("bar", "scr:component/@ns:stringAttr4");
		xt.assertAttribute("A", "scr:component/@ns:fooAttr4");
		xt.assertAttribute("true", "scr:component/@ns:booleanAttr4");
		xt.assertAttribute("bar3", "scr:component/@ns1:stringAttr5");
		xt.assertAttribute("B", "scr:component/@ns1:fooAttr5");
		xt.assertAttribute("true", "scr:component/@ns1:booleanAttr5");

	}

	@Component(name = "mixed-std-bnd")
	static class MixedStdBnd {

		@aQute.bnd.annotation.component.Reference
		@SuppressWarnings("deprecation")
		protected void setLog(@SuppressWarnings("unused") LogService log) {}

		@aQute.bnd.annotation.component.Activate
		@SuppressWarnings("deprecation")
		void start() {}

		@aQute.bnd.annotation.component.Modified
		@SuppressWarnings("deprecation")
		void update(Map<String, Object> map) {}

		@aQute.bnd.annotation.component.Deactivate
		@SuppressWarnings("deprecation")
		void stop() {}
	}

	public void testMixedStandardBnd() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$MixedStdBnd");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));
		Jar build = b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(4, b.getErrors()
			.size());
		List<String> errors = new ArrayList<>(b.getErrors());
		Collections.sort(errors);
		assertEquals(
			"The DS component mixed-std-bnd uses standard annotations to declare it as a component, but also uses the bnd DS annotation: aQute.bnd.annotation.component.Activate on method start with signature ()V. It is an error to mix these two types of annotations",
			errors.get(0));
		assertEquals(
			"The DS component mixed-std-bnd uses standard annotations to declare it as a component, but also uses the bnd DS annotation: aQute.bnd.annotation.component.Deactivate on method stop with signature ()V. It is an error to mix these two types of annotations",
			errors.get(1));
		assertEquals(
			"The DS component mixed-std-bnd uses standard annotations to declare it as a component, but also uses the bnd DS annotation: aQute.bnd.annotation.component.Modified on method update with signature (Ljava/util/Map;)V. It is an error to mix these two types of annotations",
			errors.get(2));
		assertEquals(
			"The DS component mixed-std-bnd uses standard annotations to declare it as a component, but also uses the bnd DS annotation: aQute.bnd.annotation.component.Reference on method setLog with signature (Lorg/osgi/service/log/LogService;)V. It is an error to mix these two types of annotations",
			errors.get(3));
		assertEquals(0, b.getWarnings()
			.size());
	}

	@Component
	static class VolatileField {
		@Reference
		private volatile LogService	log1;
		@Reference
		private LogService			log2;
	}

	public void testVolatileFieldDynamic() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*VolatileField");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$VolatileField.xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute("test.component.DSAnnotationTest$VolatileField", "scr:component/implementation/@class");

		xt.assertAttribute("log1", "scr:component/reference[1]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[1]/@interface");
		xt.assertAttribute("dynamic", "scr:component/reference[1]/@policy");

		xt.assertAttribute("log2", "scr:component/reference[2]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[2]/@interface");
		xt.assertNoAttribute("scr:component/reference[2]/@policy");

	}

	@Component
	static class FinalDynamicCollectionField {
		@Reference(policy = ReferencePolicy.DYNAMIC)
		private final List<LogService>					logs1	= new CopyOnWriteArrayList<>();

		@Reference(policy = ReferencePolicy.DYNAMIC)
		private final CopyOnWriteArrayList<LogService>	logs2	= new CopyOnWriteArrayList<>();
	}

	public void testFinalDynamicCollectionField() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*FinalDynamicCollectionField");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$FinalDynamicCollectionField.xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute("test.component.DSAnnotationTest$FinalDynamicCollectionField",
			"scr:component/implementation/@class");

		xt.assertAttribute("logs1", "scr:component/reference[1]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[1]/@interface");
		xt.assertAttribute("dynamic", "scr:component/reference[1]/@policy");
		xt.assertAttribute("update", "scr:component/reference[1]/@field-option");

		xt.assertAttribute("logs2", "scr:component/reference[2]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[2]/@interface");
		xt.assertAttribute("dynamic", "scr:component/reference[2]/@policy");
		xt.assertAttribute("update", "scr:component/reference[2]/@field-option");

	}

	@Component
	public static final class FinalClassNonFinalField {
		@Reference
		private LogService logService;
	}

	// A field in a final class is not final:
	// https://github.com/bndtools/bnd/issues/2928
	public void testFinalFieldReference() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.*FinalClassNonFinalField*");
			b.setProperty("Private-Package", "test.component");
			b.addClasspath(new File("bin_test"));
			b.build();
			assertOk(b);
		}
	}

	@Component
	static class FieldCardinality {
		@Reference
		private List<LogService>				log1;
		@Reference
		private volatile Collection<LogService>	log2;
		@Reference
		private LogService						log3;
		@Reference
		private volatile LogService				log4;
		@Reference(policy = ReferencePolicy.DYNAMIC)
		private final List<LogService>			log5	= new CopyOnWriteArrayList<>();
	}

	public void testFieldCardinality() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*FieldCardinality");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);
		Attributes a = getAttr(jar);
		checkRequires(a, ComponentConstants.COMPONENT_SPECIFICATION_VERSION, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$FieldCardinality.xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		// Test the defaults
		xt.assertAttribute("test.component.DSAnnotationTest$FieldCardinality", "scr:component/implementation/@class");

		xt.assertAttribute("log1", "scr:component/reference[1]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[1]/@interface");
		xt.assertAttribute("0..n", "scr:component/reference[1]/@cardinality");
		xt.assertNoAttribute("scr:component/reference[1]/@policy");
		xt.assertNoAttribute("scr:component/reference[1]/@field-option");

		xt.assertAttribute("log2", "scr:component/reference[2]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[2]/@interface");
		xt.assertAttribute("0..n", "scr:component/reference[2]/@cardinality");
		xt.assertAttribute("dynamic", "scr:component/reference[2]/@policy");
		xt.assertNoAttribute("scr:component/reference[2]/@field-option");

		xt.assertAttribute("log3", "scr:component/reference[3]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[3]/@interface");
		xt.assertNoAttribute("scr:component/reference[3]/@cardinality");
		xt.assertNoAttribute("scr:component/reference[3]/@policy");
		xt.assertNoAttribute("scr:component/reference[3]/@field-option");

		xt.assertAttribute("log4", "scr:component/reference[4]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[4]/@interface");
		xt.assertNoAttribute("scr:component/reference[4]/@cardinality");
		xt.assertAttribute("dynamic", "scr:component/reference[4]/@policy");
		xt.assertNoAttribute("scr:component/reference[4]/@field-option");

		xt.assertAttribute("log5", "scr:component/reference[5]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[5]/@interface");
		xt.assertAttribute("0..n", "scr:component/reference[5]/@cardinality");
		xt.assertAttribute("dynamic", "scr:component/reference[5]/@policy");
		xt.assertAttribute("update", "scr:component/reference[5]/@field-option");
	}

	@Component
	static class MismatchedUnbind {
		@Reference
		void setLogService10(LogService ls) {}

		void updatedLogService10(FinalDynamicCollectionField notLs) {}

		void unsetLogService10(FinalDynamicCollectionField notLs) {}

		@Reference
		void setLogService11(LogService ls, Map<String, Object> props) {}

		void unsetLogService11(FinalDynamicCollectionField notLs, Map<String, Object> props) {}

		@Reference
		void setLogService13(LogService ls, Map<String, Object> props) {}

		void unsetLogService13(FinalDynamicCollectionField notLs, Map<String, Object> props) {}
	}

	public void testMismatchedUnbind() throws Exception {

		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*MismatchedUnbind");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b, 0, 8);
		Attributes a = getAttr(jar);
		checkRequires(a, null, LogService.class.getName());

		Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$MismatchedUnbind.xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");

		xt.assertAttribute("LogService10", "scr:component/reference[1]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[1]/@interface");
		xt.assertAttribute("setLogService10", "scr:component/reference[1]/@bind");
		xt.assertNoAttribute("scr:component/reference[1]/@unbind");
		xt.assertNoAttribute("scr:component/reference[1]/@updated");

		xt.assertAttribute("LogService11", "scr:component/reference[2]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[2]/@interface");
		xt.assertAttribute("setLogService11", "scr:component/reference[2]/@bind");
		xt.assertNoAttribute("scr:component/reference[2]/@unbind");

		xt.assertAttribute("LogService13", "scr:component/reference[3]/@name");
		xt.assertAttribute(LogService.class.getName(), "scr:component/reference[3]/@interface");
		xt.assertAttribute("setLogService13", "scr:component/reference[3]/@bind");
		xt.assertNoAttribute("scr:component/reference[3]/@unbind");

	}

	@Component(service = Map.class)
	static class NotAMap1 {}

	@Component(service = HashMap.class)
	static class NotAMap2 {}

	@SuppressWarnings("serial")
	@Component(service = HashMap.class)
	static class NotAMap3 extends TreeMap<String, String> {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	}

	public void testNotImplementedService() throws Exception {
		checkClass(NotAMap1.class, 1);
		checkClass(NotAMap2.class, 1);
		checkClass(NotAMap3.class, 1);
	}

	@SuppressWarnings("serial")
	@Component(service = Map.class)
	static class IsAMap1 extends HashMap<String, String> {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	}

	@SuppressWarnings("serial")
	static class MyHashMap1<K, V> extends HashMap<K, V> {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	}

	@SuppressWarnings("serial")
	@Component(service = HashMap.class)
	static class IsAMap2 extends MyHashMap1<String, String> {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	}

	interface MyMap<K, V> extends Map<K, V> {}

	@SuppressWarnings("serial")
	static class MyHashMap2<K, V> extends HashMap<K, V> implements MyMap<K, V> {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	}

	@SuppressWarnings("serial")
	@Component(service = Map.class)
	static class IsAMap3 extends MyHashMap2<String, String> {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	}

	interface Marker {}

	@SuppressWarnings("serial")
	@Component(service = Map.class)
	static class IsAMap3a extends MyHashMap2<String, String> implements Marker {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	}

	@Component(service = Map.class)
	static class IsAMap4 implements MyMap<String, String> {

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean containsKey(Object key) {
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			return false;
		}

		@Override
		public String get(Object key) {
			return null;
		}

		@Override
		public String put(String key, String value) {
			return null;
		}

		@Override
		public String remove(Object key) {
			return null;
		}

		@Override
		public void putAll(Map<? extends String, ? extends String> m) {}

		@Override
		public void clear() {}

		@Override
		public Set<String> keySet() {
			return null;
		}

		@Override
		public Collection<String> values() {
			return null;
		}

		@Override
		public Set<java.util.Map.Entry<String, String>> entrySet() {
			return null;
		}
	}

	public void testIndirectlyImplementedService() throws Exception {
		checkClass(IsAMap1.class, 0);
		checkClass(IsAMap2.class, 0);
		checkClass(IsAMap3.class, 0);
		checkClass(IsAMap3a.class, 0);
		checkClass(IsAMap4.class, 0);
	}

	private void checkClass(Class<?> c, int i) throws IOException, Exception, AssertionFailedError {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, c.getName());
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b, i, 0);
	}

	public interface GenericMarker<K> extends Marker {}

	@Component
	public static class RefType {

		@Reference(service = Marker.class)
		void setMarker1(ServiceReference<?> ref) {}

		@Reference(service = Marker.class)
		void setMarker2(ComponentServiceObjects<?> ref) {}

		@Reference
		void setMarker3(ServiceReference<Marker> ref) {}

		@Reference
		void setMarker4(ComponentServiceObjects<Marker> ref) {}

		@Reference(service = GenericMarker.class)
		void setMarker5(ServiceReference<Marker> ref) {}

		@Reference(service = GenericMarker.class)
		void setMarker6(ComponentServiceObjects<Marker> ref) {}

		@Reference
		void setMarker7(ServiceReference<GenericMarker<?>> ref) {}

		@Reference
		void setMarker8(ComponentServiceObjects<GenericMarker<?>> ref) {}

		@Reference
		void setMarker9(ServiceReference<GenericMarker<Marker>> ref) {}

		@Reference
		void setMarker10(ComponentServiceObjects<GenericMarker<Marker>> ref) {}

		@Reference
		void setMarker11(ServiceReference<GenericMarker<? extends Marker>> ref) {}

		@Reference
		void setMarker12(ComponentServiceObjects<GenericMarker<? super Marker>> ref) {}

		@Reference
		void setMarker13(ServiceReference<? extends GenericMarker<? extends Marker>> ref) {}

		@Reference
		void setMarker14(ComponentServiceObjects<? super GenericMarker<? super Marker>> ref) {}

	}

	private List<String> indices = new ArrayList<>();

	public void testReferenceType() throws Exception {

		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*RefType");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b, 0, 0);
		Attributes a = getAttr(jar);
		checkRequires(a, null, Marker.class.getName(), GenericMarker.class.getName());

		Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$RefType.xml");
		System.err.println(Processor.join(jar.getResources()
			.keySet(), "\n"));
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");

		Collection<String> indices = new TreeSet<>();
		for (int i = 1; i < 15; i++)
			indices.add(Integer.toString(i));
		this.indices.addAll(indices);

		checkMarkerReference(xt, 1, Marker.class);
		checkMarkerReference(xt, 2, Marker.class);
		checkMarkerReference(xt, 3, Marker.class);
		checkMarkerReference(xt, 4, Marker.class);
		checkMarkerReference(xt, 5, GenericMarker.class);
		checkMarkerReference(xt, 6, GenericMarker.class);
		checkMarkerReference(xt, 7, GenericMarker.class);
		checkMarkerReference(xt, 8, GenericMarker.class);
		checkMarkerReference(xt, 9, GenericMarker.class);
		checkMarkerReference(xt, 10, GenericMarker.class);
		checkMarkerReference(xt, 11, GenericMarker.class);
		checkMarkerReference(xt, 12, GenericMarker.class);
		checkMarkerReference(xt, 13, GenericMarker.class);
		checkMarkerReference(xt, 14, GenericMarker.class);

	}

	private void checkMarkerReference(XmlTester xt, int count, Class<?> cl) throws XPathExpressionException {
		String index = Integer.toString(indices.indexOf(Integer.toString(count)) + 1);
		xt.assertAttribute("Marker" + count, "scr:component/reference[" + index + "]/@name");
		xt.assertAttribute(cl.getName(), "scr:component/reference[" + index + "]/@interface");
		xt.assertAttribute("setMarker" + count, "scr:component/reference[" + index + "]/@bind");
		xt.assertNoAttribute("scr:component/reference[" + index + "]/@unbind");
		xt.assertNoAttribute("scr:component/reference[" + index + "]/@updated");
	}

	@Component(reference = @Reference(name = "LogService", service = LogService.class))
	public static class ComponentReferenceGood {}

	@Component(reference = @Reference(service = LogService.class))
	public static class ComponentReferenceBad {}

	public void testComponentReference() throws Exception {

		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*ComponentReference*");
		b.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;minimum=1.0.0");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b, 1, 0);

		{
			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$ComponentReferenceGood.xml");
			System.err.println(Processor.join(jar.getResources()
				.keySet(), "\n"));
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream());
			xt.assertAttribute(LogService.class.getName(), "component/reference[1]/@interface");
		}
		{
			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$ComponentReferenceBad.xml");
			System.err.println(Processor.join(jar.getResources()
				.keySet(), "\n"));
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream());
			xt.assertCount(0, "component/reference");
		}
	}

	public interface Activatable<T extends Annotation> {
		void activator(T config);
	}

	@Component(service = {})
	public static class ActivatableComponent implements Activatable<ConfigA> {
		@Override
		@Activate
		public void activator(ConfigA config) {
			String a = config.a();
		}
	}

	/*
	 * See https://github.com/bndtools/bnd/issues/1546. If a component class has
	 * an annotated method for which the compiler generates a bridge method,
	 * javac will copy the annotations onto the bridge method. Bnd must ignore
	 * bridge methods.
	 */
	public void testBridgeMethod() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, "test.component.*ActivatableComponent");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b, 0, 0);

		Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$ActivatableComponent.xml");
		assertNotNull(r);
		r.write(System.err);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.3.0");
		xt.assertAttribute("activator", "scr:component/@activate");
	}

	@Component
	public static class LoggerComponent {
		@Activate
		public LoggerComponent(@Reference(service = LoggerFactory.class) Logger loggerC,
			@Reference(service = LoggerFactory.class) FormatterLogger formatterLoggerC) {}

		@Reference(service = LoggerFactory.class)
		private Logger logger;

		@Reference(service = LoggerFactory.class)
		void bindLogger(Logger logger) {}

		@Reference(service = LoggerFactory.class)
		private FormatterLogger formatterLogger;

		@Reference(service = LoggerFactory.class)
		void bindFormatterLogger(FormatterLogger flogger) {}
	}

	public void testLoggerSupport() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$LoggerComponent");
			b.setProperty("Private-Package", "test.component");
			b.addClasspath(new File("bin_test"));

			Jar jar = b.build();
			assertOk(b);
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, "1.4.0", LoggerFactory.class.getName());

			//
			// Test all the defaults
			//

			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$LoggerComponent.xml");
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.4.0");
			xt.assertAttribute("0", "count(scr:component/properties)");
			xt.assertAttribute("0", "count(scr:component/property)");

			// This test method depends upon the compiler generating
			// MethodParameters attributes so that the DS annotations code will
			// generate the desired reference name from the parameter name.
			xt.assertAttribute(LoggerFactory.class.getName(), "scr:component/reference[@name='loggerC']/@interface");
			xt.assertAttribute("0", "scr:component/reference[@name='loggerC']/@parameter");

			xt.assertAttribute(LoggerFactory.class.getName(),
				"scr:component/reference[@name='formatterLoggerC']/@interface");
			xt.assertAttribute("1", "scr:component/reference[@name='formatterLoggerC']/@parameter");

			xt.assertAttribute(LoggerFactory.class.getName(), "scr:component/reference[@name='logger']/@interface");
			xt.assertAttribute("logger", "scr:component/reference[@name='logger']/@field");

			xt.assertAttribute(LoggerFactory.class.getName(),
				"scr:component/reference[@name='formatterLogger']/@interface");
			xt.assertAttribute("formatterLogger", "scr:component/reference[@name='formatterLogger']/@field");

			xt.assertAttribute(LoggerFactory.class.getName(), "scr:component/reference[@name='Logger']/@interface");
			xt.assertAttribute("bindLogger", "scr:component/reference[@name='Logger']/@bind");

			xt.assertAttribute(LoggerFactory.class.getName(),
				"scr:component/reference[@name='FormatterLogger']/@interface");
			xt.assertAttribute("bindFormatterLogger", "scr:component/reference[@name='FormatterLogger']/@bind");

		}
	}

	public static @interface ConstructorConfig {
		String name();

		long id();
	}

	@Component
	public static class ConstructorInjection {
		@Activate
		public ConstructorInjection(ComponentContext cc, @Reference LogService log, ConstructorConfig myId) {}

		@Activate
		Map<String, Object> componentProps;

		@Activate
		void activator() {}
	}

	public void testConstructorInjection() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.DSAnnotationTest$ConstructorInjection");
			b.setProperty("Private-Package", "test.component");
			b.addClasspath(new File("bin_test"));

			Jar jar = b.build();
			assertOk(b);
			Attributes a = getAttr(jar);
			checkProvides(a);
			checkRequires(a, "1.4.0", LogService.class.getName());

			//
			// Test all the defaults
			//

			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$ConstructorInjection.xml");
			assertNotNull(r);
			r.write(System.err);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "http://www.osgi.org/xmlns/scr/v1.4.0");
			xt.assertAttribute("0", "count(scr:component/properties)");
			xt.assertAttribute("0", "count(scr:component/property)");

			xt.assertAttribute("3", "scr:component/@init");
			xt.assertAttribute("activator", "scr:component/@activate");
			xt.assertAttribute("componentProps", "scr:component/@activation-fields");

			// This test method depends upon the compiler generating
			// MethodParameters attributes so that the DS annotations code will
			// generate the desired reference name from the parameter name.
			xt.assertAttribute(LogService.class.getName(), "scr:component/reference[@name='log']/@interface");
			xt.assertAttribute("1", "scr:component/reference[@name='log']/@parameter");

		}
	}

}
