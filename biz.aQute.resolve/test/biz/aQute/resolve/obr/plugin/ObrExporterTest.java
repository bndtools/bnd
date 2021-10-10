package biz.aQute.resolve.obr.plugin;

import static aQute.lib.io.IO.getFile;
import static biz.aQute.resolve.Bndrun.createBndrun;
import static biz.aQute.resolve.obr.plugin.ObrExporter.TYPE;
import static java.util.zip.GZIPInputStream.GZIP_MAGIC;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.lib.io.IO;
import biz.aQute.resolve.Bndrun;

public class ObrExporterTest {

	private File		tempDir;
	private Bndrun		project;
	private ObrExporter	underTest;

	@BeforeEach
	public void setup() throws Exception {
		File path = IO.getPath("generated/")
			.toFile();

		tempDir = Files.createTempDirectory(path.toPath(), "obr-test")
			.toFile();

		IO.copy(IO.getFile("testdata/obr-exporter/"), tempDir);

		project = createBndrun(null, getFile(tempDir, "test.bndrun"));
		underTest = new ObrExporter();
	}

	@AfterEach
	public void teardown() throws IOException {
		IO.delete(tempDir);
	}

	@Test
	public void test_obr_generation_without_name_configuration() throws Exception {
		Map<String, String> options = new HashMap<>();
		File file = getFile(tempDir, "generated/test.xml");

		assertThat(file).doesNotExist();

		underTest.export(TYPE, project, options);

		assertThat(project.check()).isTrue();
		assertThat(file).exists();
	}

	@Test
	public void test_obr_generation_with_name_configuration() throws Exception {
		Map<String, String> options = createOptions("name", "abc.xml");
		File obr = getFile(tempDir, "generated/abc.xml");

		assertThat(obr).doesNotExist();

		underTest.export(TYPE, project, options);

		assertThat(project.check()).isTrue();
		assertThat(obr).exists();
	}

	@Test
	public void test_obr_generation_without_system_bundle_capabilities() throws Exception {
		Map<String, String> options = new HashMap<>();

		underTest.export(TYPE, project, options);

		File obr = getFile(tempDir, "generated/test.xml");

		assertThat(project.check()).isTrue();
		assertThat(obr).exists();
		assertThat(countResources(obr)).isEqualTo(1);
	}

	@Test
	public void test_obr_generation_with_system_bundle_capabilities() throws Exception {
		Map<String, String> options = createOptions("excludesystem", "false");
		File obr = getFile(tempDir, "generated/test.xml");

		assertThat(obr).doesNotExist();

		underTest.export(TYPE, project, options);

		assertThat(project.check()).isTrue();
		assertThat(obr).exists();
		assertThat(countResources(obr)).isEqualTo(2);
	}

	@Test
	public void test_obr_generation_gzipped_true() throws Exception {
		Map<String, String> options = createOptions("name", "abc.xml.gz");
		File obr = getFile(tempDir, "generated/abc.xml.gz");

		assertThat(obr).doesNotExist();

		underTest.export(TYPE, project, options);

		assertThat(project.check()).isTrue();
		assertThat(obr).exists();

		byte[] content = Files.readAllBytes(obr.toPath());
		boolean isGzipped = isCompressed(content);

		assertThat(isGzipped).isTrue();
	}

	@Test
	public void test_obr_generation_gzipped_false() throws Exception {
		Map<String, String> options = new HashMap<>();
		File obr = getFile(tempDir, "generated/test.xml");

		assertThat(obr).doesNotExist();

		underTest.export(TYPE, project, options);

		assertThat(project.check()).isTrue();
		assertThat(obr).exists();

		byte[] content = Files.readAllBytes(obr.toPath());
		boolean isGzipped = isCompressed(content);

		assertThat(isGzipped).isFalse();
	}

	@Test
	public void test_obr_generation_in_different_directory() throws Exception {
		Map<String, String> options = createOptions("outputdir", "obr");
		File obr = getFile(tempDir, "obr/test.xml");

		assertThat(obr).doesNotExist();

		underTest.export(TYPE, project, options);

		assertThat(project.check()).isTrue();
		assertThat(obr).exists();
	}

	private Map<String, String> createOptions(String key, String value) {
		Map<String, String> options = new HashMap<>();
		options.computeIfAbsent(key, e -> value);

		return options;
	}

	private boolean isCompressed(byte[] bytes) {
		if (bytes == null || bytes.length < 2) {
			return false;
		}
		return bytes[0] == (byte) GZIP_MAGIC && bytes[1] == (byte) (GZIP_MAGIC >> 8);
	}

	private int countResources(File xmlSource) throws Exception {
		return XMLResourceParser.getResources(xmlSource)
			.size();
	}

}
