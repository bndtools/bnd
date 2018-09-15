package test;

import java.io.File;
import java.util.List;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import junit.framework.TestCase;

public class TestSelfBuild extends TestCase {

	public static void testSelfBuild() throws Throwable {
		try (Project project = Workspace.getWorkspace(new File("").getAbsoluteFile()
			.getParentFile())
			.getProject("biz.aQute.bndlib")) {
			project.setPedantic(true);
			try (ProjectBuilder pb = project.getBuilder(null)) {
				List<Builder> subBuilders = pb.getSubBuilders();
				assertEquals(1, subBuilders.size());

				Builder b = subBuilders.iterator()
					.next();
				b.build();
				assertTrue(b.check("Imports that lack version ranges: \\[javax"));
			}
		}
	}
}
