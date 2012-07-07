package bndtools.wizards.bndfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

import org.bndtools.core.ui.IRunDescriptionExportWizard;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.model.BndEditModel;
import aQute.lib.io.IO;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;
import aQute.lib.osgi.URLResource;
import bndtools.Plugin;

public class ExecutableJarExportWizard extends Wizard implements IRunDescriptionExportWizard {

    private final ExecutableJarWizardPage destinationPage = new ExecutableJarWizardPage();

    private Project bndProject;

    public ExecutableJarExportWizard() {
        addPage(destinationPage);
    }

    public void setBndModel(BndEditModel model, Project bndProject) {
        this.bndProject = bndProject;
    }

    @Override
    public boolean performFinish() {
        IStatus status = Status.OK_STATUS;
        if (destinationPage.isFolder())
            status = generateFolder(destinationPage.getFolderPath());
        else
            status = generateJar(destinationPage.getJarPath());

        if (!status.isOK())
            ErrorDialog.openError(getShell(), "Error", null, status);

        return status.isOK();
    }

    private IStatus generateFolder(String folderPath) {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Errors occurred during exporting.", null);

        File folder = new File(folderPath);
        File bundleFolder = new File(folder, "bundles");

        if (!bundleFolder.exists() && !bundleFolder.mkdirs()) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unable to create folder.", null));
            return status;
        }

        ProjectLauncher launcher = null;
        try {
            launcher = bndProject.getProjectLauncher();
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting project launcher.", e));
            return status;
        }

        // Init classpath and launch JAR
        generateLauncherJar(launcher, folder, status);
        copyRunBundles(launcher, folder, status);

        return status;
    }

    private IStatus generateJar(String jarPath) {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Errors occurred during exporting.", null);
        try {
            ProjectLauncher launcher = bndProject.getProjectLauncher();
            Jar jar = launcher.executable();
            jar.write(jarPath);
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating executable JAR.", e));
        }
        return status;
    }

    private static void generateLauncherJar(ProjectLauncher launcher, File folder, MultiStatus status) {
        Jar launcherJar = new Jar("launch");

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
        } finally {
            launcherJar.close();
        }
    }

    private static void copyRunBundles(ProjectLauncher launcher, File folder, MultiStatus status) {
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
        launcherProps.put(aQute.lib.osgi.Constants.RUNBUNDLES, Processor.join(names, ",\\\n  "));

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
