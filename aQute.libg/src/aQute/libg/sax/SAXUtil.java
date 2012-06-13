package aQute.libg.sax;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;

import org.xml.sax.*;

public class SAXUtil {

	public static XMLReader buildPipeline(Result output, ContentFilter... filters) throws Exception {
		SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		TransformerHandler handler = factory.newTransformerHandler();
		handler.setResult(output);

		ContentHandler last = handler;
		if (filters != null)
			for (ContentFilter filter : filters) {
				filter.setParent(last);
				last = filter;
			}
		XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		reader.setContentHandler(last);

		return reader;
	}

}
