package bndtools.wizards.bndfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;

import bndtools.Plugin;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.URLResource;
import aQute.lib.io.IO;

public class GenerateLauncherFolderRunnable implements IRunnableWithProgress {

    private final Project project;
    private final String folderPath;

    public GenerateLauncherFolderRunnable(Project project, String folderPath) {
        this.project = project;
        this.folderPath = folderPath;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        File folder = new File(folderPath);
        File bundleFolder = new File(folder, "bundles");
        bundleFolder.mkdirs();
        if (!bundleFolder.isDirectory())
            throw new InvocationTargetException(new Exception("Unable to create folder: " + bundleFolder));

        ProjectLauncher launcher = null;
        try {
            launcher = project.getProjectLauncher();
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }

        // Init classpath and launch JAR
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Errors occurred during export.", null);
        generateLauncherJar(launcher, folder, status);
        copyRunBundles(launcher, folder, status);
        if (!status.isOK())
            throw new InvocationTargetException(new CoreException(status));
    }

    private void generateLauncherJar(ProjectLauncher launcher, File folder, MultiStatus status) {
        Jar launcherJar = new Jar("launch");
        try {
            // Merge in the classpath JARs
            Collection<String> classpath = launcher.getClasspath();
            for (String classpathEntry : classpath) {
                try {
                    Jar classpathJar = new Jar(new File(classpathEntry));
                    launcherJar.addAll(classpathJar);
                } catch (IOException e) {
                    status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("Failed to add classpath JAR '%s'.", classpathEntry), e));
                }
            }

            // Set the Main-Class
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().putValue("Main-Class", "launch");
            launcherJar.setManifest(manifest);
            launcherJar.putResource("launch.class", new URLResource(ExecutableJarExportWizard.class.getResource("launch.clazz")));

            try {
                launcherJar.write(new File(folder, "launch.jar"));
            } catch (Exception e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating launch JAR.", e));
            }
        } finally {
            launcherJar.close();
        }

    }

    private void copyRunBundles(ProjectLauncher launcher, File folder, MultiStatus status) {
        Collection<String> bundles = launcher.getRunBundles();
        List<String> names = new ArrayList<String>(bundles.size());

        for (String bundle : bundles) {
            File bundleFile = new File(bundle);
            String name = "bundles/" + bundleFile.getName();
            File destFile = new File(folder, name);

            try {
                IO.copy(bundleFile, destFile);
                names.add(name);
            } catch (IOException e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error copying run bundle: " + bundle, e));
            }
        }

        Properties launcherProps = new Properties();
        launcherProps.put(aQute.bnd.osgi.Constants.RUNBUNDLES, Processor.join(names, ",\\\n  "));

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(folder, "launch.properties"));
            launcherProps.store(fos, "launch.properties");
        } catch (IOException e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating launch properties file.", e));
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {}
        }
    }

}
