package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class LocationTest {
	Workspace		ws;
	@InjectTemporaryDirectory
	File		tmp;

	@BeforeEach
	protected void setUp() throws Exception {
		IO.copy(IO.getFile("testresources/ws-location"), tmp);
		ws = new Workspace(tmp);
	}

	@AfterEach
	protected void tearDown() throws Exception {
		ws.close();
	}

	@Test
	public void testMerged() throws Exception {
		try (Project project = ws.getProject("locationtest")) {
			FileLine fl = project.getHeader("-merged", "BAZ");
			assertNotNull(fl);
			assertEquals(project.getPropertiesFile()
				.getAbsolutePath(), fl.file.getAbsolutePath());
			assertEquals(18, fl.line);
			assertEquals(167, fl.start);
			assertEquals(170, fl.end);
		}
	}

	@Test
	public void testProjectHeaderClauses() throws Exception {
		try (Project project = ws.getProject("locationtest")) {
			assertNotNull(project);

			FileLine fl = project.getHeader("-inprojectsep", "BAZ");
			assertNotNull(fl);
			assertEquals(project.getPropertiesFile()
				.getAbsolutePath(), fl.file.getAbsolutePath());
			assertEquals(10, fl.line);
			assertEquals(104, fl.start);
			assertEquals(107, fl.end);

			fl = project.getHeader("-inproject", "BAZ");
			assertNotNull(fl);
			assertEquals(project.getPropertiesFile()
				.getAbsolutePath(), fl.file.getAbsolutePath());
			assertEquals(3, fl.line);
			assertEquals(23, fl.start);
			assertEquals(26, fl.end);
		}
	}

	@Test
	public void testHeaderInSub() throws Exception {
		try (Project project = ws.getProject("locationtest"); ProjectBuilder pb = project.getBuilder(null)) {
			Builder builder = pb.getSubBuilders()
				.get(0);
			assertNotNull(builder);

			FileLine fl = builder.getHeader("-inprojectsep", "BAZ");
			assertNotNull(fl);
			assertEquals(project.getPropertiesFile()
				.getAbsolutePath(), fl.file.getAbsolutePath());
			assertEquals(10, fl.line);
			assertEquals(104, fl.start);
			assertEquals(107, fl.end);
		}

	}

	@Test
	public void testBasic() throws Exception {
		try (Project project = ws.getProject("p1")) {
			assertNotNull(project);

			ProjectBuilder sub1 = project.getSubBuilder("sub1");
			assertNotNull(sub1);

			ProjectBuilder sub2 = project.getSubBuilder("sub2");
			assertNotNull(sub2);

			assertTrue(find(sub1, "sub1", "p1/sub1.bnd", 4));
			assertTrue(find(sub1, "bnd.bnd", "p1/bnd.bnd", 4));
			assertTrue(find(project, "bnd.bnd", "p1/bnd.bnd", 4));
			assertTrue(find(sub1, "i1", "p1/i1.bnd", 2));
			assertTrue(find(project, "i1", "p1/i1.bnd", 2));
			assertTrue(find(sub1, "i2", "p1/i2.bnd", 2));
			assertTrue(find(project, "i2", "p1/i2.bnd", 2));
			assertTrue(find(sub2, "sub2", "p1/sub2.bnd", 3));
			assertTrue(find(sub2, "bnd.bnd", "p1/bnd.bnd", 4));
			assertTrue(find(sub2, "workspace", "cnf/build.bnd", 6));
			assertTrue(find(project, "workspace", "cnf/build.bnd", 6));
			assertTrue(find(project.getWorkspace(), "workspace", "cnf/build.bnd", 6));
		}
	}

	private boolean find(Processor p, String what, String file, int line) throws Exception {
		Pattern pattern = Pattern.compile("^" + what, Pattern.MULTILINE);
		Processor.FileLine fl = p.getHeader(pattern);
		assertNotNull(fl);
		assertTrue(IO.absolutePath(fl.file)
			.endsWith(file));
		assertEquals(line, fl.line);
		return true;
	}

}
