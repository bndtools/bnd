package org.bndtools.core.xml;

import java.io.Reader;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.stax.WstxInputFactory;

public class LightweightDocumentBuilder {

    public LightweightDocument build(Reader reader) throws XMLStreamException {
        XMLInputFactory2 inputFactory = new WstxInputFactory();
        inputFactory.configureForXmlConformance();
        inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
        XMLStreamReader2 xmlReader = (XMLStreamReader2) inputFactory.createXMLStreamReader(reader);
        return build(xmlReader);
    }

    LightweightDocument build(XMLStreamReader2 reader) throws XMLStreamException {
        Deque<TagLocation> tagStack = new LinkedList<TagLocation>();

        List<LightweightElement> currentChildList = new LinkedList<LightweightElement>();
        Deque<List<LightweightElement>> elementStack = new LinkedList<List<LightweightElement>>();

        while (reader.hasNext()) {
            int eventType = reader.next();
            if (XMLStreamConstants.START_ELEMENT == eventType) {
                LocationInfo locInfo = reader.getLocationInfo();
                TagLocation openTag = new TagLocation(locInfo.getStartLocation(), locInfo.getEndLocation());
                tagStack.push(openTag);

                elementStack.push(currentChildList);
                currentChildList = new LinkedList<LightweightElement>();
            } else if (XMLStreamConstants.END_ELEMENT == eventType) {
                TagLocation openTag = tagStack.pop();

                LocationInfo locInfo = reader.getLocationInfo();
                TagLocation closeTag = new TagLocation(locInfo.getStartLocation(), locInfo.getEndLocation());
                LightweightElement newElement = new LightweightElement(reader.getLocalName(), new ElementLocation(openTag, closeTag));
                newElement.getChildren().addAll(currentChildList);

                currentChildList = elementStack.pop();
                currentChildList.add(newElement);
            }
        }

        if (currentChildList.isEmpty())
            throw new XMLStreamException("Document contains no elements.");
        if (currentChildList.size() > 1)
            throw new XMLStreamException("Document contains more than one root element.");

        return new LightweightDocument(currentChildList.get(0));
    }
}
