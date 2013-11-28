package aQute.bnd.mavenplugin;

import aQute.bnd.build.Project;

import org.apache.maven.project.MavenProject;

public class Util {
    private Util() {}

    static void setMavenDefaultsInBndProject(MavenProject mavenProject, Project bndProject) {
        bndProject.setProperty("bin", mavenProject.getBasedir() + "/target/classes");
        bndProject.setProperty("target", mavenProject.getBasedir() + "/target");
    }
}
