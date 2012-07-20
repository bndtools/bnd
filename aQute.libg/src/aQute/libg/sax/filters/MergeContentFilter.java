package aQute.libg.sax.filters;

import java.util.*;

import org.xml.sax.*;

import aQute.libg.sax.*;

public class MergeContentFilter extends ContentFilterImpl {

	private int						elementDepth	= 0;

	private final List<SAXElement>	rootElements	= new LinkedList<SAXElement>();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (elementDepth++ == 0) {
			if (rootElements.isEmpty())
				super.startElement(uri, localName, qName, atts);
			else if (!rootElements.get(0).getqName().equals(qName))
				throw new SAXException(String.format(
						"Documents have inconsistent root element names: first was %s, current is %s.", rootElements
								.get(0).getqName(), qName));
			rootElements.add(new SAXElement(uri, localName, qName, atts));
		} else {
			super.startElement(uri, localName, qName, atts);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (--elementDepth > 0) {
			super.endElement(uri, localName, qName);
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		if (rootElements.isEmpty())
			super.processingInstruction(target, data);
	}

	public void closeRootAndDocument() throws SAXException {
		if (!rootElements.isEmpty()) {
			SAXElement root = rootElements.get(0);
			super.endElement(root.getUri(), root.getLocalName(), root.getqName());
		}
		super.endDocument();
	}

	public List<SAXElement> getRootElements() {
		return Collections.unmodifiableList(rootElements);
	}

}
