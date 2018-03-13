package aQute.lib.remote;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import aQute.lib.io.IO;
import aQute.libg.remote.sink.RemoteSink;
import aQute.libg.remote.source.RemoteSource;
import junit.framework.TestCase;

public class RemoteTest extends TestCase {
	File					sinkDir;
	File					sourceDir;
	private RemoteSource	source;
	private RemoteSink		sink;

	@Override
	public void setUp() throws Exception {
		sinkDir = create("generated/sink/" + getName(), null);
		sourceDir = create("generated/source/" + getName(), "testresources/remote");
		super.setUp();
		source = new RemoteSource();
		sink = new RemoteSink(sinkDir, source);
		source.open(sink, sourceDir, "test");

	}

	@Override
	protected void tearDown() throws Exception {
		IO.delete(sinkDir);
		IO.delete(sourceDir);
		super.tearDown();
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

	public void testCmdTransform() throws Exception {
		// List<String> args = Arrays.asList("java", "-cp",
		// IO.getFile("bin_test").getAbsolutePath(),
		// "aQute.lib.remote.Foo");
		// StringBuilder sb = new StringBuilder();
		// source.launch(new HashMap<String,String>(), args, System.in, sb,
		// System.err);
		// source.join();
		// assertTrue(IO.getFile(sinkDir, "areas/test/cwd/test").isFile());
		// assertEquals("Hooray!\n", sb.toString());
	}

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
