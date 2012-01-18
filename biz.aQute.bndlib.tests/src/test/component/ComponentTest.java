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
import org.xml.sax.*;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Constants;

public class ComponentTest extends TestCase {
	final DocumentBuilderFactory	dbf		= DocumentBuilderFactory.newInstance();
	final XPathFactory				xpathf	= XPathFactory.newInstance();
	final XPath						xpath	= xpathf.newXPath();
	DocumentBuilder					db;

	{
		try {
			dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
			xpath.setNamespaceContext(new NamespaceContext() {

				public Iterator getPrefixes(String namespaceURI) {
					return Arrays.asList("md", "scr").iterator();
				}

				public String getPrefix(String namespaceURI) {
					if (namespaceURI.equals("http://www.osgi.org/xmlns/metatype/v1.1.0"))
						return "md";
					if (namespaceURI.equals("http://www.osgi.org/xmlns/scr/v1.1.0"))
						return "scr";

					return null;
				}

				public String getNamespaceURI(String prefix) {
					if (prefix.equals("md"))
						return "http://www.osgi.org/xmlns/metatype/v1.1.0";
					else if (prefix.equals("scr"))
						return "http://www.osgi.org/xmlns/scr/v1.1.0";
					else
						return null;
				}
			});
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Can we order the references? References are ordered by their name as Java does not
	 * define the order of the methods.
	 * 
	 * @throws Exception 
	 */
	
	@Component static class TestReferenceOrdering {
		
		@Reference(service=LogService.class)
		void setA(ServiceReference ref) {}
		@Reference
		void setC(LogService log) {}
		@Reference(service=LogService.class)
		void setB(Map<String,Object> map) {}
	}
	
	public void testReferenceOrdering () throws Exception {
		Builder b = new Builder();
		b.addClasspath( new File( "bin"));
		b.setProperty("Service-Component", "*TestReferenceOrdering");
		b.setProperty("Private-Package", "test.component");
		b.build();
		Document doc = doc(b, "test.component.ComponentTest$TestReferenceOrdering");
		NodeList nodes = (NodeList) xpath.evaluate("//reference", doc, XPathConstants.NODESET);
		assertEquals("a", nodes.item(0).getAttributes().getNamedItem("name").getTextContent());
		assertEquals("b", nodes.item(1).getAttributes().getNamedItem("name").getTextContent());
		assertEquals("c", nodes.item(2).getAttributes().getNamedItem("name").getTextContent());
	}

	
	
	/**
	 * Test to see if we ignore scala.ScalaObject as interface
	 * @throws Exception 
	 */
	public void testScalaObject () throws Exception {
		Builder b = new Builder();
		b.addClasspath( new File( "jar/com.test.scala.jar"));
		b.setProperty("Service-Component", "*");
		b.setProperty("Export-Package", "com.test.scala.*");
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		System.out.println(Processor.join(b.getErrors()));
		System.out.println(Processor.join(b.getWarnings()));
		System.out.println(m.getMainAttributes().getValue("Service-Component"));
		IO.copy(jar.getResource("OSGI-INF/com.test.scala.Service.xml").openInputStream(), System.out);
		Document doc = doc(b, "com.test.scala.Service");
		assertEquals("com.test.scala.Service", xpath.evaluate("component/implementation/@class", doc));
		assertEquals("", xpath.evaluate("component/service/provide/@interface", doc));
		
	}
	
	
	
	
	/**
	 * Test config with metatype
	 */

	@Component(name = "config", designateFactory = Config.class, configurationPolicy = ConfigurationPolicy.require) static class MetatypeConfig {
		interface Config {
			String name();
		}
	}

	@Component(designate = Config.class) static class MetatypeConfig2 {
		interface Config {
			String name();
		}
	}

	public void testConfig() throws Exception {
		Builder b = new Builder();
		b.setExceptions(true);
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*MetatypeConfig*");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		System.out.println(b.getJar().getResources().keySet());

		// Check component name
		{
			Resource cr = b.getJar().getResource("OSGI-INF/config.xml");
			cr.write(System.out);
			Document d = db.parse(cr.openInputStream());
			assertEquals("config", xpath.evaluate("/scr:component/@name", d, XPathConstants.STRING));
		}

		// Check if config properly linked
		{
			Resource mr = b.getJar().getResource("OSGI-INF/metatype/config.xml");
			mr.write(System.out);
			Document d = db.parse(mr.openInputStream());
			assertEquals("config", xpath.evaluate("//Designate/@factoryPid", d,
					XPathConstants.STRING));
			assertEquals("config", xpath.evaluate("//Object/@ocdref", d,
					XPathConstants.STRING));
		}

		// Now with default name
		{
			Resource cr2 = b.getJar()
					.getResource("OSGI-INF/test.component.ComponentTest$MetatypeConfig2.xml");
			cr2.write(System.out);
			Document d = db.parse(cr2.openInputStream());
			assertEquals("test.component.ComponentTest$MetatypeConfig2", xpath.evaluate("//scr:component/@name",
					d, XPathConstants.STRING));
		}
		{
			Resource mr2 = b.getJar().getResource(
					"OSGI-INF/metatype/test.component.ComponentTest$MetatypeConfig2.xml");
			mr2.write(System.out);
			Document d = db.parse(mr2.openInputStream());
			assertEquals("test.component.ComponentTest$MetatypeConfig2", xpath.evaluate(
					"//Designate/@pid", d, XPathConstants.STRING));
			assertEquals("test.component.ComponentTest$MetatypeConfig2", xpath.evaluate("//Object/@ocdref", d,
					XPathConstants.STRING));
		}
	}

	/**
	 * Test properties
	 */
	@Meta.OCD static interface Config {
		String name();
	}

	@Component(name = "props", properties = { "a=1", "b=3", "c=1|2|3" }, designate = Config.class, designateFactory = Config.class) static class PropertiesAndConfig {
		@Activate protected void activate(ComponentContext c) {
		}
	}

	public void testPropertiesAndConfig() throws Exception {
		Builder b = new Builder();
		b.setExceptions(true);
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*PropertiesAndConfig");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Resource cr = b.getJar().getResource("OSGI-INF/props.xml");
		cr.write(System.out);
		{
			Document doc = doc(b, "props");
			assertEquals("1", xpath.evaluate("scr:component/property[@name='a']/@value", doc));
			assertEquals("3", xpath.evaluate("scr:component/property[@name='b']/@value", doc));
			assertEquals("\n1\n2\n3\n", xpath.evaluate("scr:component/property[@name='c']", doc));
		}

	}

	/**
	 * Test if a reference is made to an interface implemented on a superclass.
	 * 
	 * This is from https://github.com/bndtools/bnd/issues#issue/23
	 */

	public void testProvideFromSuperClass() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*InheritedActivator");
		b.setProperty("Private-Package", "test.activator.inherits");
		b.addClasspath(new File("jar/osgi.jar"));
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Manifest m = b.getJar().getManifest();
		String imports = m.getMainAttributes().getValue("Import-Package");
		assertTrue(imports.contains("org.osgi.framework"));
	}

	/**
	 * Test if a package private method gives us 1.1 + namespace in the XML
	 */

	@Component(name = "packageprotected") static class PackageProtectedActivateMethod {
		@Activate protected void activate(ComponentContext c) {
		}
	}

	@Component(name = "packageprivate") static class PackagePrivateActivateMethod {
		@Activate void activate(ComponentContext c) {
		}
	}

	public void testPackagePrivateActivateMethod() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*Package*ActivateMethod");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		{
			Document doc = doc(b, "packageprotected");
			Object o = xpath.evaluate("scr:component", doc, XPathConstants.NODE);
			assertNotNull(o);
		}
		{
			Resource r = b.getJar().getResource("OSGI-INF/packageprivate.xml");
			r.write(System.out);
			assertNotNull(r);
			Document doc = db.parse(r.openInputStream());
			assertNotNull(xpath.evaluate("//scr:component/@name", doc, XPathConstants.STRING));
		}

	}

	/**
	 * Test an attribute on an annotation
	 */

	@Component(name = "annotated") static class Annotated {

		@Reference protected void setLog(LogService log) {
		}

	}

	public void testAnnotatedWithAttribute() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*NoUnbind;log=org.osgi.service.log.LogService");
		b.setProperty("Private-Package", "test.component");
		Jar jar = b.build();
		Manifest manifest = jar.getManifest();
		String sc = manifest.getMainAttributes().getValue(Constants.SERVICE_COMPONENT);
		assertFalse(sc.contains(";"));
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

	}

	/**
	 * A non-FQN entry but we demand no annotations, should generate an error
	 * and no component
	 */

	public void testNonFQNAndNoAnnotations() throws Exception {
		Builder b = new Builder();
		b
				.setProperty("Include-Resource",
						"org/osgi/impl/service/coordinator/AnnotationWithJSR14.class=jar/AnnotationWithJSR14.jclass");
		b.setProperty("Service-Component", "*;" + Constants.NOANNOTATIONS + "=true");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(1, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Manifest manifest = jar.getManifest();
		String component = manifest.getMainAttributes().getValue("Service-Component");
		System.out.println(component);
		assertNull(component);
	}

	/**
	 * Imported default package because JSR14 seems to do something weird with
	 * annotations.
	 * 
	 * @throws Exception
	 */
	public void testJSR14ComponentAnnotations() throws Exception {
		Builder b = new Builder();
		b
				.setProperty("Include-Resource",
						"org/osgi/impl/service/coordinator/AnnotationWithJSR14.class=jar/AnnotationWithJSR14.jclass");
		b.setProperty("Service-Component", "*");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(1, b.getWarnings().size());

		Manifest manifest = jar.getManifest();
		String component = manifest.getMainAttributes().getValue("Service-Component");
		System.out.println(component);
		assertNull(component);
	}

	@Component(name = "nounbind") static class NoUnbind {

		@Reference protected void setLog(LogService log) {
		}

	}

	public void testNoUnbind() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*NoUnbind");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "nounbind");
		assertEquals("setLog", xpath.evaluate("component/reference/@bind", doc));
		assertEquals("", xpath.evaluate("component/reference/@unbind", doc));
	}

	@Component(name = "explicitunbind") static class ExplicitUnbind {

		@Reference(unbind = "killLog") protected void setLog(LogService log) {
		}

	}

	public void testExplicitUnbind() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*ExplicitUnbind");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
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

	@Component(name = "ncomp", provide = {}) static class NewActivateVersion {

		@Activate protected void activate(ComponentContext context) {
		}

	}

	@Component(name = "ndcomp", provide = {}) static class NewDeActivateVersion {
		@Deactivate protected void deactivate() {
		}

	}

	@Component(name = "nbcomp", provide = {}) static class NewBindVersion {

		@Reference protected void bind(ServiceReference ref, Map<String, Object> map) {
		}

	}

	public void testNewVersion() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*Version");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		{
			Document doc = doc(b, "ncomp");
			Node o = (Node) xpath.evaluate("scr:component", doc, XPathConstants.NODE);
			assertNotNull("Expected ncomp to have old namespace", o);
		}

	}

	/**
	 * Test the same bind method names
	 */

	@Component(name = "cpcomp") static class SameRefName {

		@Reference protected void bind(LogService log) {

		}

		@Reference protected void bind(EventAdmin event) {

		}
	}

	public void testSameRefName() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.SameRefName");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(1, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		// Document doc = doc(b, "cpcomp");
	}

	/**
	 * Test the configuration policy
	 */

	@Component(name = "cpcomp", configurationPolicy = ConfigurationPolicy.require, provide = {
			Serializable.class, EventAdmin.class }) static class ConfigurationPolicyTest {

	}

	public void testConfigurationPolicy() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.ConfigurationPolicyTest");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "cpcomp");
		assertEquals("java.io.Serializable", doc.getElementsByTagName("provide").item(0)
				.getAttributes().getNamedItem("interface").getTextContent());
		assertEquals("org.osgi.service.event.EventAdmin", doc.getElementsByTagName("provide").item(
				1).getAttributes().getNamedItem("interface").getTextContent());

	}

	/**
	 * Test to see if we use defaults we get the old version of the namespace
	 */

	@Component(name = "vcomp", provide = {}) static class OldVersion {

		@Activate protected void activate(ComponentContext cc) {
		}

		@Deactivate protected void deactivate(ComponentContext cc) {
		}

		@Reference protected void bindLog(LogService log) {

		}
	}


	/**
	 * Test if an activate/deactivate method that has wrong prototype
	 */

	@Component(name = "wacomp", provide = {}) static class ActivateWithWrongArguments {

		@Activate// Is not allowed, must give an error
		protected void whatever(String x) {
		}

	}

	public void testActivateWithActivateWithWrongArguments() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.ActivateWithWrongArguments");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(1, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "wacomp");
		assertEquals("whatever", xpath.evaluate("scr:component/@activate",doc,XPathConstants.STRING));
		assertAttribute(doc, "whatever", "scr:component/@activate");
	}

	/**
	 * Test if an activate/deactivate method can have multiple args
	 */

	@Component(name = "amcomp", provide = {}) static class ActivateWithMultipleArguments {

		@Activate protected void whatever(Map<?, ?> map, ComponentContext cc, BundleContext bc,
				Map<?, ?> x) {
		}

	}

	public void testActivateWithMultipleArguments() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.ActivateWithMultipleArguments");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "amcomp");
		assertAttribute(doc, "whatever", "scr:component/@activate");
	}

	/**
	 * Test components with references that have multiple arguments.
	 */
	@Component(name = "mcomp", provide = {}) static class MultipleArguments {

		@Reference protected void bindWithMap(LogService log, Map<?, ?> map) {
		}

	}

	@Component(name = "rcomp", provide = {}) static class ReferenceArgument {

		@Reference(service = LogService.class) protected void bindReference(ServiceReference ref) {
		}

	}

	public void testMultipleArguments() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.(MultipleArguments|ReferenceArgument)");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "mcomp");
		assertAttribute(doc, "bindWithMap", "scr:component/reference/@bind");
		assertAttribute(doc, "org.osgi.service.log.LogService",
				"scr:component/reference/@interface");

		doc = doc(b, "rcomp");
		assertAttribute(doc, "bindReference", "scr:component/reference/@bind");
		assertAttribute(doc, "org.osgi.service.log.LogService",
				"scr:component/reference/@interface");
	}

	/**
	 * Test components with weird bind methods.
	 */
	@Component(name = "xcomp", provide = {}) static class TypeVersusDetailed {

		@Reference(type = '*') protected void bind(LogService log) {
		}

		@Reference(multiple = true, optional = true, dynamic = true) protected void bind2(
				LogService log) {
		}
	}

	public void testTypeVersusDetailed() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.TypeVersusDetailed");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
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
	@Component(name = "acomp", provide = {}) static class MyComponent4 {
		@Activate protected void xyz() {
		}
	}

	public void testAnnotationsNamespaceVersion() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.MyComponent4");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "acomp");
		System.out.println(doc.getDocumentElement().getNamespaceURI());
	}

	/**
	 * Test components with weird bind methods.
	 */
	@Component(name = "acomp", configurationPolicy = ConfigurationPolicy.require) static class MyComponent2 {
		@Reference protected void addLogMultiple(LogService log) {

		}
	}

	public void testAnnotationsStrangeBindMethods() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.MyComponent2");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
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
	@Component(name = "acomp", configurationPolicy = ConfigurationPolicy.ignore) static class MyComponent3 {
		@Reference(unbind = "destroyX") protected void putX(LogService log) {

		}
	}

	public void testAnnotationsSettingUnbind() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.MyComponent3");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
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
	 * 
	 */
	@Component(name = "acomp", enabled = true, factory = "abc", immediate = false, provide = LogService.class, servicefactory = true, configurationPolicy = ConfigurationPolicy.optional) static class MyComponent
			implements Serializable {
		private static final long	serialVersionUID	= 1L;
		LogService					log;

		@Activate protected void activatex() {
		}

		@Deactivate protected void deactivatex() {
		}

		@Modified protected void modifiedx() {
		}

		@Reference(type = '~', target = "(abc=3)") protected void setLog(LogService log) {
			this.log = log;
		}

		protected void unsetLog(LogService log) {
			this.log = null;
		}

	}

	public void testAnnotations() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin") });
		b.setProperty("Service-Component", "*.MyComponent");
		b.setProperty("Private-Package", "test.component");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Document doc = doc(b, "acomp");
		print(doc, "");
		assertAttribute(doc, "test.component.ComponentTest$MyComponent",
				"scr:component/implementation/@class");
		assertAttribute(doc, "acomp", "scr:component/@name");
		assertAttribute(doc, "abc", "scr:component/@factory");
		assertAttribute(doc, "true", "scr:component/service/@servicefactory");
		assertAttribute(doc, "activatex", "scr:component/@activate");
		assertAttribute(doc, "modifiedx", "scr:component/@modified");
		assertAttribute(doc, "deactivatex", "scr:component/@deactivate");
		assertAttribute(doc, "org.osgi.service.log.LogService",
				"scr:component/service/provide/@interface");
		assertAttribute(doc, "(abc=3)", "scr:component/reference/@target");
		assertAttribute(doc, "setLog", "scr:component/reference/@bind");
		assertAttribute(doc, "unsetLog", "scr:component/reference/@unbind");
		assertAttribute(doc, "0..1", "scr:component/reference/@cardinality");
	}

	public void assertAttribute(Document doc, String value, String expr)
			throws XPathExpressionException {
		System.out.println(expr);
		String o = (String) xpath.evaluate(expr, doc, XPathConstants.STRING);
		if ( o == null ) {
			
		}
		assertNotNull(o);
		assertEquals(value, o);
	}

	Document doc(Builder b, String name) throws Exception {
		Jar jar = b.getJar();
		Resource r = jar.getResource("OSGI-INF/" + name + ".xml");
		assertNotNull(r);
		Document doc = db.parse(r.openInputStream());
		r.write(System.out);
		return doc;
	}

	public void testV1_1Directives() throws Exception {
		Element component = setup("test.activator.Activator;factory:=blabla;immediate:=true;enabled:=false;configuration-policy:=optional;activate:=start;deactivate:=stop;modified:=modified");
		assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component.getNamespaceURI());
		assertEquals("blabla", component.getAttribute("factory"));
		assertEquals("false", component.getAttribute("enabled"));
		assertEquals("optional", component.getAttribute("configuration-policy"));
		assertEquals("start", component.getAttribute("activate"));
		assertEquals("stop", component.getAttribute("deactivate"));
		assertEquals("modified", component.getAttribute("modified"));

	}

	public void testNoNamespace() throws Exception {
		Element component = setup("test.activator.Activator");
		assertEquals(null, component.getNamespaceURI());
	}

	public void testAutoNamespace() throws Exception {
		Element component = setup("test.activator.Activator;activate:='start';deactivate:='stop'");
		assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component.getNamespaceURI());

	}

	public void testCustomNamespace() throws Exception {
		Element component = setup("test.activator.Activator;version:=2");
		assertEquals("http://www.osgi.org/xmlns/scr/v2.0.0", component.getNamespaceURI());

	}

	Element setup(String header) throws Exception {
		Builder b = new Builder();
		b.setProperty(Analyzer.SERVICE_COMPONENT, header);
		b.setClasspath(new File[] { new File("bin"), new File("jar/osgi.jar") });
		b.setProperty("Private-Package", "test.activator, org.osgi.service.http.*");
		b.build();

		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		print(b.getJar().getResource("OSGI-INF/test.activator.Activator.xml"), System.out);
		Document doc = db.parse(new InputSource(b.getJar().getResource(
				"OSGI-INF/test.activator.Activator.xml").openInputStream()));

		return doc.getDocumentElement();
	}

	private void print(Resource resource, OutputStream out) throws Exception {
		InputStream in = resource.openInputStream();
		try {
			byte[] buffer = new byte[1024];
			int size = in.read(buffer);
			while (size > 0) {
				out.write(buffer, 0, size);
				size = in.read(buffer);
			}
			out.flush();
		} finally {
			in.close();
		}
	}

	/*
	 * public void testWildcards() throws Exception { Builder b = new Builder();
	 * b .setProperty(Analyzer.SERVICE_COMPONENT, "test/component/*.xml");
	 * b.setProperty("-resourceonly", "true"); b.setProperty("Include-Resource",
	 * "test/component=test/component"); Jar jar = b.build();
	 * System.out.println(b.getErrors()); System.out.println(b.getWarnings());
	 * assertEquals(0, b.getErrors().size()); assertEquals(0,
	 * b.getWarnings().size()); }
	 */
	public void testImplementation() throws Exception {
		Builder b = new Builder();
		b
				.setProperty(
						Analyzer.SERVICE_COMPONENT,
						"silly.name;implementation:=test.activator.Activator;provide:=java.io.Serialization;servicefactory:=true");
		b.setClasspath(new File[] { new File("bin"), new File("jar/osgi.jar") });
		b.setProperty("Private-Package", "test.activator");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Jar jar = b.getJar();

		Document doc = db.parse(new InputSource(jar.getResource("OSGI-INF/silly.name.xml")
				.openInputStream()));

		assertEquals("test.activator.Activator", doc.getElementsByTagName("implementation").item(0)
				.getAttributes().getNamedItem("class").getNodeValue());
		assertEquals("true", doc.getElementsByTagName("service").item(0).getAttributes()
				.getNamedItem("servicefactory").getNodeValue());
	}

	/**
	 * Standard activator with reference to http.
	 * 
	 * @throws Exception
	 */
	public void testProperties() throws Exception {
		java.util.Properties p = new Properties();
		p.put(Analyzer.EXPORT_PACKAGE, "test.activator,org.osgi.service.http");
		p.put(Analyzer.IMPORT_PACKAGE, "*");
		p.put(Analyzer.SERVICE_COMPONENT, "test.activator.Activator;properties:=\"a=3|4,b=1|2|3\"");
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin"), new File("jar/osgi.jar") });
		b.setProperties(p);
		b.build();
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		Jar jar = b.getJar();

		Document doc = db.parse(new InputSource(jar.getResource(
				"OSGI-INF/test.activator.Activator.xml").openInputStream()));

		NodeList l = doc.getElementsByTagName("property");
		assertEquals(2, l.getLength());
		Node n = l.item(0);
		System.out.println(n.getFirstChild().getNodeValue());
		assertEquals("3\n4", l.item(0).getFirstChild().getNodeValue().trim());
		assertEquals("1\n2\n3", l.item(1).getFirstChild().getNodeValue().trim());

		assertEquals("test.activator.Activator", doc.getElementsByTagName("implementation").item(0)
				.getAttributes().getNamedItem("class").getNodeValue());
		// assertEquals("test.activator.Activator", xp.evaluate(
		// "/component/implementation/@class", doc));
		// assertEquals("org.osgi.service.http.HttpService", xp.evaluate(
		// "/component/reference[@name='http']/@interface", doc));
		// assertEquals("setHttp", xp.evaluate(
		// "/component/reference[@name='http']/@bind", doc));
		// assertEquals("unsetHttp", xp.evaluate(
		// "/component/reference[@name='http']/@unbind", doc));
		// assertEquals("", xp.evaluate(
		// "/component/reference[@name='http']/@target", doc));
	}

	/**
	 * Check if all the directives work
	 * 
	 * @throws Exception
	 */
	public void testUnknownDirective() throws Exception {
		java.util.Properties p = new Properties();
		p.put(Analyzer.EXPORT_PACKAGE, "test.activator,org.osgi.service.http");
		p.put(Analyzer.IMPORT_PACKAGE, "*");
		p.put(Analyzer.SERVICE_COMPONENT, "test.activator.Activator;provides:=true");
		Builder b = new Builder();
		b.setClasspath(new File[] { new File("bin"), new File("jar/osgi.jar") });
		b.setProperties(p);
		b.build();
		assertEquals(1, b.getErrors().size());
		assertTrue(((String) b.getErrors().get(0)).indexOf("Unrecognized directive") >= 0);
		assertEquals(0, b.getWarnings().size());
	}

	/**
	 * Check if all the directives work
	 * 
	 * @throws Exception
	 */
	public void testDirectives() throws Exception {
		// Element component =
		// setup("test.activator.Activator;http=org.osgi.service.http.HttpService;dynamic:=http;optional:=http;provide:=test.activator.Activator; multiple:=http");

		// assertEquals("test.activator.Activator", xp.evaluate(
		// "/component/implementation/@class", doc));
		// assertEquals("org.osgi.service.http.HttpService", xp.evaluate(
		// "/component/reference[@name='http']/@interface", doc));
		// assertEquals("setHttp", xp.evaluate(
		// "/component/reference[@name='http']/@bind", doc));
		// assertEquals("unsetHttp", xp.evaluate(
		// "/component/reference[@name='http']/@unbind", doc));
		// assertEquals("0..n", xp.evaluate(
		// "/component/reference[@name='http']/@cardinality", doc));
		// assertEquals("dynamic", xp.evaluate(
		// "/component/reference[@name='http']/@policy", doc));
		// assertEquals("test.activator.Activator", xp.evaluate(
		// "/component/service/provide/@interface", doc));
	}

	/**
	 * Check if a bad filter on a service component causes an error.
	 * 
	 * @throws Exception
	 */
	public void testBadFilter() throws Exception {
		// java.util.Properties p = new Properties();
		// p.put(Analyzer.EXPORT_PACKAGE,
		// "test.activator,org.osgi.service.http");
		// p.put(Analyzer.IMPORT_PACKAGE, "*");
		// p
		// .put(
		// Analyzer.SERVICE_COMPONENT,
		// "test.activator.Activator;http=\"org.osgi.service.http.HttpService(|p=1)(p=2))\"");
		// Builder b = new Builder();
		// b
		// .setClasspath(new File[] { new File("bin"),
		// new File("jar/osgi.jar") });
		// b.setProperties(p);
		// b.build();
		// assertEquals(1, b.getErrors().size());
		// assertTrue(((String) b.getErrors().get(0))
		// .indexOf("is not a correct filter") >= 0);
		// assertEquals(0, b.getWarnings().size());
	}

	/**
	 * Check if we can set a target filter
	 * 
	 * @throws Exception
	 */
	public void testFilter() throws Exception {
		Element component = setup("test.activator.Activator;http=\"org.osgi.service.http.HttpService(|(p=1)(p=2))\"");
		Element implementation = (Element) component.getElementsByTagName("implementation").item(0);
		assertEquals(null, implementation.getNamespaceURI());
		assertEquals("test.activator.Activator", implementation.getAttribute("class"));

		Element reference = (Element) component.getElementsByTagName("reference").item(0);
		assertEquals("org.osgi.service.http.HttpService", reference.getAttribute("interface"));
		assertEquals("setHttp", reference.getAttribute("bind"));
		assertEquals("unsetHttp", reference.getAttribute("unbind"));
		assertEquals("(|(p=1)(p=2))", reference.getAttribute("target"));
	}

	/**
	 * Standard activator with reference to http.
	 * 
	 * @throws Exception
	 */
	public void testSimple() throws Exception {
		Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService?");
		Element implementation = (Element) component.getElementsByTagName("implementation").item(0);
		assertEquals(null, implementation.getNamespaceURI());
		assertEquals("test.activator.Activator", implementation.getAttribute("class"));

		Element reference = (Element) component.getElementsByTagName("reference").item(0);
		assertEquals("org.osgi.service.http.HttpService", reference.getAttribute("interface"));
		assertEquals("setHttp", reference.getAttribute("bind"));
		assertEquals("unsetHttp", reference.getAttribute("unbind"));
		assertEquals("", reference.getAttribute("target"));
		assertEquals("0..1", reference.getAttribute("cardinality"));
		assertEquals("dynamic", reference.getAttribute("policy"));
	}

	/**
	 * Standard activator with reference to http.
	 * 
	 * @throws Exception
	 */
	public void testQuestion() throws Exception {
		Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService?");
		Element reference = (Element) component.getElementsByTagName("reference").item(0);
		assertEquals("0..1", reference.getAttribute("cardinality"));
		assertEquals("dynamic", reference.getAttribute("policy"));
	}

	public void testStar() throws Exception {
		Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService*");
		Element reference = (Element) component.getElementsByTagName("reference").item(0);
		assertEquals("0..n", reference.getAttribute("cardinality"));
		assertEquals("dynamic", reference.getAttribute("policy"));
	}

	public void testPlus() throws Exception {
		Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService+");
		Element reference = (Element) component.getElementsByTagName("reference").item(0);
		assertEquals("1..n", reference.getAttribute("cardinality"));
		assertEquals("dynamic", reference.getAttribute("policy"));
	}

	public void testTilde() throws Exception {
		Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService~");
		Element reference = (Element) component.getElementsByTagName("reference").item(0);
		assertEquals("0..1", reference.getAttribute("cardinality"));
		assertEquals("", reference.getAttribute("policy"));
	}

	private void print(Node doc, String indent) {
		System.out.println(indent + doc);
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
