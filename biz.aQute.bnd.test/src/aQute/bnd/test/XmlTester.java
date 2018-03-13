package aQute.bnd.test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlTester {
	final static DocumentBuilderFactory	dbf		= DocumentBuilderFactory.newInstance();
	final static XPathFactory			xpathf	= XPathFactory.newInstance();
	final static DocumentBuilder		db;

	static {
		try {
			dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	final Document	document;
	final XPath		xpath	= xpathf.newXPath();

	public XmlTester(InputStream in, final String... namespace) throws Exception {
		xpath.setNamespaceContext(new NamespaceContext() {

			public Iterator<String> getPrefixes(String namespaceURI) {
				ArrayList<String> result = new ArrayList<>(namespace.length / 2);
				for (int i = 0; i < namespace.length; i += 2) {
					result.add(namespace[i]);
				}
				return result.iterator();
			}

			public String getPrefix(String namespaceURI) {
				for (int i = 0; i < namespace.length; i += 2) {
					if (namespaceURI.equals(namespace[i + 1]))
						return namespace[i];
				}
				return null;
			}

			public String getNamespaceURI(String prefix) {
				for (int i = 0; i < namespace.length; i += 2) {
					if (prefix.equals(namespace[i]))
						return namespace[i + 1];
				}
				return null;
			}
		});

		document = db.parse(in);
	}

	public void assertExactAttribute(String value, String expr) throws XPathExpressionException {
		System.err.println(expr);
		String o = (String) xpath.evaluate(expr, document, XPathConstants.STRING);
		assertNotNull(o);
		assertEquals(value, o);
	}

	public void assertAttribute(String value, String expr) throws XPathExpressionException {
		System.err.println(expr);
		String o = (String) xpath.evaluate(expr, document, XPathConstants.STRING);
		assertNotNull(o);
		assertEquals(value, o.trim());
	}

	public void assertTrimmedAttribute(String value, String expr) throws XPathExpressionException {
		System.err.println(expr);
		String o = (String) xpath.evaluate(expr, document, XPathConstants.STRING);
		assertNotNull(o);
		assertEquals(value, o.trim()
			.replaceAll("\n", "\\\\n"));
	}

	public void assertNoAttribute(String expr) throws XPathExpressionException {
		System.err.println(expr);
		String o = (String) xpath.evaluate(expr, document, XPathConstants.STRING);
		assertEquals("", o);
	}

	public void assertNamespace(String namespace) {
		Element element = document.getDocumentElement();
		String xmlns = element.getNamespaceURI();
		assertEquals(namespace, xmlns);
	}

	public void assertNumber(Double value, String expr) throws XPathExpressionException {
		System.err.println(expr);
		Double o = (Double) xpath.evaluate(expr, document, XPathConstants.NUMBER);
		assertNotNull(o);
		assertEquals(value, o);
	}

	public void assertCount(int value, String expr) throws XPathExpressionException {
		assertNumber(Double.valueOf(value), "count(" + expr + ")");
	}

}
