package aQute.bnd.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Workspace;
import aQute.lib.osgi.Constants;
import aQute.libg.qtokens.QuotedTokenizer;

public class TestTask extends BaseTask {
	
	private boolean continuous = false;
	List<File>	files		= new ArrayList<File>();
	
	public void setFiles(String files) {
		addAll(this.files, files, ",");
	}
	
	void addAll(List<File> list, String files, String separator) {
		trace("addAll '%s' with %s", files, separator);
		QuotedTokenizer qt = new QuotedTokenizer(files, separator);
		String entries[] = qt.getTokens();
		File project = getProject().getBaseDir();
		for (int i = 0; i < entries.length; i++) {
			File f = getFile(project, entries[i]);
			if (f.exists())
				list.add(f);
			else
				error("Can not find file to process: " + f.getAbsolutePath());
		}
	}
	
	@Override
	public void execute() throws BuildException {
		try {
			Project project = Workspace.getProject(getProject().getBaseDir());
			project.clear();
			
			//[cs] process --runfile
			for(File f : this.files) {
				project.setDelayRunDependencies(true);
				// setting RUNBUILDS to false makes it so the project itself
				// isn't included in the runbundles automatically.
				project.setProperty(Constants.RUNBUILDS, "false");
				project.doIncludeFile(f, true, project.getProperties());
			}
			
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
