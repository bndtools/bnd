package bndtools.wizards.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import bndtools.Plugin;
import bndtools.RefreshFileJob;
import bndtools.types.Pair;
import bndtools.utils.BundleUtils;

public class AddFilesToRepositoryWizard extends Wizard {

    private RepositoryPlugin repository;
    private final File[] files;
    private List<Pair<String, String>> selectedBundles;

    private LocalRepositorySelectionPage repoSelectionPage;
    private AddFilesToRepositoryWizardPage fileSelectionPage;

    public AddFilesToRepositoryWizard(RepositoryPlugin repository, File[] initialFiles) {
        this.repository = repository;
        this.files = initialFiles;

        repoSelectionPage = new LocalRepositorySelectionPage("repoSelectionPage", repository);

        fileSelectionPage = new AddFilesToRepositoryWizardPage("fileSelectionPage");
        fileSelectionPage.setFiles(files);
    }

    @Override
    public void addPages() {
        if(repository == null) {
            addPage(repoSelectionPage);
            repoSelectionPage.addPropertyChangeListener(LocalRepositorySelectionPage.PROP_SELECTED_REPO, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    repository = (RepositoryPlugin) evt.getNewValue();
                }
            });
        }
        addPage(fileSelectionPage);
    }

    @Override
    public boolean performFinish() {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Failed to install one or more bundles", null);

        List<File> files = fileSelectionPage.getFiles();
        selectedBundles = new LinkedList<Pair<String,String>>();
        for (File file : files) {
            Jar jar = null;
            try {
                 jar = new Jar(file);
                 jar.setDoNotTouchManifest();

                 Attributes mainAttribs = jar.getManifest().getMainAttributes();
                 String bsn = BundleUtils.getBundleSymbolicName(mainAttribs);
                 String version = mainAttribs.getValue(Constants.BUNDLE_VERSION);
                 if(version == null) version = "0";
                 selectedBundles.add(Pair.newInstance(bsn, version));
            } catch (Exception e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to analyse JAR: {0}", file.getPath()), e));
                continue;
            }

            try {
                File newFile = repository.put(jar);

                RefreshFileJob refreshJob = new RefreshFileJob(newFile);
                if(refreshJob.isFileInWorkspace())
                    refreshJob.schedule();
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

    public List<Pair<String, String>> getSelectedBundles() {
        return Collections.unmodifiableList(selectedBundles);
    }
}

