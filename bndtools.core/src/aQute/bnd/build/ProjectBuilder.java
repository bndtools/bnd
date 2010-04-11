package aQute.bnd.build;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import aQute.lib.osgi.*;

@SuppressWarnings("unchecked")
public class ProjectBuilder extends Builder {
    Project project;

    public ProjectBuilder(Project project) {
        super(project);
        this.project = project;
    }

    public ProjectBuilder(ProjectBuilder builder) {
        super(builder);
        this.project = builder.project;
    }


    /** 
     * We put our project and our workspace on the macro path.
     */
    protected Object [] getMacroDomains() {
        return new Object[] {project, project.getWorkspace()};
    }

    public Builder getSubBuilder() throws Exception {
        return project.getBuilder(this);
    }

    public Project getProject() {
        return project;
    }
    
    public void init() throws Exception {
        for (Container file : project.getBuildpath()) {
            addClasspath(file.getFile());
        }

        for (Container file : project.getBootclasspath()) {
            addClasspath(file.getFile());
        }

        for (File file : project.getAllsourcepath()) {
            addSourcepath(file);
        }

    }
    
    @Override
    protected void changedFile(File f) {
        project.getWorkspace().changedFile(f);
    }
    
    @Override
    protected void begin() {
    	super.begin();
    	updateModified(project.lastModified(), "Project last modified");
    }
    
    @Override
    public String getClasspathEntrySuffix(File resource) throws Exception {
    	String result = super.getClasspathEntrySuffix(resource);
    	if(result != null)
    		return result;
    	
    	Collection<Container> buildpath = project.getBuildpath();
    	for (Container container : buildpath) {
    		File file = container.getFile();
    		if(file != null) {
    			file = file.getCanonicalFile();
    			String filePath = file.getAbsolutePath();
    			String resourcePath = resource.getAbsolutePath();
    			
    			if(resourcePath.startsWith(filePath)) {
    				result = resourcePath.substring(filePath.length() + 1);
    				break;
    			}
    		}
		}
    	
    	return result;
    }
}