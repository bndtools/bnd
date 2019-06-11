package biz.aQute.bnd.reporter.plugins.resource.converter;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import biz.aQute.bnd.reporter.service.resource.converter.ResourceConverterPlugin;

public class XmlConverterPlugin implements ResourceConverterPlugin {

	static private final String[]	_ext	= {
		"xml"
	};

	private final DocumentBuilder	_db;

	public XmlConverterPlugin() {
		try {
			final DocumentBuilderFactory b = DocumentBuilderFactory.newInstance();
			b.setNamespaceAware(true);
			_db = b.newDocumentBuilder();
		} catch (final ParserConfigurationException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public String[] getHandledExtensions() {
		return _ext;
	}

	@Override
	public Object extract(final InputStream input) throws Exception {
		Objects.requireNonNull(input, "input");

		return toDto(_db.parse(input)
			.getDocumentElement());
	}

	private Object toDto(final Element node) {
		Object result = null;
		final Map<String, List<Object>> elements = new LinkedHashMap<>();
		final StringBuilder text = new StringBuilder();

		final NamedNodeMap attributes = node.getAttributes();
		for (int j = 0; j < attributes.getLength(); j++) {
			final Attr attribute = (Attr) attributes.item(j);
			initAndPut(elements, attribute.getLocalName() != null ? attribute.getLocalName() : attribute.getName(),
				attribute.getValue());
		}

		int textCount = 0;
		final NodeList nodes = node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			final Node child = nodes.item(i);

			if (child.getNodeType() == Node.ELEMENT_NODE) {
				final String name = ((Element) child).getLocalName();
				initAndPut(elements, name != null ? name : ((Element) child).getTagName(), toDto((Element) child));
			} else if (child.getNodeType() == Node.TEXT_NODE) {
				final String value = ((Text) child).getTextContent()
					.trim();
				if (!value.isEmpty()) {
					textCount++;
					initAndPut(elements, "_text", value);
					text.append(value);
				}
			}
		}

		if (elements.size() > 0 && !(textCount != 0 && elements.size() == 1)) {
			final Map<String, Object> tmp = new LinkedHashMap<>();
			for (final Entry<String, List<Object>> e : elements.entrySet()) {
				if (e.getValue()
					.size() == 1) {
					tmp.put(e.getKey(), e.getValue()
						.iterator()
						.next());
				} else {
					tmp.put(e.getKey(), e.getValue());
				}
			}
			result = tmp;
		} else if (text.length() > 0) {
			result = text.toString();
		}

		return result;
	}

	private void initAndPut(final Map<String, List<Object>> map, final String key, final Object value) {
		if (value != null) {
			if (!map.containsKey(key)) {
				map.put(key, new LinkedList<>());
			}
			map.get(key)
				.add(value);
		}
	}
}
