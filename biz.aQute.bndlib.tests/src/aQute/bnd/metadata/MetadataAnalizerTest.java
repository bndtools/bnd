package aQute.bnd.metadata;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

public class MetadataAnalizerTest extends TestCase {

	public void testAnalizer() throws Exception {

		Jar bundleJar = new Jar("minimalMetadata", "testresources/metadata/org.bundle.test.jar");

		BundleMetadataDTO bundledto = MetadataAnalizer.analyze(bundleJar);

		ByteArrayOutputStream s = new ByteArrayOutputStream();

		MetadataJsonIO.toJson(bundledto, s);

		StringBuffer e = new StringBuffer();

		for (String l : Files.readAllLines(Paths.get("testresources/metadata/bundleResult.json"),
				StandardCharsets.UTF_8)) {

			e.append(l + "\n");
		}
		e.deleteCharAt(e.length() - 1);

		assertEquals(e.toString(), new String(s.toByteArray()));
	}
}
