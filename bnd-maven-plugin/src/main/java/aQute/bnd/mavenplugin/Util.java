package aQute.bnd.mavenplugin;

import aQute.bnd.build.Project;

import org.apache.maven.project.MavenProject;

public class Util {
    private Util() {}

    static void setBndDirsInMvnProject(MavenProject mavenProject, Project bndProject) throws Exception {
        mavenProject.getBuild().setDirectory(bndProject.getTarget().getAbsolutePath());
        mavenProject.getBuild().setSourceDirectory(bndProject.getSrc().getAbsolutePath());
        mavenProject.getBuild().setOutputDirectory(bndProject.getOutput().getAbsolutePath());

        return;
    }
}
