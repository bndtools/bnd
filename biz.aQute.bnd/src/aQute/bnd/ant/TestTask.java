package aQute.bnd.ant;

import org.apache.tools.ant.BuildException;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Workspace;

public class TestTask extends BaseTask {
	
	private boolean continuous = false;
	
	@Override
	public void execute() throws BuildException {
		try {
			Project project = Workspace.getProject(getProject().getBaseDir());
			project.clear();
			
			ProjectTester tester = project.getProjectTester();
			tester.setContinuous(continuous);
			tester.prepare();

			if (report(project))
				throw new BuildException("Failed to initialise for testing.");
			
			int errors = tester.test();
			if (errors == 0) {
				System.err.println("All tests passed");
			} else {
				if (errors > 0) {
					System.err.println(errors + " Error(s)");
				} else
					System.err.println("Error " + errors);
				throw new BuildException("Tests failed");
			}
			
			if (report(project))
				throw new BuildException("Tests failed");
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}
	
}
