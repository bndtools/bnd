package aQute.libg.xml;

import java.io.File;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;

import aQute.lib.io.IO;
import aQute.libg.uri.URIUtil;
import junit.framework.TestCase;

public class DocumentBuilderFactoryTest extends TestCase {

	public void testUndeclaredEntity() throws Exception {
		File file = IO.getFile("testresources/xml/portlet-api-2.0.pom");

		DocumentBuilder builder = DocumentBuilderFactory.safeInstance();
		builder.parse(file);
	}

}
