package test.component;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.osgi.framework.ServiceReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import junit.framework.TestCase;

/**
 * Test for use of DS components specified only by Service-Component headers.
 */
@SuppressWarnings({
	"resource", "rawtypes"
})
public class ComponentTest extends TestCase {
	static final int					BUFFER_SIZE	= IOConstants.PAGE_SIZE * 1;

	static final DocumentBuilderFactory	dbf			= DocumentBuilderFactory.newInstance();
	static final XPathFactory			xpathf		= XPathFactory.newInstance();
	XPath								xpath;

	static {
		dbf.setNamespaceAware(true);
	}

	@Override
	protected void setUp() {
		xpath = xpathf.newXPath();
		xpath.setNamespaceContext(new NamespaceContext() {

			@Override
			public Iterator<String> getPrefixes(String namespaceURI) {
				return Arrays.asList("md", "scr")
					.iterator();
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

	public static class ReferenceOrder {

		void setA(ServiceReference sr) {}

		void unsetA(ServiceReference sr) {}

		void setZ(ServiceReference sr) {}

		void unsetZ(ServiceReference sr) {}
	}

	/**
	 * 112.5.7 says refeence order is used to order binding services, so from
	 * headers we preserve order.
	 *
	 * @throws Exception
	 */
	public void testHeaderReferenceOrder() throws Exception {
		Document doc = setup(
			ReferenceOrder.class.getName()
				+ ";version:=1.1;z=org.osgi.service.http.HttpService?;a=org.osgi.service.http.HttpService?",
			ReferenceOrder.class.getName());
		assertAttribute(doc, "z", "scr:component/reference[1]/@name");
		assertAttribute(doc, "a", "scr:component/reference[2]/@name");
	}

	/**
	 * Test to see if we ignore scala.ScalaObject as interface
	 *
	 * @throws Exception
	 */
	public void testScalaObject() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/com.test.scala.jar"));
		b.setProperty("Service-Component", "com.test.scala.Service");
		b.setProperty("Export-Package", "com.test.scala.*");
		Jar jar = b.build();
		Manifest m = jar.getManifest();
		System.err.println(Processor.join(b.getErrors()));
		System.err.println(Processor.join(b.getWarnings()));
		System.err.println(m.getMainAttributes()
			.getValue("Service-Component"));
		IO.copy(jar.getResource("OSGI-INF/com.test.scala.Service.xml")
			.openInputStream(), System.err);
		Document doc = doc(b, "com.test.scala.Service");
		assertEquals("com.test.scala.Service", xpath.evaluate("component/implementation/@class", doc));
		assertEquals("", xpath.evaluate("component/service/provide/@interface", doc));

	}

	/**
	 * Test if a reference is made to an interface implemented on a superclass.
	 * This is from https://github.com/bndtools/bnd/issues#issue/23
	 */

	public void testProvideFromSuperClass() throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
			IO.getFile("bin_test")
		});
		b.setProperty("Private-Package", "test.activator.inherits");
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());

		Manifest m = b.getJar()
			.getManifest();
		String imports = m.getMainAttributes()
			.getValue("Import-Package");
		assertTrue(imports.contains("org.osgi.framework"));
	}

	/**
	 * A non-FQN entry, should generate an error and no component
	 */

	public void testNonFQN() throws Exception {
		Builder b = new Builder();
		b.setProperty("Include-Resource",
			"org/osgi/impl/service/coordinator/AnnotationWithJSR14.class=jar/AnnotationWithJSR14.jclass");
		b.setProperty("Service-Component", "*");
		b.setProperty("-resourceonly", "true");
		Jar jar = b.build();
		b.check("Service-Component is normally generated by bnd.");

		Manifest manifest = jar.getManifest();
		String component = manifest.getMainAttributes()
			.getValue("Service-Component");
		System.err.println(component);
		assertNull(component);
	}

	void assertAttribute(Document doc, String value, String expr) throws XPathExpressionException {
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
		Document doc = dbf.newDocumentBuilder()
			.parse(r.openInputStream());
		r.write(System.err);
		return doc;
	}

	public void testV1_1Directives() throws Exception {
		Element component = setup(
			"test.activator.Activator11;factory:=blabla;immediate:=true;enabled:=false;configuration-policy:=optional;activate:=start;deactivate:=stop;modified:=modded",
			"test.activator.Activator11").getDocumentElement();
		assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component.getNamespaceURI());
		assertEquals("blabla", component.getAttribute("factory"));
		assertEquals("false", component.getAttribute("enabled"));
		assertEquals("optional", component.getAttribute("configuration-policy"));
		assertEquals("start", component.getAttribute("activate"));
		assertEquals("stop", component.getAttribute("deactivate"));
		assertEquals("modded", component.getAttribute("modified"));

	}

	public void testNoNamespace() throws Exception {
		Element component = setup("test.activator.Activator");
		assertEquals(null, component.getNamespaceURI());
	}

	public void testAutoNamespace() throws Exception {
		Element component = setup("test.activator.Activator;activate:='start';deactivate:='stop'");
		assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component.getNamespaceURI());

		// activate/deactivate with BundleContext args
		component = setup("test.activator.Activator2", "test.activator.Activator2").getDocumentElement();
		assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component.getNamespaceURI());

		// deactivate with a int reason
		component = setup("test.activator.Activator3", "test.activator.Activator3").getDocumentElement();
		assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component.getNamespaceURI());

		// package access activate/deactivate with ComponentContext args
		component = setup("test.activator.ActivatorPackage", "test.activator.ActivatorPackage").getDocumentElement();
		assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component.getNamespaceURI());

		// private access activate/deactivate with ComponentContext args
		component = setup("test.activator.ActivatorPrivate", "test.activator.ActivatorPrivate").getDocumentElement();
		assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component.getNamespaceURI());

	}

	public void testCustomVersion() throws Exception {
		Element component = setup("test.activator.Activator;version:=2");
		assertEquals("http://www.osgi.org/xmlns/scr/v2.0.0", component.getNamespaceURI());
	}

	public void testCustomNamespace() throws Exception {
		Element component = setup("test.activator.Activator;xmlns:='http://www.osgi.org/xmlns/xscr/v2.0.0'");
		assertEquals("http://www.osgi.org/xmlns/xscr/v2.0.0", component.getNamespaceURI());
	}

	static Document setup(String header, String className) throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.SERVICE_COMPONENT, header);
		b.setClasspath(new File[] {
			IO.getFile("bin_test"), IO.getFile("jar/osgi.jar")
		});
		b.setProperty("Private-Package", "test.activator, org.osgi.service.http.*");
		b.build();

		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		List<String> errors = b.getErrors();
		// ignore the dynamic-without-unbind error here
		if (!errors.isEmpty()) {
			assertEquals(1, errors.size());
			assertTrue(errors.get(0)
				.endsWith("dynamic but has no unbind method."));
		}
		assertEquals(0, b.getWarnings()
			.size());

		String path = "OSGI-INF/" + className + ".xml";
		print(b.getJar()
			.getResource(path), System.err);
		Document doc = dbf.newDocumentBuilder()
			.parse(new InputSource(b.getJar()
				.getResource(path)
				.openInputStream()));

		return doc;
	}

	static Element setup(String header) throws Exception {
		return setup(header, "test.activator.Activator").getDocumentElement();
	}

	private static void print(Resource resource, OutputStream out) throws Exception {
		InputStream in = resource.openInputStream();
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
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
	 * b .setProperty(Analyzer.SERVICE_COMPONENT,
	 * "testresources/component/*.xml"); b.setProperty("-resourceonly", "true");
	 * b.setProperty("Include-Resource",
	 * "testresources/component=testresources/component"); Jar jar = b.build();
	 * System.err.println(b.getErrors()); System.err.println(b.getWarnings());
	 * assertEquals(0, b.getErrors().size()); assertEquals(0,
	 * b.getWarnings().size()); }
	 */
	public void testImplementation() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.SERVICE_COMPONENT,
			"silly.name;implementation:=test.activator.Activator;provide:=java.io.Serialization;servicefactory:=true");
		b.setClasspath(new File[] {
			IO.getFile("bin_test"), IO.getFile("jar/osgi.jar")
		});
		b.setProperty("Private-Package", "test.activator");
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());

		Jar jar = b.getJar();

		Document doc = dbf.newDocumentBuilder()
			.parse(new InputSource(jar.getResource("OSGI-INF/silly.name.xml")
				.openInputStream()));

		assertEquals("test.activator.Activator", doc.getElementsByTagName("implementation")
			.item(0)
			.getAttributes()
			.getNamedItem("class")
			.getNodeValue());
		assertEquals("true", doc.getElementsByTagName("service")
			.item(0)
			.getAttributes()
			.getNamedItem("servicefactory")
			.getNodeValue());
	}

	/**
	 * Standard activator with reference to http.
	 *
	 * @throws Exception
	 */
	public void testProperties() throws Exception {
		java.util.Properties p = new Properties();
		p.put(Constants.EXPORT_PACKAGE, "test.activator,org.osgi.service.http");
		p.put(Constants.IMPORT_PACKAGE, "*");
		p.put(Constants.SERVICE_COMPONENT, "test.activator.Activator;properties:=\"a=3|4,b=1|2|3\"");
		Builder b = new Builder();
		b.setClasspath(new File[] {
			IO.getFile("bin_test"), IO.getFile("jar/osgi.jar")
		});
		b.setProperties(p);
		b.build();
		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());

		Jar jar = b.getJar();

		Resource resource = jar.getResource("OSGI-INF/test.activator.Activator.xml");
		IO.copy(resource.openInputStream(), System.out);

		Document doc = dbf.newDocumentBuilder()
			.parse(new InputSource(resource.openInputStream()));

		NodeList l = doc.getElementsByTagName("property");
		assertEquals(2, l.getLength());

		boolean aset = false, bset = false;

		for (int i = 0; i < 2; i++) {
			Node child = l.item(i);
			NamedNodeMap attributes = child.getAttributes();
			Node namedItem = attributes.getNamedItem("name");
			String name = namedItem.getNodeValue();
			String text = child.getFirstChild()
				.getNodeValue()
				.trim();
			if (name.equals("a")) {
				aset = true;
				assertEquals("3\n4", text);
			}
			if (name.equals("b")) {
				bset = true;
				assertEquals("1\n2\n3", text);
			}
		}
		assertTrue(aset);
		assertTrue(bset);
		assertEquals("test.activator.Activator", doc.getElementsByTagName("implementation")
			.item(0)
			.getAttributes()
			.getNamedItem("class")
			.getNodeValue());
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
		p.put(Constants.EXPORT_PACKAGE, "test.activator,org.osgi.service.http");
		p.put(Constants.IMPORT_PACKAGE, "*");
		p.put(Constants.SERVICE_COMPONENT, "test.activator.Activator;provides:=true");
		Builder b = new Builder();
		b.setClasspath(new File[] {
			IO.getFile("bin_test"), IO.getFile("jar/osgi.jar")
		});
		b.setProperties(p);
		b.build();
		doc(b, "test.activator.Activator");
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(1, b.getErrors()
			.size());
		assertTrue(b.getErrors()
			.get(0)
			.contains("Unrecognized directive"));
		assertEquals(0, b.getWarnings()
			.size());
	}

	/**
	 * Check if all the directives work
	 *
	 * @throws Exception
	 */
	public void testDirectives() throws Exception {
		Document doc = setup(
			"test.activator.Activator;http=org.osgi.service.http.HttpService;dynamic:=http;optional:=http;provide:=test.activator.Activator; multiple:=http",
			"test.activator.Activator");

		assertEquals("test.activator.Activator", xpath.evaluate("/component/implementation/@class", doc));
		assertEquals("org.osgi.service.http.HttpService",
			xpath.evaluate("/component/reference[@name='http']/@interface", doc));
		// there are no bind/unbind methods...
		assertEquals("", xpath.evaluate("/component/reference[@name='http']/@bind", doc));
		assertEquals("", xpath.evaluate("/component/reference[@name='http']/@unbind", doc));
		assertEquals("0..n", xpath.evaluate("/component/reference[@name='http']/@cardinality", doc));
		assertEquals("dynamic", xpath.evaluate("/component/reference[@name='http']/@policy", doc));
		assertEquals("test.activator.Activator", xpath.evaluate("/component/service/provide/@interface", doc));
	}

	/**
	 * Check if a bad filter on a service component causes an error.
	 *
	 * @throws Exception
	 */
	public void testBadFilter() throws Exception {
		java.util.Properties p = new Properties();
		p.put(Constants.EXPORT_PACKAGE, "test.activator,org.osgi.service.http");
		p.put(Constants.IMPORT_PACKAGE, "*");
		p.put(Constants.SERVICE_COMPONENT,
			"test.activator.Activator;http=\"org.osgi.service.http.HttpService(|p=1)(p=2))\"");
		Builder b = new Builder();
		b.setClasspath(new File[] {
			IO.getFile("bin_test"), IO.getFile("jar/osgi.jar")
		});
		b.setProperties(p);
		b.build();
		System.err.println(b.getErrors());
		System.err.println(b.getWarnings());
		assertEquals(1, b.getErrors()
			.size());
		assertTrue(b.getErrors()
			.get(0)
			.contains("Invalid target filter"));
		assertEquals(0, b.getWarnings()
			.size());
	}

	/**
	 * Check if we can set a target filter
	 *
	 * @throws Exception
	 */
	public void testFilter() throws Exception {
		Element component = setup("test.activator.Activator;http=\"org.osgi.service.http.HttpService(|(p=1)(p=2))\"");
		Element implementation = (Element) component.getElementsByTagName("implementation")
			.item(0);
		assertEquals(null, implementation.getNamespaceURI());
		assertEquals("test.activator.Activator", implementation.getAttribute("class"));

		Element reference = (Element) component.getElementsByTagName("reference")
			.item(0);
		assertEquals("org.osgi.service.http.HttpService", reference.getAttribute("interface"));
		// we actually check for the methods and don't add them blindly
		assertEquals("", reference.getAttribute("bind"));
		assertEquals("", reference.getAttribute("unbind"));
		assertEquals("(|(p=1)(p=2))", reference.getAttribute("target"));
	}

	/**
	 * Standard activator with reference to http.
	 *
	 * @throws Exception
	 */
	public void testSimple() throws Exception {
		Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService?");
		Element implementation = (Element) component.getElementsByTagName("implementation")
			.item(0);
		assertEquals(null, implementation.getNamespaceURI());
		assertEquals("test.activator.Activator", implementation.getAttribute("class"));

		Element reference = (Element) component.getElementsByTagName("reference")
			.item(0);
		assertEquals("org.osgi.service.http.HttpService", reference.getAttribute("interface"));
		assertEquals("", reference.getAttribute("bind"));
		assertEquals("", reference.getAttribute("unbind"));
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
		Element reference = (Element) component.getElementsByTagName("reference")
			.item(0);
		assertEquals("0..1", reference.getAttribute("cardinality"));
		assertEquals("dynamic", reference.getAttribute("policy"));
	}

	public void testStar() throws Exception {
		Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService*");
		Element reference = (Element) component.getElementsByTagName("reference")
			.item(0);
		assertEquals("0..n", reference.getAttribute("cardinality"));
		assertEquals("dynamic", reference.getAttribute("policy"));
	}

	public void testPlus() throws Exception {
		Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService+");
		Element reference = (Element) component.getElementsByTagName("reference")
			.item(0);
		assertEquals("1..n", reference.getAttribute("cardinality"));
		assertEquals("dynamic", reference.getAttribute("policy"));
	}

	public void testTilde() throws Exception {
		Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService~");
		Element reference = (Element) component.getElementsByTagName("reference")
			.item(0);
		assertEquals("0..1", reference.getAttribute("cardinality"));
		assertEquals("", reference.getAttribute("policy"));
	}

}
