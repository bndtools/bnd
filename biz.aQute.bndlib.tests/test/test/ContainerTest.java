package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import aQute.bnd.build.Container;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Builder;
import aQute.lib.io.IO;

public class ContainerTest {
	File tmp;

	@BeforeEach
	public void setUp(TestInfo testInfo) {
		tmp = IO.getFile("generated/tmp/test/" + testInfo.getTestClass()
			.get()
			.getName() + "/"
			+ testInfo.getTestMethod()
				.get()
				.getName())
			.getAbsoluteFile();

		IO.delete(tmp);
		tmp.mkdirs();
	}

	@Test
	public void testBundleClasspath() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(new File("bin_test"));
			b.setBundleClasspath(".,osgi.jar");
			b.setIncludeResource("jar/osgi.jar");
			b.setExportPackage("test.annotation");
			b.setProperty("Hello", "World");
			b.build();
			assertTrue(b.check());
			File testjar = IO.getFile(tmp, "test.jar");
			b.getJar()
				.write(testjar);

			Attrs attrs = new Attrs();
			attrs.put("expand-bcp", "true");
			Container c = new Container(testjar, null, attrs);
			List<File> files = new ArrayList<>();
			c.contributeFiles(files, b);

			assertEquals(2, files.size());

			assertEquals(files.get(0)
				.getCanonicalFile(), testjar.getCanonicalFile());
		}
	}
}
