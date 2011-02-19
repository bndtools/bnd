package org.bndtools.core.xml;

import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

public class TestLightweightDocumentBuilder extends TestCase {

    private Reader input1;

    @Override
    protected void setUp() throws Exception {
        input1 = new InputStreamReader(getClass().getResourceAsStream("sample1.xml"));
    }

    public void testSampleTree() throws XMLStreamException {
        LightweightDocument doc = new LightweightDocumentBuilder().build(input1);

        assertEquals("root", doc.getRootElement().getLocalName());
        assertEquals(2, doc.getRootElement().getChildren().size());

        LightweightElement element1 = doc.getRootElement().getChildren().get(0);
        assertEquals("element1", element1.getLocalName());
        assertTrue(element1.getChildren().isEmpty());

        LightweightElement parent = doc.getRootElement().getChildren().get(1);
        assertEquals("parent", parent.getLocalName());
        assertEquals(1, parent.getChildren().size());

        LightweightElement child = parent.getChildren().get(0);
        assertEquals("child", child.getLocalName());
        assertEquals(1, child.getChildren().size());

        LightweightElement grandChild = child.getChildren().get(0);
        assertEquals("grandchild", grandChild.getLocalName());
        assertTrue(grandChild.getChildren().isEmpty());
    }
}
