package test.component;

import java.io.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.component.*;
import org.osgi.service.component.annotations.*;
import org.osgi.service.log.*;

import aQute.bnd.test.*;
import aQute.lib.osgi.*;

public class DSAnnotationTest extends BndTestCase {

	/**
	 * The basic test. This test will take an all default component and a
	 * component that has all values set.
	 */
	@Component public class Defaults implements Serializable, Runnable {
		private static final long	serialVersionUID	= 1L;

		@Activate void open() {
		}

		@Deactivate void close() {
		}

		@Modified void modified() {
		}

		@Reference void setLogService(LogService log) {

		}

		void unsetLogService(LogService log) {

		}

		// void modifiedLogService(LogService log) {
		//
		// }

		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component(service = Object.class, configurationPolicy = ConfigurationPolicy.IGNORE, enabled = false, factory = "factory", immediate = false, name = "name", property = {
			"a=1", "a=2", "b=3" }, properties = "resource.props", servicefactory = false, configurationPid = "configuration-pid", xmlns = "xmlns") public class Explicit
			implements Serializable, Runnable {
		private static final long	serialVersionUID	= 1L;

		@Activate void open() {
		}

		@Deactivate void close() {
		}

		@Modified void changed() {
		}

		@Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, name = "foo", policy = ReferencePolicy.DYNAMIC, service = Object.class, target = "(objectclass=*)", unbind = "unset", updated = "updatedLogService", policyOption = ReferencePolicyOption.GREEDY) void setLogService(
				LogService log) {

		}

		void unset(Object log) {

		}

		void unset() {

		}

		void unsetLogService(LogService log) {

		}

		void updatedLogService(Object log) {

		}

		public void run() {
			// TODO Auto-generated method stub

		}
	}

	public void testBasic() throws Exception {
		Builder b = new Builder();
		b.setProperty("-dsannotations", "test.component.*");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin"));

		Jar jar = b.build();
		assertOk(b);

		{
			//
			// Test all the defaults
			//

			Resource r = jar.getResource("OSGI-INF/test.component.DSAnnotationTest$Defaults.xml");
			System.out.println(Processor.join(jar.getResources().keySet(), "\n"));
			assertNotNull(r);
			r.write(System.out);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr",
					"http://www.osgi.org/xmlns/scr/1.1.0");

			// Test the defaults
			xt.assertAttribute("test.component.DSAnnotationTest$Defaults",
					"scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("test.component.DSAnnotationTest$Defaults", "scr:component/@name");

			xt.assertAttribute("", "scr:component/@configuration-policy");
			xt.assertAttribute("", "scr:component/@immediate");
			xt.assertAttribute("", "scr:component/@enabled");
			xt.assertAttribute("", "scr:component/@factory");
			xt.assertAttribute("", "scr:component/@servicefactory");
			xt.assertAttribute("", "scr:component/@configuration-pid");
			xt.assertAttribute("open", "scr:component/@activate");
			xt.assertAttribute("close", "scr:component/@deactivate");
			xt.assertAttribute("modified", "scr:component/@modified");
			xt.assertAttribute("java.io.Serializable",
					"scr:component/service/provide[1]/@interface");
			xt.assertAttribute("java.lang.Runnable", "scr:component/service/provide[2]/@interface");

			xt.assertAttribute("0", "count(scr:component/properties)");
			xt.assertAttribute("0", "count(scr:component/property)");

			xt.assertAttribute("", "scr:component/reference[1]/@target");
			xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
			xt.assertAttribute("unsetLogService", "scr:component/reference[1]/@unbind");
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
			r.write(System.out);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr", "xmlns");

			// Test the defaults
			xt.assertAttribute("test.component.DSAnnotationTest$Explicit",
					"scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("name", "scr:component/@name");

			xt.assertAttribute("ignore", "scr:component/@configuration-policy");
			xt.assertAttribute("configuration-pid", "scr:component/@configuration-pid");
			xt.assertAttribute("false", "scr:component/@immediate");
			xt.assertAttribute("false", "scr:component/@enabled");
			xt.assertAttribute("factory", "scr:component/@factory");
			xt.assertAttribute("false", "scr:component/@servicefactory");
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
	}

	/**
	 * Tests all the different enum values. This also tests the ordering.
	 */
	@Component(name = "enums") public class Enums {

		@Reference void setA(LogService l) {
		}

		@Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY) void setB(
				LogService l) {
		}

		@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.RELUCTANT) void setE(
				LogService l) {
		}

		@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.RELUCTANT) void setC(
				LogService l) {
		}

		@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY) void setD(
				LogService l) {
		}

	}

	public void testEnums() throws Exception {
		Builder b = new Builder();
		b.setProperty("-dsannotations", "test.component.DSAnnotationTest*Enums");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin"));

		Jar jar = b.build();
		assertOk(b);

		Resource r = jar.getResource("OSGI-INF/enums.xml");
		assertNotNull(r);
		r.write(System.out);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr",
				"http://www.osgi.org/xmlns/scr/1.2.0");

		xt.assertAttribute("setA", "scr:component/reference[1]/@name");
		xt.assertAttribute("", "scr:component/reference[1]/@cardinality");
		xt.assertAttribute("", "scr:component/reference[1]/@policy");
		xt.assertAttribute("", "scr:component/reference[1]/@policy-option");

		xt.assertAttribute("setB", "scr:component/reference[2]/@name");
		xt.assertAttribute("1..n", "scr:component/reference[2]/@cardinality");
		xt.assertAttribute("dynamic", "scr:component/reference[2]/@policy");
		xt.assertAttribute("greedy", "scr:component/reference[2]/@policy-option");

		xt.assertAttribute("setC", "scr:component/reference[3]/@name");
		xt.assertAttribute("1..1", "scr:component/reference[3]/@cardinality");
		xt.assertAttribute("static", "scr:component/reference[3]/@policy");
		xt.assertAttribute("reluctant", "scr:component/reference[3]/@policy-option");

		xt.assertAttribute("setD", "scr:component/reference[4]/@name");
		xt.assertAttribute("0..n", "scr:component/reference[4]/@cardinality");
		xt.assertAttribute("dynamic", "scr:component/reference[4]/@policy");
		xt.assertAttribute("greedy", "scr:component/reference[4]/@policy-option");

		xt.assertAttribute("setE", "scr:component/reference[5]/@name");
		xt.assertAttribute("0..1", "scr:component/reference[5]/@cardinality");
		xt.assertAttribute("dynamic", "scr:component/reference[5]/@policy");
		xt.assertAttribute("reluctant", "scr:component/reference[5]/@policy-option");
	}

	/**
	 * Test the - for the unbind and updated parameter.
	 */
	@Component(name = "methods") public class Methods {

		@Reference(unbind="-", updated="-") void setA(LogService l) {
		}

		void updatedA(LogService l) {}
		void unsetA(LogService l) {}

		@Reference(unbind="_B", updated="__B") void setB(LogService l) {
		}

		void _B(LogService l) {}
		void __B(LogService l) {}
		void updatedB(LogService l) {}
		void unsetB(LogService l) {}

		
		@Reference void setC(LogService l) {
		}
		void updatedC(LogService l) {}
		void unsetC(LogService l) {}
		
		@Reference void setD(LogService l) {
		}
		
		
	}

	public void testMethods() throws Exception {
		Builder b = new Builder();
		b.setProperty("-dsannotations", "test.component.DSAnnotationTest*Methods");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin"));

		Jar jar = b.build();
		assertOk(b);

		Resource r = jar.getResource("OSGI-INF/methods.xml");
		assertNotNull(r);
		r.write(System.out);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr",
				"http://www.osgi.org/xmlns/scr/1.2.0");

		// use - to make sure no unbind and updated method is set
		xt.assertAttribute("setA", "scr:component/reference[1]/@name");
		xt.assertAttribute("setA", "scr:component/reference[1]/@bind");
		xt.assertAttribute("", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("", "scr:component/reference[1]/@updated");

		// override the names for the methods
		xt.assertAttribute("setB", "scr:component/reference[2]/@name");
		xt.assertAttribute("setB", "scr:component/reference[2]/@bind");
		xt.assertAttribute("_B", "scr:component/reference[2]/@unbind");
		xt.assertAttribute("__B", "scr:component/reference[2]/@updated");

		xt.assertAttribute("setC", "scr:component/reference[3]/@name");
		xt.assertAttribute("setC", "scr:component/reference[3]/@bind");
		xt.assertAttribute("unsetC", "scr:component/reference[3]/@unbind");
		xt.assertAttribute("updatedC", "scr:component/reference[3]/@updated");
		
		xt.assertAttribute("setD", "scr:component/reference[4]/@name");
		xt.assertAttribute("setD", "scr:component/reference[4]/@bind");
		xt.assertAttribute("", "scr:component/reference[4]/@unbind");
		xt.assertAttribute("", "scr:component/reference[4]/@updated");
	}

	/**
	 * Test inheritance (this is not official)
	 */
	
	public class Top {
		
		@Reference
		void setLogService( LogService l) {			
		}
		
		void updatedLogService(ServiceReference ref) {
			
		}
		
		@Reference 
		protected void setPrivateLogService(LogService l) {
			
		}
		
		@SuppressWarnings("unused") private void updatedPrivateLogService(ServiceReference ref) {
			
		}
	}
	
	@Component(name="bottom") public class Bottom extends Top{
		void unsetLogService(LogService l, Map map) {
			
		}
		
		void unsetPrivateLogService(ServiceReference ref) {
			
		}
	}
	
	public void testInheritance() throws Exception {
		Builder b = new Builder();
		b.setProperty("-dsannotations", "test.component.DSAnnotationTest*Bottom");
		b.setProperty("-dsannotations-inherit", "true");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin"));
		
		Jar jar = b.build();
		assertOk(b);

		Resource r = jar.getResource("OSGI-INF/bottom.xml");
		assertNotNull(r);
		r.write(System.out);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr",
				"http://www.osgi.org/xmlns/scr/1.2.0");
		
		
		xt.assertAttribute("setLogService", "scr:component/reference[1]/@name");
		xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
		xt.assertAttribute("unsetLogService", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("updatedLogService", "scr:component/reference[1]/@updated");
		
		xt.assertAttribute("setPrivateLogService", "scr:component/reference[2]/@name");
		xt.assertAttribute("setPrivateLogService", "scr:component/reference[2]/@bind");
		xt.assertAttribute("unsetPrivateLogService", "scr:component/reference[2]/@unbind");
		xt.assertAttribute("", "scr:component/reference[2]/@updated"); // is private in super class	
		

	}

	
	/**
	 * Test the different prototypes ...
	 */
	
	@Component(name="prototypes") public class Prototypes {
		@SuppressWarnings("unused") @Activate
		private void activate() {}
		
		@Deactivate
		protected void deactivate(ComponentContext ctx) {
			
		}

		@Modified
		void modified( BundleContext context) {
			
		}
		
		@SuppressWarnings("unused") @Reference
		private void setLogService( LogService l) {
			
		}
		protected void unsetLogService( LogService l, Map map) {
			
		}
		
		void updatedLogService(ServiceReference ref) {
			
		}
	}
	
	public void testPrototypes() throws Exception {
		Builder b = new Builder();
		b.setProperty("-dsannotations", "test.component.DSAnnotationTest*Prototypes");
		b.setProperty("Private-Package", "test.component");
		b.addClasspath(new File("bin"));
		
		Jar jar = b.build();
		assertOk(b);

		Resource r = jar.getResource("OSGI-INF/prototypes.xml");
		assertNotNull(r);
		r.write(System.out);
		XmlTester xt = new XmlTester(r.openInputStream(), "scr",
				"http://www.osgi.org/xmlns/scr/1.2.0");
		
		xt.assertAttribute("setLogService", "scr:component/reference[1]/@name");
		xt.assertAttribute("setLogService", "scr:component/reference[1]/@bind");
		xt.assertAttribute("unsetLogService", "scr:component/reference[1]/@unbind");
		xt.assertAttribute("updatedLogService", "scr:component/reference[1]/@updated");
		
		
		
	}
	
	
	
	
	
}
