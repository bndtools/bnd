package biz.aQute.bndoc.lib;

import java.io.*;

import aQute.lib.io.*;
import junit.framework.*;

public class SimpleSingleTest extends TestCase {

	public void testSimple() throws Exception {
		File tmp = new File("tmp");
		tmp.mkdirs();
		Generator g = new Generator();
		try {
			g.clean();
			g.setTrace(true);
			g.setExceptions(true);
			g.setTrace(true);
			g.setProperty("tmp", tmp.getAbsolutePath());
			g.setBase(IO.getFile("testdocs/docs"));
			g.generate();
			assertTrue(g.check());
		}
		finally {
			g.close();
			IO.delete(tmp);
		}
	}

}
