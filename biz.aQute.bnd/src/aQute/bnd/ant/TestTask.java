package aQute.bnd.ant;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Workspace;

public class TestTask extends BaseTask {

	private boolean	continuous	= false;
	private String	runFiles	= null;
	private File	dir			= null;

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
				projects = new LinkedList<>();
				while (tokenizer.hasMoreTokens()) {
					String runFilePath = tokenizer.nextToken()
						.trim();
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

			// Bnd #372: clear projects first, to ensure projects with multiple
			// runfiles do not clear previous results...
			for (Project project : projects) {
				project.clear();
			}

			// Test them
			for (Project project : projects) {
				executeProject(project);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}

	private void executeProject(Project project) throws Exception {

		ProjectTester tester = project.getProjectTester();
		tester.setContinuous(continuous);
		if (dir != null)
			tester.setCwd(dir);
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

	public void setDir(File dir) {
		this.dir = dir;
	}

}
