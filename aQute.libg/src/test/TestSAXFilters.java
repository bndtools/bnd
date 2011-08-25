package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import aQute.libg.sax.SAXUtil;
import aQute.libg.sax.filters.ElementSelectionFilter;
import aQute.libg.sax.filters.MergeContentFilter;

public class TestSAXFilters extends TestCase {
	
	private static final String SAMPLE1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<root a=\"1\"><a><b><c></c></b></a></root>";
	private static final String SAMPLE2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<root b=\"2\"><x><y><z></z></y></x></root>";
	
	private static final String MERGED1_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<root a=\"1\"><a><b><c/></b></a><x><y><z/></y></x></root>";
	
	private static final String SAMPLE3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<anotherRoot b=\"2\"><x><y><z></z></y></x></anotherRoot>";
	private static final String SAMPLE4 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<root c=\"3\"><a><b><c><d><e><f><g><h></h></g></f></e></d></c></b></a></root>";
	
	private static final String SAMPLE5 = "<?xml version='1.0' encoding='UTF-8'?><?xml-stylesheet type='text/xsl' href='http://www2.osgi.org/www/obr2html.xsl'?><root><a/></root>";
	
	public void testMerge() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		MergeContentFilter merger = new MergeContentFilter(2);
		XMLReader reader = SAXUtil.buildPipeline(new StreamResult(output), merger);
		
		reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE1.getBytes())));
		reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE2.getBytes())));
		
		assertEquals(MERGED1_2, output.toString());
		assertEquals(2, merger.getRootElements().size());
	}
	
	public void testMergeInconsistentRoots() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		MergeContentFilter merger = new MergeContentFilter(2);
		XMLReader reader = SAXUtil.buildPipeline(new StreamResult(output), merger);
		
		try {
			reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE1.getBytes())));
			reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE3.getBytes())));
			fail ("Should throw exception for inconsistent roots");
		} catch (SAXException e) {
		}
	}
	
	public void testMergeTooManyDocs() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		MergeContentFilter merger = new MergeContentFilter(3);
		XMLReader reader = SAXUtil.buildPipeline(new StreamResult(output), merger);
		
		try {
			reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE1.getBytes())));
			reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE2.getBytes())));
			reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE1.getBytes())));
			reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE2.getBytes())));
			fail ("Should throw exception for root many docs");
		} catch (SAXException e) {
		}
	}
	
	public void testDontRepeatProcessingInstruction() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		MergeContentFilter merger = new MergeContentFilter(2);
		XMLReader reader = SAXUtil.buildPipeline(new StreamResult(output), merger);
		
		reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE5.getBytes())));
		reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE5.getBytes())));

		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><?xml-stylesheet type='text/xsl' href='http://www2.osgi.org/www/obr2html.xsl'?>\n<root><a/><a/></root>", output.toString());
	}
	
	public void testSelectionFilter() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		ElementSelectionFilter filter = new ElementSelectionFilter() {
			@Override
			protected boolean select(int depth, String uri, String localName, String qName, Attributes attribs) {
				return !"e".equals(qName);
			}
		};
		XMLReader reader = SAXUtil.buildPipeline(new StreamResult(output), filter);
		reader.parse(new InputSource(new ByteArrayInputStream(SAMPLE4.getBytes())));
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root c=\"3\"><a><b><c><d/></c></b></a></root>", output.toString());
	}
	
}
