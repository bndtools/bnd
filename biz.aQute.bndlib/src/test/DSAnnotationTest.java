package test;

import java.io.*;

import org.osgi.service.component.annotations.*;
import org.osgi.service.log.*;

import aQute.bnd.test.*;
import aQute.lib.osgi.*;

public class DSAnnotationTest extends BndTestCase {

	@Component public class Defaults implements Serializable, Runnable {

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

		void modifiedLogService(LogService log) {

		}

		public void run() {
			// TODO Auto-generated method stub

		}
	}

	@Component(service = Object.class, configurationPolicy = ConfigurationPolicy.IGNORE, enabled = false, factory = "factory", immediate = false, name = "name", property = {
			"a=1", "a=2", "b=3" }, properties = "resource.props", servicefactory = false) public class Explicit
			implements Serializable, Runnable {

		@Activate void open() {
		}

		@Deactivate void close() {
		}

		@Modified void changed() {
		}

		@Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, name = "foo", policy = ReferencePolicy.DYNAMIC, service = Object.class, target = "(objectclass=*)", unbind = "unset" /*
																																														 * ,
																																														 * modified
																																														 */) void setLogService(
				LogService log) {

		}

		void unset(Object log) {

		}

		void unset() {

		}

		void unsetLogService(LogService log) {

		}

		void modifiedLogService(Object log) {

		}

		public void run() {
			// TODO Auto-generated method stub

		}
	}

	public void testBasic() throws Exception {
		Builder b = new Builder();
		b.setProperty("-dsannotations", "test.*");
		b.setProperty("Private-Package", "test");
		b.addClasspath(new File("bin"));

		Jar jar = b.build();
		assertOk(b);

		{
			//
			// Test all the defaults
			//

			Resource r = jar.getResource("OSGI-INF/test.DSAnnotationTest$Defaults.xml");
			System.out.println(Processor.join(jar.getResources().keySet(),"\n"));
			assertNotNull(r);
			r.write(System.out);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr",
					"http://www.osgi.org/xmlns/scr/1.1.0");

			// Test the defaults
			xt.assertAttribute("test.DSAnnotationTest$Defaults",
					"scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("test.DSAnnotationTest$Defaults", "scr:component/@name");

			xt.assertAttribute("", "scr:component/@configuration-policy");
			xt.assertAttribute("", "scr:component/@immediate");
			xt.assertAttribute("", "scr:component/@enabled");
			xt.assertAttribute("", "scr:component/@factory");
			xt.assertAttribute("", "scr:component/@servicefactory");
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
		}
		{
			//
			// Test explicit
			//

			Resource r = jar.getResource("OSGI-INF/test.DSAnnotationTest$Explicit.xml");
			assertNotNull(r);
			r.write(System.out);
			XmlTester xt = new XmlTester(r.openInputStream(), "scr",
					"http://www.osgi.org/xmlns/scr/1.1.0");

			// Test the defaults
			xt.assertAttribute("test.DSAnnotationTest$Explicit",
					"scr:component/implementation/@class");

			// Default must be the implementation class
			xt.assertAttribute("test.DSAnnotationTest$Explicit", "scr:component/@name");

			xt.assertAttribute("ignore", "scr:component/@configuration-policy");
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
			xt.assertAttribute("modifiedLogService", "scr:component/reference[1]/@modified");
			xt.assertAttribute("1..n", "scr:component/reference[1]/@cardinality");
			xt.assertAttribute("dynamic", "scr:component/reference[1]/@policy");
			xt.assertAttribute("(objectclass=*)", "scr:component/reference[1]/@target");
			
			xt.assertAttribute("2", "count(scr:component/property)");
			xt.assertAttribute("1", "count(scr:component/properties)");
			xt.assertAttribute("resource.props", "scr:component/properties[1]/@entry");
		}
	}
}
