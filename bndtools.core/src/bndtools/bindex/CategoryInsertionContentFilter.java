package bndtools.bindex;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import aQute.libg.sax.ContentFilterImpl;

public class CategoryInsertionContentFilter extends ContentFilterImpl {

    private final String category;

    public CategoryInsertionContentFilter(String category) {
        this.category = category;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(uri, localName, qName, atts);

        if ("resource".equals(qName)) {
            // Insert category child element
            AttributesImpl categoryAttribs = new AttributesImpl();
            categoryAttribs.addAttribute(null, null, "id", "CDATA", category);

            super.startElement(null, null, "category", categoryAttribs);
            super.endElement(null, null, "category");
        }
    }

}
