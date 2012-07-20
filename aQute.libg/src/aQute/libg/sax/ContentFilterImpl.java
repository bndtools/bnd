package aQute.libg.sax;

import org.xml.sax.*;

public class ContentFilterImpl implements ContentFilter {

	private ContentHandler	parent;

	public void setParent(ContentHandler parent) {
		this.parent = parent;

	}

	public ContentHandler getParent() {
		return parent;
	}

	public void setDocumentLocator(Locator locator) {
		parent.setDocumentLocator(locator);
	}

	public void startDocument() throws SAXException {
		parent.startDocument();
	}

	public void endDocument() throws SAXException {
		parent.endDocument();
	}

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		parent.startPrefixMapping(prefix, uri);
	}

	public void endPrefixMapping(String prefix) throws SAXException {
		parent.endPrefixMapping(prefix);
	}

	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		parent.startElement(uri, localName, qName, atts);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		parent.endElement(uri, localName, qName);
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		parent.characters(ch, start, length);
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		parent.ignorableWhitespace(ch, start, length);
	}

	public void processingInstruction(String target, String data) throws SAXException {
		parent.processingInstruction(target, data);
	}

	public void skippedEntity(String name) throws SAXException {
		parent.skippedEntity(name);
	}

}
