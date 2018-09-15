package test;

import java.io.File;

import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class RunTest extends TestCase {
	Workspace	ws;
	File		tmp;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tmp = IO.getFile("generated/tmp");
		tmp.mkdirs();

		IO.copy(IO.getFile("testresources/ws"), tmp);
		ws = Workspace.findWorkspace(tmp);
	}

	@Override
	protected void tearDown() throws Exception {
		IO.delete(tmp);
		super.tearDown();
	}

	public void testSimple() throws Exception {
		// Project p = ws.getProject("runtest");
		// assertNotNull(p);
		//
		// Run run = new Run(ws, p.getBase(), new File(p.getBase(),
		// "simple.bndrun"));
		//
		// Entry<String,Resource> export =
		// run.export("osgi.subsystem.application", new Attrs());
		// assertNotNull(export);
		// assertTrue(run.check());
		//
		// Jar jar = new Jar(export.getKey(),
		// export.getValue().openInputStream());
		// IO.copy(jar.getResource("OSGI-INF/SUBSYSTEM.MF").openInputStream(),
		// System.out);
		// IO.copy(export.getValue().openInputStream(), new File(tmp,
		// "lookhere.esa"));
		// jar.close();
		// run.close();
	}
}
