package bndtools.wizards.workspace;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;
import bndtools.Plugin;
import bndtools.central.RefreshFileJob;
import bndtools.types.Pair;

public class AddFilesToRepositoryWizard extends Wizard {

	private RepositoryPlugin						repository;
	private final File[]							files;
	private List<Pair<String, String>>				selectedBundles;

	private final LocalRepositorySelectionPage		repoSelectionPage;
	private final AddFilesToRepositoryWizardPage	fileSelectionPage;

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
			repoSelectionPage.addPropertyChangeListener(LocalRepositorySelectionPage.PROP_SELECTED_REPO,
				evt -> repository = (RepositoryPlugin) evt.getNewValue());
		}
		addPage(fileSelectionPage);
	}

	@Override
	public boolean performFinish() {
		WorkspaceJob job = new WorkspaceJob("Adding files to repository") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Failed to install one or more bundles",
					null);
				List<File> files = fileSelectionPage.getFiles();
				List<File> refresh = new ArrayList<>();
				selectedBundles = new LinkedList<>();
				SubMonitor progress = SubMonitor.convert(monitor, getName(), files.size());
				for (File file : files) {
					try (Jar jar = new Jar(file)) {
						String bsn = jar.getBsn();
						String version = jar.getVersion();
						selectedBundles.add(Pair.newInstance(bsn, (version != null) ? version : "0"));
					} catch (Exception e) {
						status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
							MessageFormat.format("Failed to analyse JAR: {0}", file.getPath()), e));
						progress.worked(1);
						continue;
					}

					try {
						RepositoryPlugin.PutResult result = repository.put(new BufferedInputStream(IO.stream(file)),
							new RepositoryPlugin.PutOptions());
						URI artifact = result.artifact;
						if ((artifact != null) && artifact.getScheme()
							.equalsIgnoreCase("file")) {
							refresh.add(new File(artifact));
						}
					} catch (Exception e) {
						status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
							MessageFormat.format("Failed to add JAR to repository: {0}", file.getPath()), e));
						progress.worked(1);
						continue;
					}
					progress.worked(1);
				}
				RefreshFileJob refreshJob = new RefreshFileJob(refresh, false);
				if (refreshJob.needsToSchedule())
					refreshJob.schedule();
				progress.done();
				return status;
			}
		};
		job.schedule();
		return true;
	}

	public List<Pair<String, String>> getSelectedBundles() {
		return Collections.unmodifiableList(selectedBundles);
	}
}
