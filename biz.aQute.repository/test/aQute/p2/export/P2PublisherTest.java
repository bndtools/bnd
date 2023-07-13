package aQute.p2.export;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;

class P2PublisherTest {

	@Test
	void testBasic() throws Exception {
		try (Workspace ws = new Workspace(IO.getFile("testdata/p2-publish/ws-1"))) {
			ws.addBasicPlugin( new P2Exporter());

			Project p = ws.getProject("p1");
			File[] build = p.build();
			assertTrue(p.check());

			File file = p.getFile("generated/bndtools.jar");
			try (Jar jar = new Jar(file);
				Jar content = new Jar("content.jar", jar.getResource("content.jar")
					.openInputStream())) {
				InputStream xml = content.getResource("content.xml")
					.openInputStream();

				validateXML(xml, P2PublisherTest.class.getResourceAsStream("content-schema.xsd"));

			}
		}
	}

	@Test
	public void testExisting() throws FileNotFoundException {
		File file = IO.getFile("testdata/p2-publish/existing/previous-content.xml");
		validateXML(new FileInputStream(file), P2PublisherTest.class.getResourceAsStream("content-schema.xsd"));
	}

	public static boolean validateXML(InputStream xmlStream, InputStream xsdStream) {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(new StreamSource(xsdStream));
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(xmlStream));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
