package bndtools.wizards.bndfile;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;

class GenerateLauncherJarRunnable implements IRunnableWithProgress {

    private final Project project;
    private final String path;
    private final boolean folder;

    GenerateLauncherJarRunnable(Project project, String path, boolean folder) {
        this.project = project;
        this.path = path;
        this.folder = folder;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
            ProjectLauncher launcher = project.getProjectLauncher();
            Jar jar = launcher.executable();
            project.getInfo(launcher);

            if (folder) {
                File folder = new File(path);
                jar.writeFolder(folder);

                File start = IO.getFile(folder, "start");
                if (start.isFile())
                    start.setExecutable(true);
            } else
                jar.write(path);

        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

}
