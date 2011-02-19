package org.bndtools.core.xml;

import java.util.LinkedList;
import java.util.List;

public class LightweightElement {

    private final String localName;
    private final ElementLocation location;
    private final List<LightweightElement> children = new LinkedList<LightweightElement>();

    LightweightElement(String localName, ElementLocation location) {
        this.localName = localName;
        this.location = location;
    }

    public String getLocalName() {
        return localName;
    }

    public ElementLocation getLocation() {
        return location;
    }

    public List<LightweightElement> getChildren() {
        return children;
    }

}
