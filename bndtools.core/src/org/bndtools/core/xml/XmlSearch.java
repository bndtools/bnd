package org.bndtools.core.xml;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.stax.WstxInputFactory;

public class XmlSearch {

    public ElementLocation searchElement(String text, String[] elementPath) throws XMLStreamException {
        return searchElement(new StringReader(text), elementPath);
    }

    public ElementLocation searchElement(Reader input, String[] elementPath) throws XMLStreamException {
        XMLStreamReader2 reader = createXmlReader(input);

        return doSearchElement(elementPath, 0, reader);
    }

    XMLStreamReader2 createXmlReader(Reader input) throws FactoryConfigurationError, XMLStreamException {
        XMLInputFactory2 inputFactory = new WstxInputFactory();
        inputFactory.configureForXmlConformance();
        inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(input);
        return reader;
    }

    ElementLocation doSearchElement(String[] elementPath, int pathIndex, XMLStreamReader2 reader) throws XMLStreamException {
        int depth = 0;

        while (reader.hasNext()) {
            int eventType = reader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                if (depth == 0 && elementPath[pathIndex].equals(reader.getLocalName())) {
                    int nextPathIndex = pathIndex + 1;
                    if (nextPathIndex == elementPath.length) {
                        // Found the start of the element we want... now find the end
                        LocationInfo loc = reader.getLocationInfo();
                        TagLocation openTag = new TagLocation(loc.getStartLocation(), loc.getEndLocation());
                        return readWholeElement(reader, openTag);
                    } else {
                        ElementLocation childResult = doSearchElement(elementPath, nextPathIndex, reader);
                        if (childResult != null)
                            return childResult;
                    }
                } else {
                    depth ++;
                }
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                depth --;
            }
        }

        return null;
    }

    ElementLocation readWholeElement(XMLStreamReader2 reader, TagLocation openTag) throws XMLStreamException {
        int depth = 0;

        while (reader.hasNext()) {
            int eventType = reader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT)
                depth ++;
            else if (eventType == XMLStreamConstants.END_ELEMENT) {
                depth --;
                if (depth < 0) {
                    LocationInfo loc = reader.getLocationInfo();
                    TagLocation closeTag = new TagLocation(loc.getStartLocation(), loc.getEndLocation());
                    return new ElementLocation(openTag, closeTag);
                }
            }
        }

        return null;
    }

    public LightweightElement searchElement(LightweightDocument document, String[] elementPath) {
        if (elementPath == null || elementPath.length == 0)
            return null;
        if (elementPath[0].equals(document.getRootElement().getLocalName())) {
            return doSearchElement(document.getRootElement(), elementPath, 1);
        }
        return null;
    }

    public LightweightElement searchElement(LightweightElement rootElement, String[] elementPath) {
        return doSearchElement(rootElement, elementPath, 0);
    }

    private LightweightElement doSearchElement(LightweightElement element, String[] elementPath, int pathIndex) {
        if (pathIndex == elementPath.length)
            return element;

        for (LightweightElement child : element.getChildren()) {
            if (elementPath[pathIndex].equals(child.getLocalName())) {
                LightweightElement childResult = doSearchElement(child, elementPath, pathIndex + 1);
                if (childResult != null)
                    return childResult;
            }
        }

        return null;
    }
}
