package test;

import java.io.*;
import java.util.regex.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.osgi.*;
import aQute.lib.io.*;
import aQute.service.reporter.*;

public class LocationTest extends TestCase {
	Workspace	ws;

	public void setUp() throws Exception {
		File tmp = new File("tmp");
		IO.copy(new File("test/ws-location"), tmp);
		ws = new Workspace(tmp);

	}

	public void tearDown() throws Exception {
		IO.delete(new File("tmp"));
	}

	public void testBasic() throws Exception {
		Project project = ws.getProject("p1");
		assertNotNull(project);
		
		ProjectBuilder sub1 = project.getSubBuilder("sub1");
		assertNotNull(sub1);
		
		ProjectBuilder sub2 = project.getSubBuilder("sub2");
		assertNotNull(sub2);
		
		assertTrue( find( sub1, "sub1", "p1/sub1.bnd", 4));
		assertTrue( find( sub1, "bnd.bnd", "p1/bnd.bnd", 4));
		assertTrue( find( project, "bnd.bnd", "p1/bnd.bnd", 4));
		assertTrue( find( sub1, "i1", "p1/i1.bnd", 2));
		assertTrue( find( project, "i1", "p1/i1.bnd", 2));
		assertTrue( find( sub1, "i2", "p1/i2.bnd", 2));
		assertTrue( find( project, "i2", "p1/i2.bnd", 2));
		assertTrue( find( sub2, "sub2", "p1/sub2.bnd", 3));
		assertTrue( find( sub2, "bnd.bnd", "p1/bnd.bnd", 4));
		assertTrue( find( sub2, "workspace", "cnf/build.bnd", 6));
	}

	private boolean find(Processor p, String what, String file, int line) throws IOException {
		Reporter.Location l = new Reporter.Location();
		Pattern pattern = Pattern.compile("^"+what, Pattern.MULTILINE);
		
		assertTrue( what, p.getHeader(l,pattern ));
		assertTrue( l.file.endsWith(file));
		assertEquals(line,l.line);
		return true;
	}

}
