package biz.aQute.resolve;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;
import aQute.bnd.build.Container;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.lib.deployer.FileRepo;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

/**
 * Test the project resolver
 */
public class ProjectResolverTest extends TestCase {

	private Workspace	ws;
	private File		tmp;
	private FileRepo	fr;
	private File		home	= IO.getFile("testdata/projectresolver");

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ws = Workspace.findWorkspace(IO.getFile("testdata/ws"));
		tmp = new File("tmp");
		tmp.mkdirs();
		fr = new FileRepo();
		fr.setLocation(tmp.getAbsolutePath());
		fr.setIndex(true);
		ws.getPlugins().add(fr);
		fr.put(IO.stream(IO.getFile(home, "jar/com.example.jerome.application.jar")), null);
		fr.put(IO.stream(IO.getFile(home, "jar/osgi.enroute.base.api.jar")), null);
	}

	@Override
	protected void tearDown() throws Exception {
		ws.close();
		IO.delete(tmp);
		super.tearDown();
	}

	public void testSimple() throws Exception {
		Run run = new Run(ws, IO.work, IO.getFile("testdata/projectresolver/simple.bndrun"));
		ProjectResolver pr = new ProjectResolver(run);
		pr.setTrace(true);
		pr.resolve();
		assertTrue(pr.check());
		List<Container> runbundles = pr.getRunbundles();
		assertEquals(2, runbundles.size());
		System.out.println(Strings.join("\n", runbundles));
		pr.close();
	}

	public void testProfile() throws Exception {
		Run run = new Run(ws, IO.work, IO.getFile("testdata/projectresolver/simple.bndrun"));
		run.setProperty("-distro", "org.apache.felix.gogo.runtime");
		ProjectResolver pr = new ProjectResolver(run);
		pr.setTrace(true);
		pr.resolve();
		assertTrue(pr.check());
		List<Container> runbundles = pr.getRunbundles();
		assertEquals(1, runbundles.size());
		System.out.println(Strings.join("\n", pr.getRunbundles()));
		pr.close();
	}

	public void testDistro() throws Exception {
		try {
			Run run = new Run(ws, IO.work, IO.getFile(home, "base.bndrun"));
			run.setProperty(Constants.RUNREQUIRES,
					"osgi.identity;filter:='(osgi.identity=com.example.jerome.application)'");
			run.setProperty(Constants.DISTRO, "osgi.enroute.base.api");

			ProjectResolver pr = new ProjectResolver(run);
			pr.setTrace(true);
			pr.resolve();
			assertTrue(pr.check());
			List<Container> runbundles = pr.getRunbundles();
			assertEquals(1, runbundles.size());
			System.out.println(Strings.join("\n", pr.getRunbundles()));
			pr.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
