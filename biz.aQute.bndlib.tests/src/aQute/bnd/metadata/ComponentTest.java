package aQute.bnd.metadata;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

public class ComponentTest extends TestCase {

	public void testComponents() throws Exception {

		Jar jar = new Jar("jar", "testresources/metadata/org.component.test.jar");

		ComponentExtractor e = new ComponentExtractor();
		BundleMetadataDTO dto = new BundleMetadataDTO();

		e.extract(dto, jar);
		e.verify(dto);

		ByteArrayOutputStream s = new ByteArrayOutputStream();
		MetadataJsonIO.toJson(dto, s);

		StringBuffer ee = new StringBuffer();

		for (String l : Files.readAllLines(Paths.get("testresources/metadata/resultComponent.json"),
				StandardCharsets.UTF_8)) {

			ee.append(l + "\n");
		}
		ee.deleteCharAt(ee.length() - 1);
		assertEquals(ee.toString(), new String(s.toByteArray()));
	}
}
