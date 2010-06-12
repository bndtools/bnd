package bndtools.wizards.workspace;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Jar;
import bndtools.Plugin;

public class AddFilesToRepositoryWizard extends Wizard {

    private final RepositoryPlugin repository;
    private final File[] files;

    private AddFilesToRepositoryWizardPage fileSelectionPage;

    public AddFilesToRepositoryWizard(RepositoryPlugin repository, File[] initialFiles) {
        this.repository = repository;
        this.files = initialFiles;

        fileSelectionPage = new AddFilesToRepositoryWizardPage("fileSelectionPage");
        fileSelectionPage.setFiles(files);

        addPage(fileSelectionPage);
    }

    @Override
    public boolean performFinish() {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Failed to install one or more bundles", null);

        List<File> files = fileSelectionPage.getFiles();
        for (File file : files) {
            Jar jar = null;
            try {
                 jar = new Jar(file);
                 jar.setDoNotTouchManifest();
            } catch (IOException e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to analyse JAR: {0}", file.getPath()), e));
                continue;
            }

            try {
                repository.put(jar);
            } catch (Exception e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to add JAR to repository: {0}", file.getPath()), e));
                continue;
            }
        }

        if(status.isOK()) {
            return true;
        } else {
            ErrorDialog.openError(getShell(), "Error", null, status);
            return false;
        }
    }
}
