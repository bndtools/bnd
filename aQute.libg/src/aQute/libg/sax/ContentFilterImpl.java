package aQute.libg.sax;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class ContentFilterImpl implements ContentFilter {

	private ContentHandler parent;

	@Override
	public void setParent(ContentHandler parent) {
		this.parent = parent;

	}

	@Override
	public ContentHandler getParent() {
		return parent;
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		parent.setDocumentLocator(locator);
	}

	@Override
	public void startDocument() throws SAXException {
		parent.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		parent.endDocument();
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		parent.startPrefixMapping(prefix, uri);
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		parent.endPrefixMapping(prefix);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		parent.startElement(uri, localName, qName, atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		parent.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		parent.characters(ch, start, length);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		parent.ignorableWhitespace(ch, start, length);
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		parent.processingInstruction(target, data);
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		parent.skippedEntity(name);
	}

}
