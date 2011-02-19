package org.bndtools.core.xml;

public class LightweightDocument {
    private final LightweightElement rootElement;

    LightweightDocument(LightweightElement rootElement) {
        this.rootElement = rootElement;
    }

    public LightweightElement getRootElement() {
        return rootElement;
    }
}
