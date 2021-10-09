package aQute.lib.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import aQute.lib.io.IO;
import aQute.libg.remote.sink.RemoteSink;
import aQute.libg.remote.source.RemoteSource;

public class RemoteTest {
	File					sinkDir;
	File					sourceDir;
	private RemoteSource	source;
	private RemoteSink		sink;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		String baseDir = "generated/tmp/test/" + testInfo.getTestClass()
			.get()
			.getName() + "/"
			+ testInfo.getTestMethod()
				.get()
				.getName();
		sinkDir = create(baseDir + "/sink", null);
		sourceDir = create(baseDir + "/source", "testresources/remote");
		source = new RemoteSource();
		sink = new RemoteSink(sinkDir, source);
		source.open(sink, sourceDir, "test");

	}

	@AfterEach
	protected void tearDown() throws Exception {
		IO.delete(sinkDir);
		IO.delete(sourceDir);
	}

	private File create(String dir, String source) throws IOException {
		File tmp = IO.getFile(dir);
		IO.delete(tmp);
		tmp.mkdirs();

		if (source == null)
			return tmp;

		File src = IO.getFile(source);
		IO.copy(src, tmp);
		return tmp;
	}

	@Test
	public void testTransform() throws Exception {
		File file = new File(sourceDir, "list");
		Formatter f = new Formatter();
		f.format("%s\n", IO.getFile(sourceDir, "a.txt")
			.getAbsolutePath());
		f.format("%s\n", IO.getFile(sourceDir, "b.txt")
			.getAbsolutePath());
		f.format("%s\n", IO.getFile("bnd.bnd")
			.getAbsolutePath());
		IO.store(f.toString(), file);
		f.close();
		source.update(file);
		source.sync();
		assertTrue(IO.getFile(sinkDir, "areas/test/cwd/a.txt")
			.isFile());
		assertTrue(IO.getFile(sinkDir, "areas/test/cwd/b.txt")
			.isFile());
		assertTrue(IO.getFile(sinkDir, "areas/test/cwd/list")
			.isFile());
		assertTrue(IO.getFile(sinkDir, "shacache/9124D0084FC1DECD361E82332F535E6371496CEB")
			.isFile());
		assertTrue(IO.getFile(sinkDir, "shacache/A6A4DB850D85C513F549A51A3315A67B50EA86F2")
			.isFile());
		assertTrue(IO.getFile(sinkDir, "areas/test/cwd/_ABS")
			.isDirectory());
	}

	@Test
	public void testSimple() throws Exception {
		source.add(sourceDir);
		source.sync();

		assertTrue(IO.getFile(sinkDir, "areas/test/cwd/a.txt")
			.isFile());
		assertTrue(IO.getFile(sinkDir, "areas/test/cwd/b.txt")
			.isFile());
		assertTrue(IO.getFile(sinkDir, "shacache/9124D0084FC1DECD361E82332F535E6371496CEB")
			.isFile());
		assertTrue(IO.getFile(sinkDir, "shacache/A6A4DB850D85C513F549A51A3315A67B50EA86F2")
			.isFile());
		assertEquals(2, sinkDir.list().length);
		assertFalse(IO.getFile(sinkDir, "areas/test/cwd/_ABS")
			.isDirectory());

		source.add(new File("testresources/remote"));
		source.sync();
		assertTrue(IO.getFile(sinkDir, "areas/test/cwd/_ABS")
			.isDirectory());
		assertEquals(2, IO.getFile(sinkDir, "shacache")
			.list().length);
	}

}
