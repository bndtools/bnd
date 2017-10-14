package biz.aQute.bnd.reporter.generator;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.lib.json.JSONCodec;
import aQute.lib.tag.Tag;

public class ReportConverter {

	public static List<Tag> fromXml(final InputStream inputStream) throws Exception {
		Objects.requireNonNull(inputStream);

		final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);

		return fromXml(doc, doc.getDocumentElement().getTagName());
	}

	public static List<Tag> fromXml(final InputStream inputStream, final String parentName) throws Exception {
		Objects.requireNonNull(inputStream);
		Objects.requireNonNull(parentName);

		return fromXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream), parentName);
	}

	private static List<Tag> fromXml(final Document document, final String parentName) {
		final List<Tag> result = new LinkedList<>();

		Tag e = new Tag(parentName);
		NamedNodeMap att = document.getDocumentElement().getAttributes();
		for (int j = 0; j < att.getLength(); j++) {
			e.addAttribute(att.item(j).getNodeName(), att.item(j).getNodeValue());
		}
		fillElement(document.getDocumentElement(), e);
		result.add(e);

		return result;
	}

	private static void fillElement(final Element element, final Tag tag) {
		NodeList nodes = element.getElementsByTagName("*");
		if (nodes.getLength() == 0) {
			tag.addContent(element.getTextContent());
		} else {
			for (int i = 0; i < nodes.getLength(); i++) {
				Element toFill = (Element) nodes.item(i);
				Tag e = new Tag(toFill.getTagName());
				NamedNodeMap att = toFill.getAttributes();
				for (int j = 0; j < att.getLength(); j++) {
					e.addAttribute(att.item(j).getNodeName(), att.item(j).getNodeValue());
				}
				fillElement(toFill, e);
				tag.addContent(e);
			}
		}
	}

	public static List<Tag> fromJson(final InputStream inputStream, final String parentName) throws Exception {
		Objects.requireNonNull(inputStream);
		Objects.requireNonNull(parentName);

		final List<Tag> result = new LinkedList<>();

		JSONCodec codec = new JSONCodec();
		result.addAll(fromJson(codec.dec().from(inputStream).get(), parentName));

		return result;
	}

	private static List<Tag> fromJson(final Object element, final String parentName) throws Exception {
		final List<Tag> result = new LinkedList<>();

		if (element == null) {
			return result;
		} else if (element instanceof Collection) {
			for (Object e : (Collection<?>) element) {
				result.addAll(fromJson(e, parentName));
			}
		} else if (element.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(element); i++) {
				result.addAll(fromJson(Array.get(element, i), parentName));
			}
		} else if (element instanceof Map) {
			Tag objectTag = new Tag(parentName);
			result.add(objectTag);
			for (Entry<?, ?> entry : ((Map<?, ?>) element).entrySet()) {
				for (Tag t : fromJson(entry.getValue(), entry.getKey().toString())) {
					objectTag.addContent(t);
				}
			}
		} else {
			result.add(new Tag(parentName, element.toString()));
		}

		return result;
	}

	public static List<Tag> fromProperties(final InputStream inputStream, final String parentName) throws Exception {
		Objects.requireNonNull(inputStream);
		Objects.requireNonNull(parentName);

		final List<Tag> result = new LinkedList<>();
		Properties properties = new Properties();
		Tag top = new Tag(parentName);

		properties.load(inputStream);

		for (Object key : properties.keySet()) {
			if (properties.getProperty(key.toString()) != null) {
				Tag propertyTag = new Tag("property");

				propertyTag.addContent(new Tag("key", key.toString()));
				propertyTag.addContent(new Tag("value", properties.getProperty(key.toString())));

				top.addContent(propertyTag);
			}
		}

		result.add(top);
		return result;
	}

	public static List<Tag> fromManifest(final InputStream inputStream, final String parentName) throws Exception {
		Objects.requireNonNull(inputStream);
		Objects.requireNonNull(parentName);

		final List<Tag> result = new LinkedList<>();

		Manifest manifest = new Manifest(inputStream);
		for (Object key : manifest.getMainAttributes().keySet()) {
			final Parameters param = new Parameters(true);
			OSGiHeader.parseHeader(manifest.getMainAttributes().getValue(key.toString()), null, param);

			Tag header = new Tag(key.toString());
			for (Entry<String, ? extends Map<String, String>> entry : param.asMapMap().entrySet()) {
				String paraKey = entry.getKey();
				while (paraKey.endsWith("~")) {
					paraKey = paraKey.substring(0, paraKey.length() - 1);
				}

				Tag paraTag = new Tag("clause");
				Tag valTag = new Tag("value", paraKey);
				for (Entry<String, String> entryAttr : entry.getValue().entrySet()) {
					String attrKey = entryAttr.getKey();
					while (attrKey.endsWith("~")) {
						attrKey = attrKey.substring(0, attrKey.length() - 1);
					}

					if (attrKey.endsWith(":")) {
						attrKey = attrKey.substring(0, attrKey.length() - 1);
					}

					Tag attrTag = new Tag("attribute");

					attrTag.addContent(new Tag("key", attrKey));
					attrTag.addContent(new Tag("value", entryAttr.getValue()));

					paraTag.addContent(attrTag);
				}
				paraTag.addContent(valTag);
				header.addContent(paraTag);
			}
			result.add(header);
		}
		return result;
	}
}
