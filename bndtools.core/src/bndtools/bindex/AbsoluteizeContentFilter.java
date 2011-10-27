package bndtools.bindex;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import aQute.libg.sax.ContentFilterImpl;

public class AbsoluteizeContentFilter extends ContentFilterImpl {

    private final String root;

    public AbsoluteizeContentFilter(String root) {
        if (root.endsWith("/"))
            this.root = root;
        else
            this.root = root + "/";
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException {
        if ("resource".equals(qName)) {
            AttributesImpl newAttribs = new AttributesImpl();

            for (int i = 0; i < attribs.getLength(); i++) {
                String value = attribs.getValue(i);
                if ("uri".equals(attribs.getQName(i))) {
                    value = absoluteize(value);
                }
                newAttribs.addAttribute(attribs.getURI(i), attribs.getLocalName(i), attribs.getQName(i), attribs.getType(i), value);
            }
            attribs = newAttribs;
        }

        super.startElement(uri, localName, qName, attribs);
    }

    private String absoluteize(String value) {
        if (value.startsWith("file:"))
            return value; // already absolute

        if (value.startsWith("/"))
            return "file:" + value;

        return root + value;
    }
}
