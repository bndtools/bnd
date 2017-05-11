package aQute.lib.xpath;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
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
	static DocumentBuilder				db;
	static XPath						xp;
	final Document						doc;

	static {
		try {
			db = dbf.newDocumentBuilder();
			xp = xpf.newXPath();
		} catch (Exception e) {
			// ignore
		}
	}

	public XPathParser(File file) throws Exception {
		doc = db.parse(file);
	}

	public <X> void parse(String what, Class<X> type, List<X> map) throws XPathExpressionException, Exception {
		NodeList proxies = (NodeList) xp.evaluate(what, doc, XPathConstants.NODESET);
		for (int i = 0; i < proxies.getLength(); i++) {
			Node node = proxies.item(i);
			X dto = type.getConstructor().newInstance();
			parse(node, dto);
			map.add(dto);
		}
	}

	public <X> void parse(Node node, X dto) throws Exception {

		for (Field f : dto.getClass().getFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;

			String value = xp.evaluate(f.getName(), node);
			if (value == null || value.isEmpty())
				continue;

			if (f.getType().isAnnotation())
				value = value.toUpperCase();

			Object o = Converter.cnv(f.getGenericType(), value);
			f.set(dto, o);
		}
	}

	public String parse(String expression) throws Exception {
		return xp.evaluate(expression, doc);
	}
}
