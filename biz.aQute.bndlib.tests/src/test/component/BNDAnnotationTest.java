package test.component;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import javax.xml.namespace.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import junit.framework.*;

import org.osgi.framework.*;
import org.osgi.service.component.*;
import org.osgi.service.event.*;
import org.osgi.service.log.*;
import org.w3c.dom.*;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Constants;

public class BNDAnnotationTest extends TestCase {
	static final DocumentBuilderFactory	dbf		= DocumentBuilderFactory.newInstance();
	static final XPathFactory				xpathf	= XPathFactory.newInstance();
	static final XPath						xpath	= xpathf.newXPath();
	static DocumentBuilder					db;

	static {
		try {
			dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
			xpath.setNamespaceContext(new NamespaceContext() {

				@Override
				public Iterator<String> getPrefixes(String namespaceURI) {
					return Arrays.asList("md", "scr").iterator();
				}

				@Override
				public String getPrefix(String namespaceURI) {
					if (namespaceURI.equals("http://www.osgi.org/xmlns/metatype/v1.1.0"))
						return "md";
					if (namespaceURI.equals("http://www.osgi.org/xmlns/scr/v1.1.0"))
						return "scr";

					return null;
				}

				@Override
				public String getNamespaceURI(String prefix) {
					if (prefix.equals("md"))
						return "http://www.osgi.org/xmlns/metatype/v1.1.0";
					else if (prefix.equals("scr"))
						return "http://www.osgi.org/xmlns/scr/v1.1.0";
					else
						return null;
				}
			});
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Can we order the references? References are ordered by their name as Java
	 * does not define the order of the methods.
	 * 
	 * @throws Exception
	 */

	@Component
	static class TestReferenceOrdering {

		@Reference(service = LogService.class)
		void setA(@SuppressWarnings("unused") ServiceReference ref) {}

		@Reference
		void setC(@SuppressWarnings("unused") LogService log) {}

		@Reference(service = LogService.class)
		void setB(@SuppressWarnings("unused") Map<String,Object> map) {}
	}

	public static void testAnnotationReferenceOrdering() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Service-Component", "*TestReferenceOrdering");
		b.setProperty("Private-Package", "test.component");
		b.build();
		Document doc = doc(b, "test.component.BNDAnnotationTest$TestReferenceOrdering");
		NodeList nodes = (NodeList) xpath.evaluate("//reference", doc, XPathConstants.NODESET);
		assertEquals("a", nodes.item(0).getAttributes().getNamedItem("name").getTextContent());
		assertEquals("b", nodes.item(1).getAttributes().getNamedItem("name").getTextContent());
		assertEquals("c", nodes.item(2).getAttributes().getNamedItem("name").getTextContent());
	}

	/**
	 * Test config with metatype
	 */

	@Component(name = "config", designateFactory = Config.class, configurationPolicy = ConfigurationPolicy.require)
	static class MetatypeConfig {
		interface Config {
			String name();
		}
	}

	@Component(designate = Config.class)
	static class MetatypeConfig2 {
		interface Config {
			String name();
		}
	}

	public static void testConfig() throws Exception {
		Builder b = new Builder();
		b.setExceptions(true);
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*MetatypeConfig*");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.err.println(b.getJar().getResources().keySet());

		// Check component name
		{
			Resource cr = b.getJar().getResource("OSGI-INF/config.xml");
			cr.write(System.err);
			Document d = db.parse(cr.openInputStream());
			assertEquals("config", xpath.evaluate("/scr:component/@name", d, XPathConstants.STRING));
		}

		// Check if config properly linked
		{
			Resource mr = b.getJar().getResource("OSGI-INF/metatype/config.xml");
			mr.write(System.err);
			Document d = db.parse(mr.openInputStream());
			assertEquals("config", xpath.evaluate("//Designate/@factoryPid", d, XPathConstants.STRING));
			assertEquals("config", xpath.evaluate("//Object/@ocdref", d, XPathConstants.STRING));
		}

		// Now with default name
		{
			Resource cr2 = b.getJar().getResource("OSGI-INF/test.component.BNDAnnotationTest$MetatypeConfig2.xml");
			cr2.write(System.err);
			Document d = db.parse(cr2.openInputStream());
			assertEquals("test.component.BNDAnnotationTest$MetatypeConfig2",
					xpath.evaluate("//scr:component/@name", d, XPathConstants.STRING));
		}
		{
			Resource mr2 = b.getJar().getResource("OSGI-INF/metatype/test.component.BNDAnnotationTest$MetatypeConfig2.xml");
			mr2.write(System.err);
			Document d = db.parse(mr2.openInputStream());
			assertEquals("test.component.BNDAnnotationTest$MetatypeConfig2",
					xpath.evaluate("//Designate/@pid", d, XPathConstants.STRING));
			assertEquals("test.component.BNDAnnotationTest$MetatypeConfig2",
					xpath.evaluate("//Object/@ocdref", d, XPathConstants.STRING));
		}
	}

	/**
	 * Test properties
	 */
	@Meta.OCD
	static interface Config {
		String name();
	}

	@Component(name = "props", properties = {
			" a =1", "b=3", "c=1|2|3"
	}, designate = Config.class, designateFactory = Config.class)
	static class PropertiesAndConfig {
		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext c) {}
	}

	public static void testPropertiesAndConfig() throws Exception {
		Builder b = new Builder();
		b.setExceptions(true);
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*PropertiesAndConfig");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Resource cr = b.getJar().getResource("OSGI-INF/props.xml");
		cr.write(System.err);
		{
			Document doc = doc(b, "props");
			assertEquals("1", xpath.evaluate("scr:component/property[@name='a']/@value", doc));
			assertEquals("3", xpath.evaluate("scr:component/property[@name='b']/@value", doc));
			assertEquals("1\n2\n3", xpath.evaluate("scr:component/property[@name='c']", doc).trim());
		}

	}

	/**
	 * Test if a package private method gives us 1.1 + namespace in the XML using bnd annotations
	 */

	@Component(name = "protected")
	static class PackageProtectedActivateMethod {
		@Activate
		protected void activatex(@SuppressWarnings("unused") ComponentContext c) {}
	}

	@Component(name = "packageprivate")
	static class PackagePrivateActivateMethod {
		@Activate
		void activatex(@SuppressWarnings("unused") ComponentContext c) {}
	}

	@Component(name = "private")
	static class PrivateActivateMethod {
		@SuppressWarnings("unused")
		@Activate
		private void activatex(ComponentContext c) {}
	}

	@Component(name = "default-private")
	static class DefaultPrivateActivateMethod {
		@SuppressWarnings("unused")
		@Activate
		private void activate(ComponentContext c) {}
	}

	@Component(name = "default-protected")
	static class DefaultProtectedActivateMethod {
		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext c) {}
	}

	@Component(name = "public")
	static class PublicActivateMethod {
		@Activate
		public void activatex(@SuppressWarnings("unused") ComponentContext c) {}
	}

	public static void testPackagePrivateActivateMethodBndAnnos() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*ActivateMethod");
		b.setProperty("Private-Package", "test.component");
		b.build();
		assertTrue(b.check());

		{
			Document doc = doc(b, "default-private");
			Object o = xpath.evaluate("scr:component", doc, XPathConstants.NODE);
			assertNotNull(o);
			assertEquals("", xpath.evaluate("component/@activate", doc));
		}
		{
			Document doc = doc(b, "default-protected");
			Object o = xpath.evaluate("component", doc, XPathConstants.NODE);
			assertNotNull(o);
			assertEquals("", xpath.evaluate("component/@activate", doc));
		}
		{
			Document doc = doc(b, "public");
			Object o = xpath.evaluate("//scr:component", doc, XPathConstants.NODE);
			assertNotNull(o);
			assertEquals("activatex", xpath.evaluate("scr:component/@activate", doc));
		}
		{
			Document doc = doc(b, "private");
			Object o = xpath.evaluate("//scr:component", doc, XPathConstants.NODE);
			assertNotNull(o);
			assertEquals("activatex", xpath.evaluate("scr:component/@activate", doc));
		}
		{
			Document doc = doc(b, "protected");
			Object o = xpath.evaluate("//scr:component", doc, XPathConstants.NODE);
			assertNotNull(o);
			assertEquals("activatex", xpath.evaluate("scr:component/@activate", doc));
		}
		{
			Document doc = doc(b, "packageprivate");
			Object o = xpath.evaluate("//scr:component", doc, XPathConstants.NODE);
			assertNotNull(o);
			assertEquals("activatex", xpath.evaluate("scr:component/@activate", doc));
		}

	}
	
	/**
	 * Test an attribute on an annotation
	 */

	@Component(name = "annotated")
	static class Annotated {

		@Reference
		protected void setLog(@SuppressWarnings("unused") LogService log) {}

	}

	public static void testAnnotatedWithAttribute() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*NoUnbind;log=org.osgi.service.log.LogService");
		b.setProperty("Private-Package", "test.component");
		Jar jar = b.build();
		Manifest manifest = jar.getManifest();
		String sc = manifest.getMainAttributes().getValue(Constants.SERVICE_COMPONENT);
		assertFalse(sc.contains(";"));
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

	}

	/**
	 * Imported default package because JSR14 seems to do something weird with
	 * annotations.
	 * 
	 * @throws Exception
	 */
	public static void testJSR14ComponentAnnotations() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource",
				"org/osgi/impl/service/coordinator/AnnotationWithJSR14.class=jar/AnnotationWithJSR14.jclass");
		b.setProperty("Service-Component", "*");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(1, b.getWarnings().size());

		Manifest manifest = jar.getManifest();
		String component = manifest.getMainAttributes().getValue("Service-Component");
		System.err.println(component);
		assertNull(component);
	}

	@Component(name = "nounbind")
	static class NoUnbind {

		@Reference
		protected void setLog(@SuppressWarnings("unused") LogService log) {}

	}
	
	public static void testNoUnbind() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*NoUnbind");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "nounbind");
		assertEquals("setLog", xpath.evaluate("component/reference/@bind", doc));
		assertEquals("", xpath.evaluate("component/reference/@unbind", doc));
	}

	@Component(name = "nounbind_dynamic")
	static class NoUnbindDynamic {
		
		@Reference(dynamic = true)
		protected void setLog(@SuppressWarnings("unused") LogService log) {}
	}
	
	public static void testNoUnbindDynamic() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*NoUnbindDynamic");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(1, b.getErrors().size());
		assertTrue(b.getErrors().get(0).endsWith("dynamic but has no unbind method."));
		assertEquals(0, b.getWarnings().size());
	}
	
	//this is a bnd annotation not a DS annotation
	@Component(name = "explicitunbind")
	static class ExplicitUnbind {

		@Reference(unbind = "killLog")
		protected void setLog(@SuppressWarnings("unused") LogService log) {}

	}

	public static void testExplicitUnbind() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*ExplicitUnbind");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(1, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "explicitunbind");
		assertEquals("setLog", xpath.evaluate("component/reference/@bind", doc));
		assertEquals("killLog", xpath.evaluate("component/reference/@unbind", doc));
	}

	/**
	 * Test to see if we get a namespace when we use 1.1 semantics on the
	 * activate and deactivate
	 */

	@Component(name = "ncomp", provide = {})
	static class NewActivateVersion {

		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext context) {}

	}

	@Component(name = "ndcomp", provide = {})
	static class NewDeActivateVersion {
		@Deactivate
		protected void deactivate() {}

	}

	@Component(name = "nbcomp", provide = {})
	static class NewBindVersion {

		@Reference
		protected void bind(@SuppressWarnings("unused") ServiceReference ref, @SuppressWarnings("unused") Map<String,Object> map) {}

	}

	public static void testNewVersion() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*Version");
		b.setProperty("Private-Package", "test.component");
		b.build();
		assertTrue(b.check());

		{
			Document doc = doc(b, "ncomp");
			Node o = (Node) xpath.evaluate("scr:component", doc, XPathConstants.NODE);
			assertNull("Expected ncomp to have old namespace", o);
			o = (Node) xpath.evaluate("component", doc, XPathConstants.NODE);
			assertNotNull("Expected ncomp to have old namespace", o);
		}

	}

	/**
	 * Test the same bind method names
	 */

	@Component(name = "cpcomp")
	static class SameRefName {

		@Reference
		protected void bind(@SuppressWarnings("unused") LogService log) {

		}

		@Reference
		protected void bind(@SuppressWarnings("unused") EventAdmin event) {

		}
	}

	public static void testSameRefName() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.SameRefName");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(1, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		// Document doc = doc(b, "cpcomp");
	}

	/**
	 * Test the configuration policy
	 */

	@Component(name = "cpcomp", configurationPolicy = ConfigurationPolicy.require, provide = {
			Serializable.class, EventAdmin.class
	})
	static class ConfigurationPolicyTest {

	}

	public static void testConfigurationPolicy() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.ConfigurationPolicyTest");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "cpcomp");
		assertEquals("java.io.Serializable",
				doc.getElementsByTagName("provide").item(0).getAttributes().getNamedItem("interface").getTextContent());
		assertEquals("org.osgi.service.event.EventAdmin", doc.getElementsByTagName("provide").item(1).getAttributes()
				.getNamedItem("interface").getTextContent());

	}

	/**
	 * Test to see if we use defaults we get the old version of the namespace
	 */

	@Component(name = "vcomp", provide = {})
	static class OldVersion {

		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Deactivate
		protected void deactivate(@SuppressWarnings("unused") ComponentContext cc) {}

		@Reference
		protected void bindLog(@SuppressWarnings("unused") LogService log) {

		}
	}

	/**
	 * Test if an activate/deactivate method that has wrong prototype
	 */

	@Component(name = "wacomp", provide = {})
	static class ActivateWithWrongArguments {

		@Activate
		// Is not allowed, must give an error
		protected void whatever(@SuppressWarnings("unused") String x) {}

	}

	public static void testActivateWithActivateWithWrongArguments() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.ActivateWithWrongArguments");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(2, b.getErrors().size()); //same error detected twice
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "wacomp");
		assertAttribute(doc, "", "scr:component/@activate"); //validation removes the non-existent method.
	}

	/**
	 * Test if an activate/deactivate method can have multiple args
	 */

	@Component(name = "amcomp", provide = {})
	static class ActivateWithMultipleArguments {

		@Activate
		protected void whatever(@SuppressWarnings("unused") Map< ? , ? > map, @SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") BundleContext bc, @SuppressWarnings("unused") Map< ? , ? > x) {}

	}

	public static void testActivateWithMultipleArguments() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.ActivateWithMultipleArguments");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "amcomp");
		assertAttribute(doc, "whatever", "scr:component/@activate");
	}

	/**
	 * Test components with references that have multiple arguments.
	 */
	@Component(name = "mcomp", provide = {})
	static class MultipleArguments {

		@Reference
		protected void bindWithMap(@SuppressWarnings("unused") LogService log, @SuppressWarnings("unused") Map< ? , ? > map) {}

	}

	@Component(name = "rcomp", provide = {})
	static class ReferenceArgument {

		@Reference(service = LogService.class)
		protected void bindReference(@SuppressWarnings("unused") ServiceReference ref) {}

	}

	public static void testMultipleArguments() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.(MultipleArguments|ReferenceArgument)");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "mcomp");
		assertAttribute(doc, "bindWithMap", "scr:component/reference/@bind");
		assertAttribute(doc, "org.osgi.service.log.LogService", "scr:component/reference/@interface");

		doc = doc(b, "rcomp");
		assertAttribute(doc, "bindReference", "scr:component/reference/@bind");
		assertAttribute(doc, "org.osgi.service.log.LogService", "scr:component/reference/@interface");
	}

	/**
	 * Test components with weird bind methods.
	 */
	@Component(name = "xcomp", provide = {})
	static class TypeVersusDetailed {

		@Reference(type = '*')
		protected void bind(@SuppressWarnings("unused") LogService log) {}
		protected void unbind(@SuppressWarnings("unused") LogService log) {}

		@Reference(multiple = true, optional = true, dynamic = true)
		protected void bind2(@SuppressWarnings("unused") LogService log) {}
		protected void unbind2(@SuppressWarnings("unused") LogService log) {}
	}

	public static void testTypeVersusDetailed() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.TypeVersusDetailed");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "xcomp");
		print(doc, "");
		assertAttribute(doc, "bind2", "component/reference[1]/@bind");
		assertAttribute(doc, "dynamic", "component/reference[1]/@policy");
		assertAttribute(doc, "0..n", "component/reference[1]/@cardinality");
		assertAttribute(doc, "bind", "component/reference[2]/@bind");
		assertAttribute(doc, "dynamic", "component/reference[2]/@policy");
		assertAttribute(doc, "0..n", "component/reference[2]/@cardinality");
	}

	/**
	 * Test components with weird bind methods.
	 */
	@Component(name = "acomp", provide = {})
	static class MyComponent4 {
		@Activate
		protected void xyz() {}
	}

	public static void testAnnotationsNamespaceVersion() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.MyComponent4");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "acomp");
		System.err.println(doc.getDocumentElement().getNamespaceURI());
	}

	/**
	 * Test components with weird bind methods.
	 */
	@Component(name = "acomp", configurationPolicy = ConfigurationPolicy.require)
	static class MyComponent2 {
		@Reference
		protected void addLogMultiple(@SuppressWarnings("unused") LogService log) {

		}
	}

	public static void testAnnotationsStrangeBindMethods() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.MyComponent2");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "acomp");
		assertEquals("addLogMultiple", xpath.evaluate("//@bind", doc));
		assertEquals("", xpath.evaluate("//@unbind", doc));

		assertAttribute(doc, "logMultiple", "scr:component/reference[1]/@name");
		assertAttribute(doc, "addLogMultiple", "scr:component/reference[1]/@bind");

	}

	/**
	 * Test setting the unbind method
	 */
	@Component(name = "acomp", configurationPolicy = ConfigurationPolicy.ignore)
	static class MyComponent3 {
		@Reference(unbind = "destroyX")
		protected void putX(@SuppressWarnings("unused") LogService log) {

		}
	}

	public static void testAnnotationsSettingUnbind() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.MyComponent3");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(1, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "acomp");
		assertAttribute(doc, "putx", "scr:component/reference[1]/@name");
		assertAttribute(doc, "putX", "scr:component/reference[1]/@bind");
		assertAttribute(doc, "destroyX", "scr:component/reference[1]/@unbind");
	}

	/**
	 * Test some more components
	 * 
	 * @author aqute
	 */
	@Component(name = "acomp", enabled = true, factory = "abc", immediate = false, provide = LogService.class, servicefactory = true, configurationPolicy = ConfigurationPolicy.optional)
	static class MyComponent implements Serializable {
		private static final long	serialVersionUID	= 1L;
		LogService					log;

		@Activate
		protected void activatex() {}

		@Deactivate
		protected void deactivatex() {}

		@Modified
		protected void modifiedx() {}

		@Reference(type = '~', target = "(abc=3)")
		protected void setLog(LogService log) {
			this.log = log;
		}

		protected void unsetLog(@SuppressWarnings("unused") LogService log) {
			this.log = null;
		}

	}

	public static void testAnnotations() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			new File("bin")
		});
		b.setProperty("Service-Component", "*.MyComponent");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "acomp");
		print(doc, "");
		assertAttribute(doc, "test.component.BNDAnnotationTest$MyComponent", "scr:component/implementation/@class");
		assertAttribute(doc, "acomp", "scr:component/@name");
		assertAttribute(doc, "abc", "scr:component/@factory");
		assertAttribute(doc, "true", "scr:component/service/@servicefactory");
		assertAttribute(doc, "activatex", "scr:component/@activate");
		assertAttribute(doc, "modifiedx", "scr:component/@modified");
		assertAttribute(doc, "deactivatex", "scr:component/@deactivate");
		assertAttribute(doc, "org.osgi.service.log.LogService", "scr:component/service/provide/@interface");
		assertAttribute(doc, "(abc=3)", "scr:component/reference/@target");
		assertAttribute(doc, "setLog", "scr:component/reference/@bind");
		assertAttribute(doc, "unsetLog", "scr:component/reference/@unbind");
		assertAttribute(doc, "0..1", "scr:component/reference/@cardinality");
	}

	public static void assertAttribute(Document doc, String value, String expr) throws XPathExpressionException {
		System.err.println(expr);
		String o = (String) xpath.evaluate(expr, doc, XPathConstants.STRING);
		if (o == null) {

		}
		assertNotNull(o);
		assertEquals(value, o);
	}

	static Document doc(Builder b, String name) throws Exception {
		Jar jar = b.getJar();
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		assertNotNull(r);
		Document doc = db.parse(r.openInputStream());
		r.write(System.err);
		return doc;
	}

	private static void print(Node doc, String indent) {
		System.err.println(indent + doc);
		NamedNodeMap attributes = doc.getAttributes();
		if (attributes != null)
			for (int i = 0; i < attributes.getLength(); i++) {
				print(attributes.item(i), indent + "  ");
			}
		NodeList nl = doc.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			print(nl.item(i), indent + "  ");
		}
	}

}
