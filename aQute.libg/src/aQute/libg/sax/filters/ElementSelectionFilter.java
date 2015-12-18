package aQute.libg.sax.filters;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import aQute.libg.sax.ContentFilterImpl;

public abstract class ElementSelectionFilter extends ContentFilterImpl {

	int	depth		= 0;
	int	hiddenDepth	= -1;

	protected abstract boolean select(int depth, String uri, String localName, String qName, Attributes attribs);

	@Override
	public final void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (hiddenDepth < 0) {
			boolean allow = select(depth, uri, localName, qName, atts);
			if (allow)
				super.startElement(uri, localName, qName, atts);
			else
				hiddenDepth = 0;
		} else {
			hiddenDepth++;
		}
		depth++;
	}

	@Override
	public final void endElement(String uri, String localName, String qName) throws SAXException {
		if (hiddenDepth < 0) {
			super.endElement(uri, localName, qName);
		} else {
			hiddenDepth--;
		}
		depth--;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (hiddenDepth < 0)
			super.characters(ch, start, length);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		if (hiddenDepth < 0)
			super.ignorableWhitespace(ch, start, length);
	}

}
