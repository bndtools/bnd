package aQute.p2.provider;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import aQute.lib.converter.Converter;

public class XML {
	final static DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();
	final static XPathFactory			xpf	= XPathFactory.newInstance();
	final XPath							xp;
	final Document						document;

	public XML(Document document) {
		this.document = document;
		xp = xpf.newXPath();
	}

	String getAttribute(Node node, String name) {
		NamedNodeMap attributes = node.getAttributes();
		Node namedItem = attributes.getNamedItem(name);
		if (namedItem == null)
			return null;

		return namedItem.getNodeValue();
	}

	static Document getDocument(InputStream in) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(in);
	}

	NodeList getNodes(String path) throws Exception {
		return getNodes(document, path);
	}

	NodeList getNodes(Node root, String path) throws Exception {
		return (NodeList) xp.evaluate(path, root, XPathConstants.NODESET);
	}

	Map<String, String> getProperties(String path) throws Exception {
		return getProperties(document, path);
	}

	Map<String, String> getProperties(Node node, String path) throws Exception {
		Map<String, String> properties = new HashMap<>();
		NodeList propertyNodes = getNodes(node, path);
		for (int i = 0; i < propertyNodes.getLength(); i++) {
			Node propertyNode = propertyNodes.item(i);
			String name = getAttribute(propertyNode, "name");
			String value = getAttribute(propertyNode, "value");
			properties.put(name, value);
		}
		return properties;
	}

	<T> T getFromType(Node item, Class<T> clazz) throws Exception {
		T a = newInstance(clazz);
		for (Field f : clazz.getDeclaredFields()) {
			String s = getAttribute(item, f.getName());
			if (s != null) {
				setField(f, a, Converter.cnv(f.getGenericType(), s));
			}
		}
		return a;
	}

	private static final MethodType defaultConstructor = methodType(void.class);

	private static <T> T newInstance(Class<T> rawClass) throws Exception {
		try {
			return (T) publicLookup().findConstructor(rawClass, defaultConstructor)
				.invoke();
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static void setField(Field f, Object targetObject, Object value) throws Exception {
		try {
			MethodHandle mh = publicLookup().unreflectSetter(f);
			if (Modifier.isStatic(f.getModifiers())) {
				mh.invoke(value);
			} else {
				mh.invoke(targetObject, value);
			}
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new InvocationTargetException(e);
		}
	}

}
