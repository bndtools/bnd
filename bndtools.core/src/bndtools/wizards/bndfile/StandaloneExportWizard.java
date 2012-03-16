package bndtools.wizards.bndfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Manifest;

import org.bndtools.core.ui.IRunDescriptionExportWizard;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.ui.refactoring.reorg.CopyToClipboardAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.lib.io.IO;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;
import aQute.lib.osgi.URLResource;
import aQute.libg.header.Attrs;
import aQute.libg.header.Parameters;
import bndtools.Plugin;
import bndtools.api.IBndModel;

public class StandaloneExportWizard extends Wizard implements IRunDescriptionExportWizard {
    
    private final StandaloneExportWizardPage destinationPage = new StandaloneExportWizardPage();

    private IBndModel model;
    private Project bndProject;
    
    public StandaloneExportWizard() {
        addPage(destinationPage);
    }
    
    public void setBndModel(IBndModel model, Project bndProject) {
        this.model = model;
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
        
        bundleFolder.mkdirs();
        if (!bundleFolder.exists()) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unable to create folder.", null));
            return status;
        }
        
        ProjectLauncher launcher = null;
        try {
            launcher = bndProject.getProjectLauncher();
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting project launcher.", e));
        }
        
        // Init classpath and launch JAR
        generateLauncherJar(launcher, folder, status);
        copyRunBundles(launcher, folder, status);
        
        return status;
    }

    private IStatus generateJar(String jarPath) {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Errors occurred during exporting.", null);
        ProjectLauncher launcher = null;
        try {
            launcher = bndProject.getProjectLauncher();
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting project launcher.", e));
        }
        
        try {
            Jar jar = launcher.executable();
            jar.write(jarPath);
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating executable JAR.", e));
        }
        return status;
    }
    
    private void generateLauncherJar(ProjectLauncher launcher, File folder, MultiStatus status) {
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
        launcherJar.putResource("launch.class", new URLResource(StandaloneExportWizard.class.getResource("launch.clazz")));
        

        try {
            launcherJar.write(new File(folder, "launch.jar"));
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating launch JAR.", e));
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
        
        try {
            Properties launcherProps = new Properties();
            launcherProps.put(aQute.lib.osgi.Constants.RUNBUNDLES, Processor.join(names, ",\\\n  "));
            launcherProps.store(new FileOutputStream(new File(folder, "launch.properties")), "launch.properties");
        } catch (IOException e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating launch properties file.", e));
        }
    }

    private void copyBundles(MultiStatus status, File folder, Parameters jarSpec) {
        for (Entry<String, Attrs> entry : jarSpec.entrySet()) {
            String bsn = entry.getKey();
            String version = entry.getValue().get(Constants.VERSION_ATTRIBUTE, Version.emptyVersion.toString());
            
            copyBundle(status, folder, bsn, version);
        }
    }

    private void copyBundle(MultiStatus status, File folder, String bsn, String version) {
        Container container = null;
        try {
            container = bndProject.getBundle(bsn, version, Strategy.HIGHEST, null);
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error resolving OSGi framework.", e));
        }
        if (container == null) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("Name: %s, version: %s", bsn, version), null));
        } else if (container.getType() == TYPE.ERROR) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error resolving OSGi framework: " + container.getError(), null));
        } else  {
            try {
                File source = container.getFile();
                File dest = new File(folder, source.getName());
                IO.copy(source, dest);
            } catch (IOException e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to copy file to ", e));
            }
        }
    }

}
