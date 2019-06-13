package aQute.lib.xpath;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;

import java.io.File;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.lib.converter.Converter;

public class XPathParser {
	final static DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();
	final static XPathFactory			xpf	= XPathFactory.newInstance();
	final XPath							xp;
	final Document						doc;

	public XPathParser(File file) throws Exception {
		doc = dbf.newDocumentBuilder()
			.parse(file);
		xp = xpf.newXPath();
	}

	public <X> void parse(String what, Class<X> type, List<X> map) throws XPathExpressionException, Exception {
		NodeList proxies = (NodeList) xp.evaluate(what, doc, XPathConstants.NODESET);
		for (int i = 0; i < proxies.getLength(); i++) {
			Node node = proxies.item(i);
			X dto = newInstance(type);
			parse(node, dto);
			map.add(dto);
		}
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

	public <X> void parse(Node node, X dto) throws Exception {

		for (Field f : dto.getClass()
			.getFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;

			String value = xp.evaluate(f.getName(), node);
			if (value == null || value.isEmpty())
				continue;

			value = processValue(value);

			if (f.getType()
				.isAnnotation())
				value = value.toUpperCase();

			Object o = Converter.cnv(f.getGenericType(), value);
			try {
				publicLookup().unreflectSetter(f)
					.invoke(dto, o);
			} catch (Error | Exception e) {
				throw e;
			} catch (Throwable e) {
				throw new InvocationTargetException(e);
			}
		}
	}

	protected String processValue(String value) {
		return value;
	}

	public String parse(String expression) throws Exception {
		return xp.evaluate(expression, doc);
	}
}
