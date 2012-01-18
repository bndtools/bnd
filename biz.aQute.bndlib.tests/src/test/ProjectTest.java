package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.lib.deployer.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.eclipse.*;
import aQute.libg.version.*;

public class ProjectTest extends TestCase {

	
	
	/**
	 * Check isStale
	 */
	
	public void testIsStale() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
		Project top = ws.getProject("p-stale");
		assertNotNull(top);
		Project bottom = ws.getProject("p-stale-dep");
		assertNotNull(bottom);
		
		long lastModified = bottom.lastModified();
		top.getPropertiesFile().setLastModified(lastModified+1000);
		
		stale(top,true);
		stale(bottom,true);
		assertTrue(top.isStale());
		assertTrue(bottom.isStale());
		
		stale(top, false);
		stale(bottom, true);
		assertTrue(top.isStale());
		assertTrue(bottom.isStale());
		
		stale(top, true);
		stale(bottom, false);
		assertTrue(top.isStale());
		assertFalse(bottom.isStale());
		
//		Thread.sleep(1000);
//		stale(top, false);
//		stale(bottom, false);
//		assertFalse(top.isStale());
//		assertFalse(bottom.isStale());
	}
	
	
	
	private void stale(Project project, boolean b) throws Exception {
		File file = project.getBuildFiles(false)[0];
		if ( b ) 
			file.setLastModified(project.lastModified()-10000);
		else
			file.setLastModified(project.lastModified()+10000);
	}



	/**
	 * Check multiple repos
	 * 
	 * @throws Exception
	 */
	public void testMultipleRepos() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
		Project project = ws.getProject("p1");
		System.out.println(project.getBundle("org.apache.felix.configadmin", "1.1.0",
				Strategy.EXACT, null));
		System.out.println(project.getBundle("org.apache.felix.configadmin", "1.1.0",
				Strategy.HIGHEST, null));
		System.out.println(project.getBundle("org.apache.felix.configadmin", "1.1.0",
				Strategy.LOWEST, null));
	}

	/**
	 * Check if the getSubBuilders properly predicts the output.
	 */

	public void testSubBuilders() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
		Project project = ws.getProject("p4-sub");

		Collection<? extends Builder> bs = project.getSubBuilders();
		assertNotNull(bs);
		assertEquals(3, bs.size());
		Set<String> names = new HashSet<String>();
		for (Builder b : bs) {
			names.add(b.getBsn());
		}
		assertTrue(names.contains("p4-sub.a"));
		assertTrue(names.contains("p4-sub.b"));
		assertTrue(names.contains("p4-sub.c"));

		File[] files = project.build();
		System.out.println(Processor.join(project.getErrors(), "\n"));
		System.out.println(Processor.join(project.getWarnings(), "\n"));
		assertEquals(0, project.getErrors().size());
		assertEquals(0, project.getWarnings().size());
		assertNotNull(files);
		assertEquals(3, files.length);
		for (File file : files) {
			Jar jar = new Jar(file);
			Manifest m = jar.getManifest();
			assertTrue(names.contains(m.getMainAttributes().getValue("Bundle-SymbolicName")));
		}
	}

	/**
	 * Tests the handling of the -sub facility
	 * 
	 * @throws Exception
	 */

	public void testSub() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
		Project project = ws.getProject("p4-sub");
		File[] files = project.build();
		System.out.println(Processor.join(project.getErrors(), "\n"));
		System.out.println(Processor.join(project.getWarnings(), "\n"));

		assertEquals(0, project.getErrors().size());
		assertEquals(0, project.getWarnings().size());
		assertNotNull(files);
		assertEquals(3, files.length);

		Jar a = new Jar(files[0]);
		Jar b = new Jar(files[1]);
		Manifest ma = a.getManifest();
		Manifest mb = b.getManifest();

		assertEquals("base", ma.getMainAttributes().getValue("Base-Header"));
		assertEquals("base", mb.getMainAttributes().getValue("Base-Header"));
		assertEquals("a", ma.getMainAttributes().getValue("Sub-Header"));
		assertEquals("b", mb.getMainAttributes().getValue("Sub-Header"));
	}

	public void testOutofDate() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
		Project project = ws.getProject("p3");
		File bnd = new File("test/ws/p3/bnd.bnd");
		assertTrue(bnd.exists());

		project.clean();
		project.getTarget().mkdirs();
		try {
			// Now we build it.
			File[] files = project.build();
			System.out.println(project.getErrors());
			System.out.println(project.getWarnings());
			assertTrue(project.isOk());
			assertNotNull(files);
			assertEquals(1, files.length);

			// Now we should not rebuild it
			long lastTime = files[0].lastModified();
			files = project.build();
			assertEquals(1, files.length);
			assertTrue(files[0].lastModified() == lastTime);

			Thread.sleep(2000);

			project.updateModified(System.currentTimeMillis(), "Testing");
			files = project.build();
			assertEquals(1, files.length);
			assertTrue("Must have newer files now", files[0].lastModified() > lastTime);
		} finally {
			project.clean();
		}
	}

	public void testRepoMacro() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));
		Project project = ws.getProject("p2");
		System.out.println(project.getPlugins(FileRepo.class));
		String s = project.getReplacer().process(("${repo;libtest}"));
		System.out.println(s);
		assertTrue(s.contains("org.apache.felix.configadmin/org.apache.felix.configadmin-1.2.0"));
		assertTrue(s.contains("org.apache.felix.ipojo/org.apache.felix.ipojo-1.0.0.jar"));

		s = project.getReplacer().process(("${repo;libtestxyz}"));
		assertTrue(s.matches("<<[^>]+>>"));

		s = project.getReplacer().process("${repo;org.apache.felix.configadmin;1.0.0;highest}");
		s.endsWith("org.apache.felix.configadmin-1.1.0.jar");
		s = project.getReplacer().process("${repo;org.apache.felix.configadmin;1.0.0;lowest}");
		s.endsWith("org.apache.felix.configadmin-1.0.1.jar");
	}

	public void testClasspath() throws Exception {
		File project = new File("").getAbsoluteFile();
		File workspace = project.getParentFile();
		Processor processor = new Processor();
		EclipseClasspath p = new EclipseClasspath(processor, workspace, project);
		System.out.println(p.getDependents());
		System.out.println(p.getClasspath());
		System.out.println(p.getSourcepath());
		System.out.println(p.getOutput());
	}

	public void testBump() throws Exception {
		File tmp = new File("tmp-ws");
		if (tmp.exists())
			IO.delete(tmp);
		tmp.mkdir();
		assertTrue(tmp.isDirectory());

		try {
			IO.copy(new File("test/ws"), tmp);
			Workspace ws = Workspace.getWorkspace(tmp);
			Project project = ws.getProject("p1");
			int size = project.getProperties().size();
			Version old = new Version(project.getProperty("Bundle-Version"));
			System.out.println("Old version " + old);
			project.bump("=+0");
			Version newv = new Version(project.getProperty("Bundle-Version"));
			System.out.println("New version " + newv);
			assertEquals(old.getMajor(), newv.getMajor());
			assertEquals(old.getMinor() + 1, newv.getMinor());
			assertEquals(0, newv.getMicro());
			assertEquals(size, project.getProperties().size());
			assertEquals("sometime", newv.getQualifier());
		} finally {
			IO.delete(tmp);
		}
	}

	public void testBumpIncludeFile() throws Exception {
		File tmp = new File("tmp-ws");
		if (tmp.exists())
			IO.delete(tmp);
		tmp.mkdir();
		assertTrue(tmp.isDirectory());

		try {
			IO.copy(new File("test/ws"), tmp);
			Workspace ws = Workspace.getWorkspace(tmp);
			Project project = ws.getProject("bump-included");
			project.setTrace(true);
			Version old = new Version(project.getProperty("Bundle-Version"));
			assertEquals( new Version(1,0,0), old);
			project.bump("=+0");
			
			Processor processor = new Processor();
			processor.setProperties(project.getFile("include.txt"));
			
			
			Version newv = new Version(processor.getProperty("Bundle-Version"));
			System.out.println("New version " + newv);
			assertEquals(1, newv.getMajor());
			assertEquals(1, newv.getMinor());
			assertEquals(0, newv.getMicro());
		} finally {
			IO.delete(tmp);
		}
	}
	
	public void testBumpSubBuilders() throws Exception {
		File tmp = new File("tmp-ws");
		if (tmp.exists())
			IO.delete(tmp);
		tmp.mkdir();
		assertTrue(tmp.isDirectory());

		try {
			IO.copy(new File("test/ws"), tmp);
			Workspace ws = Workspace.getWorkspace(tmp);
			Project project = ws.getProject("bump-sub");
			project.setTrace(true);
			
			assertNull( project.getProperty("Bundle-Version"));
			
			project.bump("=+0");
			
			assertNull( project.getProperty("Bundle-Version"));

			for ( Builder b : project.getSubBuilders()) {
				assertEquals( new Version(1,1,0), new Version(b.getVersion()));
			}
		} finally {
			IO.delete(tmp);
		}
	}
	public void testRunBuilds() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File("test/ws"));

		// Running a .bnd includes built bundles by default
		Project p1 = ws.getProject("p1");
		assertTrue(p1.getRunBuilds());

		// Can override the default by specifying -runbuilds: false
		Project p2 = ws.getProject("p2");
		assertFalse(p2.getRunBuilds());

		// Running a .bndrun DOES NOT include built bundles by default
		Project p1a = new Project(ws, new File("test/ws/p1"), new File("test/ws/p1/p1a.bndrun"));
		assertFalse(p1a.getRunBuilds());

		// ... unless we override the default by specifying -runbuilds: true
		Project p1b = new Project(ws, new File("test/ws/p1"), new File("test/ws/p1/p1b.bndrun"));
		assertTrue(p1b.getRunBuilds());
	}

}
