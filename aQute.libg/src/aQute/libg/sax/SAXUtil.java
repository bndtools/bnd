package aQute.libg.sax;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;

public class SAXUtil {

	public static XMLReader buildPipeline(Result output, ContentFilter... filters) throws Exception {
		SAXTransformerFactory factory = (SAXTransformerFactory) TransformerFactory.newInstance();
		TransformerHandler handler = factory.newTransformerHandler();
		handler.setResult(output);

		ContentHandler last = handler;
		if (filters != null)
			for (ContentFilter filter : filters) {
				filter.setParent(last);
				last = filter;
			}
		XMLReader reader = SAXParserFactory.newInstance()
			.newSAXParser()
			.getXMLReader();
		reader.setContentHandler(last);

		return reader;
	}

}
