package aQute.libg.sax.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import aQute.libg.sax.ContentFilterImpl;
import aQute.libg.sax.SAXElement;

public class MergeContentFilter extends ContentFilterImpl {

	private final int expectedDocs;

	private int startedCounter = 0;
	private int elementDepth = 0;
	private final List<SAXElement> rootElements;

	public MergeContentFilter(int expectedDocs) {
		this.expectedDocs = expectedDocs;
		this.rootElements = new ArrayList<SAXElement>(expectedDocs);
	}

	public List<SAXElement> getRootElements() {
		return Collections.unmodifiableList(rootElements);
	}

	@Override
	public void startDocument() throws SAXException {
		if (startedCounter++ == 0)
			super.startDocument();
		if (startedCounter > expectedDocs)
			throw new SAXException(String.format("Received too many documents, expected only %d.", expectedDocs));
	}

	@Override
	public void endDocument() throws SAXException {
		if (startedCounter == expectedDocs)
			super.endDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		if (elementDepth++ == 0) {
			if (rootElements.isEmpty())
				super.startElement(uri, localName, qName, atts);
			else if (!rootElements.get(0).getqName().equals(qName))
				throw new SAXException(String.format("Documents have inconsistent root element names: first was %s, current is %s.", rootElements.get(0).getqName(), qName));
			rootElements.add(new SAXElement(uri, localName, qName, atts));
		} else {
			super.startElement(uri, localName, qName, atts);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (--elementDepth == 0) {
			if (rootElements.size() == expectedDocs)
				super.endElement(uri, localName, qName);
		} else {
			super.endElement(uri, localName, qName);
		}
	}
	
	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		if (startedCounter == 1)
			super.processingInstruction(target, data);
	}

}
