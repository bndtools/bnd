package org.bndtools.core.xml;

import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

public class TestXmlElementSearch extends TestCase {

    private Reader input1;
    private Reader input2;

    @Override
    protected void setUp() throws Exception {
        input1 = new InputStreamReader(getClass().getResource("sample1.xml").openStream());
        input2 = new InputStreamReader(getClass().getResource("sample2.xml").openStream());
    }

    public void testSearchSimpleElement() throws XMLStreamException {
        XmlSearch search = new XmlSearch();
        ElementLocation location = search.searchElement(input1, new String[] { "root", "element1" });

        assertEquals(49, location.getOpen().getStart().getCharacterOffset());
        assertEquals(59, location.getOpen().getEnd().getCharacterOffset());
        assertEquals(70, location.getClose().getStart().getCharacterOffset());
        assertEquals(81, location.getClose().getEnd().getCharacterOffset());
    }

    public void testSearchDeepNestedElement() throws XMLStreamException {
        XmlSearch search = new XmlSearch();
        ElementLocation location = search.searchElement(input1, new String[] { "root", "parent", "child", "grandchild" });

        assertEquals(121, location.getOpen().getStart().getCharacterOffset());
        assertEquals(133, location.getOpen().getEnd().getCharacterOffset());
        assertEquals(138, location.getClose().getStart().getCharacterOffset());
        assertEquals(151, location.getClose().getEnd().getCharacterOffset());
    }

    public void testSearchComplexElement() throws XMLStreamException {
        XmlSearch search = new XmlSearch();
        ElementLocation location = search.searchElement(input1, new String[] { "root", "parent", "child" });

        assertEquals(104, location.getOpen().getStart().getCharacterOffset());
        assertEquals(111, location.getOpen().getEnd().getCharacterOffset());
        assertEquals(158, location.getClose().getStart().getCharacterOffset());
        assertEquals(166, location.getClose().getEnd().getCharacterOffset());
    }

    public void testBigComplexDoc() throws XMLStreamException {
        XmlSearch search = new XmlSearch();
        ElementLocation location = search.searchElement(input2, new String[] { "project", "profiles", "profile", "activation" });

        assertEquals(3702, location.getOpen().getStart().getCharacterOffset());
        assertEquals(3714, location.getOpen().getEnd().getCharacterOffset());
        assertEquals(3769, location.getClose().getStart().getCharacterOffset());
        assertEquals(3782, location.getClose().getEnd().getCharacterOffset());
    }

    public void testNotFound() throws XMLStreamException {
        XmlSearch search = new XmlSearch();
        ElementLocation location = search.searchElement(input2, new String[] { "project", "profiles", "profile", "activation", "activeByDefault", "WHOOPS" });

        assertNull(location);
    }

    public void testTreeSearch() throws XMLStreamException {
        LightweightDocument doc = new LightweightDocumentBuilder().build(input1);

        XmlSearch search = new XmlSearch();
        LightweightElement element = search.searchElement(doc, new String[] { "root", "parent", "child" });

        assertNotNull(element);

        ElementLocation location = element.getLocation();
        assertEquals(104, location.getOpen().getStart().getCharacterOffset());
        assertEquals(111, location.getOpen().getEnd().getCharacterOffset());
        assertEquals(158, location.getClose().getStart().getCharacterOffset());
        assertEquals(166, location.getClose().getEnd().getCharacterOffset());
    }

}
