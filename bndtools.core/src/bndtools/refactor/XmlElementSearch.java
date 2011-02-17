package bndtools.refactor;

import java.io.Reader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

public class XmlElementSearch {

    public XmlElementLocation search(Reader input, String[] elementPath) throws XMLStreamException {
        XMLInputFactory2 inputFactory = (XMLInputFactory2) XMLInputFactory2.newInstance();
        inputFactory.configureForXmlConformance();
        inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(input);

        return doSearch(elementPath, 0, reader);
    }

    private XmlElementLocation doSearch(String[] elementPath, int pathIndex, XMLStreamReader2 reader) throws XMLStreamException {
        int depth = 0;
        int eventType = reader.next();

        while (eventType != XMLStreamConstants.END_DOCUMENT)
        if (eventType == XMLStreamConstants.START_ELEMENT) {
            if (depth == 0 && elementPath[pathIndex].equals(reader.getLocalName())) {
                int nextPathIndex = pathIndex + 1;
                if (nextPathIndex == elementPath.length) {
                    // Found the start of the element we want... now find the end
                    LocationInfo loc = reader.getLocationInfo();
                    return readWholeElement(reader, loc.getStartingCharOffset(), loc.getEndingCharOffset());
                } else {
                    XmlElementLocation childResult = doSearch(elementPath, nextPathIndex, reader);
                    if (childResult != null)
                        return childResult;
                }
            } else {
                depth ++;
            }
        } else if (eventType == XMLStreamConstants.END_ELEMENT) {
            depth --;
        }

        return null;
    }

    private XmlElementLocation readWholeElement(XMLStreamReader2 reader, long openTagStart, long openTagEnd) throws XMLStreamException {
        int depth = 0;

        int eventType = reader.next();
        while (eventType != XMLStreamConstants.END_DOCUMENT) {
            if (eventType == XMLStreamConstants.START_ELEMENT)
                depth ++;
            else if (eventType == XMLStreamConstants.END_ELEMENT) {
                depth --;
                if (depth < 0) {
                    LocationInfo loc = reader.getLocationInfo();
                    return new XmlElementLocation(openTagStart, openTagEnd, loc.getStartingCharOffset(), loc.getEndingCharOffset());
                }
            }

            eventType = reader.next();
        }

        return null;
    }
}
