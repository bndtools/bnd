package aQute.bnd.build;

import java.io.*;
import java.util.*;

import aQute.lib.osgi.*;

public class ProjectBuilder extends Builder {
    Project project;
    boolean initialized;

    public ProjectBuilder(Project project) {
        super(project);
        this.project = project;
    }

    public ProjectBuilder(ProjectBuilder builder) {
        super(builder);
        this.project = builder.project;
    }

    @Override
    public long lastModified() {
        return Math.max(project.lastModified(), super.lastModified());
    }

    /**
     * We put our project and our workspace on the macro path.
     */
    protected Object[] getMacroDomains() {
        return new Object[] { project, project.getWorkspace() };
    }

    public Builder getSubBuilder() throws Exception {
        return project.getBuilder(this);
    }

    public Project getProject() {
        return project;
    }

    public void init() {
        try {
            if (!initialized) {
                initialized = true;
                for (Container file : project.getClasspath()) {
                    addClasspath(file.getFile());
                }
                
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
        } catch (Exception e) {
            error("init project builder fails", e);
        }
    }

    public List<Jar> getClasspath() {
        init();
        return super.getClasspath();
    }

    @Override
    protected void changedFile(File f) {
        project.getWorkspace().changedFile(f);
    }
}
