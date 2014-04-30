package bndtools.wizards.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.version.Version;
import bndtools.Plugin;
import bndtools.central.RefreshFileJob;
import bndtools.types.Pair;

public class AddFilesToRepositoryWizard extends Wizard {

    private RepositoryPlugin repository;
    private final File[] files;
    private List<Pair<String,String>> selectedBundles;

    private final LocalRepositorySelectionPage repoSelectionPage;
    private final AddFilesToRepositoryWizardPage fileSelectionPage;
    private boolean strict = true;

    public AddFilesToRepositoryWizard(RepositoryPlugin repository, File[] initialFiles) {
        this.repository = repository;
        this.files = initialFiles;

        if (repository != null && "aQute.lib.deployer.FileRepo".equals(repository.getClass().getName())) {
            strict = false;
        }

        repoSelectionPage = new LocalRepositorySelectionPage("repoSelectionPage", repository);

        fileSelectionPage = new AddFilesToRepositoryWizardPage(repository, "fileSelectionPage");
        fileSelectionPage.setFiles(files);
    }

    @Override
    public void addPages() {
        if (repository == null) {
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
            String bsn = null;
            Version version = null;
            Jar jar = null;
            try {
                jar = new Jar(file);
                jar.setDoNotTouchManifest();

                bsn = jar.getBsn(strict);
                if (bsn == null && !strict) {
                    bsn = Jar.getBsnFromFileName(file.getName(), strict);
                }

                try {
                    version = Version.fromManifest(jar.getManifest(), strict);
                } catch (Exception e) {
                    /* swallow */
                }
                if (version == null && !strict) {
                    version = Version.fromFileName(file.getName(), strict);
                }

                if (version == null) {
                    version = Version.LOWEST;
                }
                assert (version != null);

                selectedBundles.add(Pair.newInstance(bsn, version.toString()));
            } catch (Exception e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to analyse JAR: {0}", file.getPath()), e));
                continue;
            } finally {
                if (jar != null)
                    jar.close();
            }

            try {
                PutOptions options = new RepositoryPlugin.PutOptions();
                options.bsnHint = bsn;
                options.versionHint = version;
                RepositoryPlugin.PutResult result = repository.put(new BufferedInputStream(new FileInputStream(file)), options);
                if (result.artifact != null && result.artifact.getScheme().equals("file")) {
                    File newFile = new File(result.artifact);

                    RefreshFileJob refreshJob = new RefreshFileJob(newFile, false);
                    if (refreshJob.needsToSchedule())
                        refreshJob.schedule();
                }
            } catch (Exception e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to add JAR to repository: {0}", file.getPath()), e));
                continue;
            }

        }

        if (status.isOK()) {
            return true;
        }
        ErrorDialog.openError(getShell(), "Error", null, status);
        return false;
    }

    public List<Pair<String,String>> getSelectedBundles() {
        return Collections.unmodifiableList(selectedBundles);
    }
}
