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
import java.util.jar.Attributes;

import org.bndtools.utils.osgi.BundleUtils;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;
import bndtools.central.RefreshFileJob;
import bndtools.types.Pair;

public class AddFilesToRepositoryWizard extends Wizard {

    private RepositoryPlugin repository;
    private final File[] files;
    private List<Pair<String,String>> selectedBundles;

    private final LocalRepositorySelectionPage repoSelectionPage;
    private final AddFilesToRepositoryWizardPage fileSelectionPage;

    public AddFilesToRepositoryWizard(RepositoryPlugin repository, File[] initialFiles) {
        this.repository = repository;
        this.files = initialFiles;

        repoSelectionPage = new LocalRepositorySelectionPage("repoSelectionPage", repository);

        fileSelectionPage = new AddFilesToRepositoryWizardPage("fileSelectionPage");
        fileSelectionPage.setFiles(files);
    }

    @Override
    public void addPages() {
        if (repository == null) {
            addPage(repoSelectionPage);
            repoSelectionPage.addPropertyChangeListener(LocalRepositorySelectionPage.PROP_SELECTED_REPO, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    repository = (RepositoryPlugin) evt.getNewValue();
                }
            });
        }
        addPage(fileSelectionPage);
    }

    @Override
    public boolean performFinish() {
        WorkspaceJob job = new WorkspaceJob("Adding files to repository") {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                return performFinishAddFiles(monitor);
            }
        };
        job.schedule();
        return true;
    }

    private IStatus performFinishAddFiles(IProgressMonitor monitor) {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Failed to install one or more bundles", null);

        List<File> files = fileSelectionPage.getFiles();
        selectedBundles = new LinkedList<Pair<String,String>>();
        monitor.beginTask("Processing files", files.size());
        for (File file : files) {
            monitor.subTask(file.getName());

            Jar jar = null;
            try {
                jar = new Jar(file);
                jar.setDoNotTouchManifest();

                Attributes mainAttribs = jar.getManifest().getMainAttributes();
                String bsn = BundleUtils.getBundleSymbolicName(mainAttribs);
                String version = mainAttribs.getValue(Constants.BUNDLE_VERSION);
                if (version == null)
                    version = "0";
                selectedBundles.add(Pair.newInstance(bsn, version));
            } catch (Exception e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to analyse JAR: {0}", file.getPath()), e));
                continue;
            } finally {
                if (jar != null)
                    jar.close();
            }

            try {
                RepositoryPlugin.PutResult result = repository.put(new BufferedInputStream(new FileInputStream(file)), new RepositoryPlugin.PutOptions());
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
            monitor.worked(1);
        }
        return status;
    }

    public List<Pair<String,String>> getSelectedBundles() {
        return Collections.unmodifiableList(selectedBundles);
    }
}
