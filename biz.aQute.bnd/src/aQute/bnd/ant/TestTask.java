package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;

public class TestTask extends BaseTask {

	private boolean	continuous	= false;
	private String	runFiles	= null;

	@Override
	public void execute() throws BuildException {

		try {
			// Prepare list of projects...
			List<Project> projects;
			File baseDir = getProject().getBaseDir();
			Project baseProject = Workspace.getProject(baseDir);
			if (runFiles == null) {
				projects = Collections.singletonList(baseProject);
			} else {
				StringTokenizer tokenizer = new StringTokenizer(runFiles, ",");
				projects = new LinkedList<Project>();
				while (tokenizer.hasMoreTokens()) {
					String runFilePath = tokenizer.nextToken().trim();
					Project runProject;
					if (".".equals(runFilePath)) {
						runProject = baseProject;
					} else {
						File runFile = new File(baseDir, runFilePath);
						if (!runFile.isFile())
							throw new BuildException(String.format("Run file %s does not exist (or is not a file).",
									runFile.getAbsolutePath()));
						runProject = new Project(baseProject.getWorkspace(), baseDir, runFile);
						runProject.setParent(baseProject);
					}
					projects.add(runProject);
				}
			}

			// Test them
			for (Project project : projects) {
				executeProject(project);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}

	private void executeProject(Project project) throws Exception {
		System.out.println("Testing " + project.getPropertiesFile());
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
	}

	public void setRunfiles(String runFiles) {
		this.runFiles = runFiles;
	}

}
