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
    public void startElement(String uri, String localName, String qName, Attributes attribs1) throws SAXException {
        Attributes a = attribs1;
        if ("resource".equals(qName)) {
            AttributesImpl newAttribs = new AttributesImpl();

            for (int i = 0; i < a.getLength(); i++) {
                String value = a.getValue(i);
                if ("uri".equals(a.getQName(i))) {
                    value = absoluteize(value);
                }
                newAttribs.addAttribute(a.getURI(i), a.getLocalName(i), a.getQName(i), a.getType(i), value);
            }
            a = newAttribs;
        }

        super.startElement(uri, localName, qName, a);
    }

    private String absoluteize(String value) {
        if (value.startsWith("file:"))
            return value; // already absolute

        if (value.startsWith("/"))
            return "file:" + value;

        return root + value;
    }
}
