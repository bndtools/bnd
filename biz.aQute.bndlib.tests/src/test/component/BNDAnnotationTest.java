package test.component;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.SearchableRepository;
import aQute.bnd.test.BndTestCase;
import junit.framework.AssertionFailedError;

/**
 * Test for use of DS components specified using bnd proprietary annotations.
 */
@SuppressWarnings({
		"resource", "unused", "rawtypes"
})
public class BNDAnnotationTest extends BndTestCase {
	static final DocumentBuilderFactory	dbf		= DocumentBuilderFactory.newInstance();
	static final XPathFactory			xpathf	= XPathFactory.newInstance();
	static final XPath					xpath	= xpathf.newXPath();
	static DocumentBuilder				db;

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
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new ExceptionInInitializerError(e);
		}
	}

	private Builder builder(String spec) throws Exception {
		return builder(spec, 0, 0);
	}

	private Builder builder(String spec, int errors, int warnings) throws IOException, Exception, AssertionFailedError {
		Builder b = new Builder();
		b.setExceptions(true);
		b.setClasspath(new File[] {
				new File("bin")
		});
		b.setProperty("Service-Component", spec);
		b.setProperty("Private-Package", "test.component");
		b.setProperty("-dsannotations", "");
		b.setProperty("-fixupmessages.bndannodeprecated", "Bnd DS annotations are deprecated");
		b.build();
		assertOk(b, errors, warnings);
		return b;
	}

	/**
	 * Whitespace in Component name causes Component initialization to fail #548
	 */

	@Component(name = "Hello World Bnd ^ % / \\ $")
	static class BrokenNameDS {

	}

	@Component(name = "Hello World")
	static class BrokenNameBnd {

	}

	public void testBrokenName() throws Exception {
		Builder b = builder("*Broken*", 0, 2);
		Jar build = b.getJar();
		assertTrue(b.check("Invalid component name"));

		Domain m = Domain.domain(build.getManifest());

		Parameters parameters = m.getParameters("Service-Component");
		assertEquals(2, parameters.size());

		System.out.println(parameters);
		assertTrue(parameters.keySet().contains("OSGI-INF/Hello-World-Bnd---------$.xml"));
		assertTrue(parameters.keySet().contains("OSGI-INF/Hello-World.xml"));
	}

	/**
	 * Can we order the references? References are ordered by their name as Java
	 * does not define the order of the methods.
	 * 
	 */

	@Component
	static class TestReferenceOrdering {

		@Reference(service = LogService.class)
		void setA(@SuppressWarnings("unused") ServiceReference< ? > ref) {}

		@Reference
		void setC(@SuppressWarnings("unused") LogService log) {}

		@Reference(service = LogService.class)
		void setB(@SuppressWarnings("unused") Map<String,Object> map) {}
	}

	public void testAnnotationReferenceOrdering() throws Exception {
		Builder b = builder("*TestReferenceOrdering");
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

	@Component(designate = RepositoryPlugin.class, designateFactory = SearchableRepository.class)
	static class MetatypeConfig3 {}

	@Component(designateFactory = SearchableRepository.class)
	static class MetatypeConfig4 {}

	public void testConfig() throws Exception {
		Builder b = builder("*MetatypeConfig*");
		assertTrue(b.check());
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
			Resource mr2 = b.getJar()
					.getResource("OSGI-INF/metatype/test.component.BNDAnnotationTest$MetatypeConfig2.xml");
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
			" a =1", "b=3", "c=1|2|3", "d=1|"
	}, designate = Config.class, designateFactory = Config.class)
	static class PropertiesAndConfig {
		@Activate
		protected void activate(@SuppressWarnings("unused") ComponentContext c) {}
	}

	public void testPropertiesAndConfig() throws Exception {
		Builder b = builder("*PropertiesAndConfig");

		Resource cr = b.getJar().getResource("OSGI-INF/props.xml");
		cr.write(System.err);
		{
			Document doc = doc(b, "props");
			assertEquals("1", xpath.evaluate("scr:component/property[@name='a']/@value", doc));
			assertEquals("3", xpath.evaluate("scr:component/property[@name='b']/@value", doc));
			assertEquals("1\n2\n3", xpath.evaluate("scr:component/property[@name='c']", doc).trim());
			assertEquals("1", xpath.evaluate("scr:component/property[@name='d']", doc).trim());
		}

	}

	/**
	 * Test if a package private method gives us 1.1 + namespace in the XML
	 * using bnd annotations
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

	public void testPackagePrivateActivateMethodBndAnnos() throws Exception {
		Builder b = builder("*ActivateMethod");
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

	public void testAnnotatedWithAttribute() throws Exception {
		Builder b = builder("*NoUnbind;log=org.osgi.service.log.LogService");
		Jar jar = b.getJar();
		Manifest manifest = jar.getManifest();
		String sc = manifest.getMainAttributes().getValue(Constants.SERVICE_COMPONENT);
		assertFalse(sc.contains(";"));

	}

	/**
	 * Imported default package because JSR14 seems to do something weird with
	 * annotations.
	 * 
	 * @throws Exception
	 */
	public void testJSR14ComponentAnnotations() throws Exception {
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

	public void testNoUnbind() throws Exception {
		Builder b = builder("*NoUnbind");

		Document doc = doc(b, "nounbind");
		assertEquals("setLog", xpath.evaluate("component/reference/@bind", doc));
		assertEquals("", xpath.evaluate("component/reference/@unbind", doc));
	}

	@Component(name = "nounbind_dynamic")
	static class NoUnbindDynamic {

		@Reference(dynamic = true)
		protected void setLog(@SuppressWarnings("unused") LogService log) {}
	}

	public void testNoUnbindDynamic() throws Exception {
		Builder b = builder("*NoUnbindDynamic", 1, 0);
		assertTrue(b.getErrors().get(0).endsWith("dynamic but has no unbind method."));
	}

	// this is a bnd annotation not a DS annotation
	@Component(name = "explicitunbind")
	static class ExplicitUnbind {

		@Reference(unbind = "killLog")
		protected void setLog(@SuppressWarnings("unused") LogService log) {}

	}

	public void testExplicitUnbind() throws Exception {
		Builder b = builder("*ExplicitUnbind", 1, 0);

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

		@SuppressWarnings("rawtypes")
		@Reference
		protected void bind(ServiceReference ref, Map<String,Object> map) {}

	}

	public void testNewVersion() throws Exception {
		Builder b = builder("*Version");
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

	public void testSameRefName() throws Exception {
		Builder b = builder("*.SameRefName", 1, 0);

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

	public void testConfigurationPolicy() throws Exception {
		Builder b = builder("*.ConfigurationPolicyTest");

		Document doc = doc(b, "cpcomp");
		assertEquals("java.io.Serializable",
				doc.getElementsByTagName("provide").item(0).getAttributes().getNamedItem("interface").getTextContent());
		assertEquals("org.osgi.service.event.EventAdmin",
				doc.getElementsByTagName("provide").item(1).getAttributes().getNamedItem("interface").getTextContent());

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

	public void testActivateWithActivateWithWrongArguments() throws Exception {
		Builder b = builder("*.ActivateWithWrongArguments", 2, 0);// same error
																	// detected
																	// twice

		Document doc = doc(b, "wacomp");
		assertAttribute(doc, "", "scr:component/@activate"); // validation
																// removes the
																// non-existent
																// method.
	}

	/**
	 * Test if an activate/deactivate method can have multiple args
	 */

	@Component(name = "amcomp", provide = {})
	static class ActivateWithMultipleArguments {

		@Activate
		protected void whatever(@SuppressWarnings("unused") Map< ? , ? > map,
				@SuppressWarnings("unused") ComponentContext cc, @SuppressWarnings("unused") BundleContext bc,
				@SuppressWarnings("unused") Map< ? , ? > x) {}

	}

	public void testActivateWithMultipleArguments() throws Exception {
		Builder b = builder("*.ActivateWithMultipleArguments");

		Document doc = doc(b, "amcomp");
		assertAttribute(doc, "whatever", "scr:component/@activate");
	}

	/**
	 * Test components with references that have multiple arguments.
	 */
	@Component(name = "mcomp", provide = {})
	static class MultipleArguments {

		@Reference
		protected void bindWithMap(@SuppressWarnings("unused") LogService log,
				@SuppressWarnings("unused") Map< ? , ? > map) {}

	}

	@Component(name = "rcomp", provide = {})
	static class ReferenceArgument {

		@Reference(service = LogService.class)
		protected void bindReference(@SuppressWarnings("unused") ServiceReference ref) {}

	}

	public void testMultipleArguments() throws Exception {
		Builder b = builder("*.(MultipleArguments|ReferenceArgument)");

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

	public void testTypeVersusDetailed() throws Exception {
		Builder b = builder("*.TypeVersusDetailed");

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

	public void testAnnotationsNamespaceVersion() throws Exception {
		Builder b = builder("*MyComponent4");

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

	public void testAnnotationsStrangeBindMethods() throws Exception {
		Builder b = builder("*MyComponent2");

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

	public void testAnnotationsSettingUnbind() throws Exception {
		Builder b = builder("*MyComponent3", 1, 0);

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

	public void testAnnotations() throws Exception {
		Builder b = builder("*MyComponent");

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

	@Component(name = "mixed-bnd-std")
	static class MixedBndStd {

		@org.osgi.service.component.annotations.Reference
		EventAdmin ea;

		@org.osgi.service.component.annotations.Reference
		protected void setLog(@SuppressWarnings("unused") LogService log) {}

		@org.osgi.service.component.annotations.Activate
		void start() {}

		@org.osgi.service.component.annotations.Modified
		void update(Map<String,Object> map) {}

		@org.osgi.service.component.annotations.Deactivate
		void stop() {}
	}

	public void testMixedBndStandard() throws Exception {
		Builder b = builder("*MixedBndStd", 5, 0);
		List<String> errors = new ArrayList<String>(b.getErrors());
		Collections.sort(errors);
		assertEquals(
				"The DS component mixed-bnd-std uses bnd annotations to declare it as a component, but also uses the standard DS annotation: org.osgi.service.component.annotations.Activate on method start with signature ()V. It is an error to mix these two types of annotations",
				errors.get(0));
		assertEquals(
				"The DS component mixed-bnd-std uses bnd annotations to declare it as a component, but also uses the standard DS annotation: org.osgi.service.component.annotations.Deactivate on method stop with signature ()V. It is an error to mix these two types of annotations",
				errors.get(1));
		assertEquals(
				"The DS component mixed-bnd-std uses bnd annotations to declare it as a component, but also uses the standard DS annotation: org.osgi.service.component.annotations.Modified on method update with signature (Ljava/util/Map;)V. It is an error to mix these two types of annotations",
				errors.get(2));
		assertEquals(
				"The DS component mixed-bnd-std uses bnd annotations to declare it as a component, but also uses the standard DS annotation: org.osgi.service.component.annotations.Reference on field ea. It is an error to mix these two types of annotations",
				errors.get(3));
		assertEquals(
				"The DS component mixed-bnd-std uses bnd annotations to declare it as a component, but also uses the standard DS annotation: org.osgi.service.component.annotations.Reference on method setLog with signature (Lorg/osgi/service/log/LogService;)V. It is an error to mix these two types of annotations",
				errors.get(4));
	}

}
