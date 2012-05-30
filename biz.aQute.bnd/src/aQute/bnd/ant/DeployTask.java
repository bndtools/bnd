package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;

public class DeployTask extends BaseTask {
	private String deployRepo = null;
    List<FileSet> filesets = new ArrayList<FileSet>();

    public void execute() throws BuildException {
        try {
            Project project = Workspace.getProject(getProject().getBaseDir());

            // Deploy the files that need to be released
            for (FileSet fileset : filesets) {
                DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
                String[] files = ds.getIncludedFiles();
                if (files.length == 0)
                    trace("No files included");

                for (int i = 0; i < files.length; i++) {
                    File file = new File(ds.getBasedir(), files[i]);
                    try {
                        if (file.isFile() && file.getName().endsWith(".jar")) {
                            if (deployRepo != null) project.deploy(deployRepo, file);
                            else project.deploy(file);
                        } else
                            error("Not a jar file: " + file);
                    } catch (Exception e) {
                        error("Failed to deploy " + file + " : " + e);
                    }
                }
            }
            report(project.getWorkspace());
            if (project.getErrors().size() > 0)
                throw new BuildException("Deploy failed");
        } catch (Throwable t) {
            t.printStackTrace();
            throw new BuildException(t);
        }
    }
    
    public void setDeployrepo(String name) {
    	this.deployRepo = name;
    }

    public void addFileset(FileSet files) {
        this.filesets.add(files);
    }

}
