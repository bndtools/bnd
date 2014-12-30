package bndtools.wizards.bndfile;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.osgi.Jar;

class GenerateLauncherJarRunnable implements IRunnableWithProgress {

    private final Project project;
    private final String jarPath;

    GenerateLauncherJarRunnable(Project project, String jarPath) {
        this.project = project;
        this.jarPath = jarPath;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
            ProjectLauncher launcher = project.getProjectLauncher();
            Jar jar = launcher.executable();
            jar.write(jarPath);
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

}
