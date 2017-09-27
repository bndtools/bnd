package aQute.bnd.metadata;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

public class DTOUtilTest extends TestCase {

	public void testBundleTranslation() throws Exception {

		Jar bundleJar = new Jar("minimalMetadata", "testresources/metadata/org.bundle.test.jar");

		BundleMetadataDTO dtoFromJar = MetadataAnalizer.analyze(bundleJar);

		BundleMetadataDTO transdto = DTOUtil.deepCopy(dtoFromJar, new Locale("en", "US"));

		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		MetadataJsonIO.toJson(transdto, stream);

		StringBuffer e = new StringBuffer();

		for (String l : Files.readAllLines(Paths.get("testresources/metadata/bundleEnResult.json"),
				StandardCharsets.UTF_8)) {

			e.append(l + "\n");
		}
		e.deleteCharAt(e.length() - 1);

		assertEquals(e.toString(), new String(stream.toByteArray()));
	}
}
