package aQute.bnd.metadata;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

public class MetadataJsonIOTest extends TestCase {


	public void testRead() throws Exception {

		try (InputStream inputStream = Files.newInputStream(Paths.get("testresources/metadata/bundleResult.json"))) {

			BundleMetadataDTO dto = MetadataJsonIO.fromJson(inputStream);

			assertNotNull(dto);
		}
	}

	public void testWrite() throws Exception {

		BundleMetadataDTO dto = new BundleMetadataDTO();

		Path p = Files.createTempFile(null, ".json");
		try (OutputStream outputStream = Files.newOutputStream(p, StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE)) {
			MetadataJsonIO.toJson(dto, outputStream);
		}

		assertEquals(new String(Files.readAllBytes(p)), "{\n\t\n}");
	}

	public void testSame() throws Exception {

		Jar bundleJar = new Jar("minimalMetadata", "testresources/metadata/org.bundle.test.jar");

		BundleMetadataDTO dtoFromJar = MetadataAnalizer.analyze(bundleJar);
		BundleMetadataDTO dtoFromFile = null;

		try (InputStream inputStream = Files.newInputStream(Paths.get("testresources/metadata/bundleResult.json"))) {

			dtoFromFile = MetadataJsonIO.fromJson(inputStream);
		}

		ByteArrayOutputStream streamFromJar = new ByteArrayOutputStream();
		ByteArrayOutputStream streamFromFile = new ByteArrayOutputStream();

		MetadataJsonIO.toJson(dtoFromJar, streamFromJar);
		MetadataJsonIO.toJson(dtoFromFile, streamFromFile);

		assertEquals(new String(streamFromJar.toByteArray()), new String(streamFromFile.toByteArray()));
	}
}
