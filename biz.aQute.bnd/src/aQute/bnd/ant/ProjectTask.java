package aQute.bnd.ant;

/**
 * The idea of this task is to read all the properties as if bnd has read them.
 * This makes it easier to use bnd standalone on the same data.
 */

import java.io.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;

public class ProjectTask extends BaseTask {
    File basedir;
    boolean underTest;
    
    public void execute() throws BuildException {
        try {
            if (basedir == null || !basedir.isDirectory())
                throw new BuildException("The given base dir does not exist "
                        + basedir);

            Project project = Workspace.getProject(basedir);
            project.build(underTest);
            report(project.getWorkspace());
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e);
        }
    }

    /**
     * Set the base directory of the project. This property MUST be set.
     * 
     * @param basedir
     */
    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }
    
    
    public void setUnderTest(boolean underTest) {
        this.underTest = underTest;
    }
}
