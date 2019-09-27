package org.bndtools.core.ui.wizards.index;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bndtools.core.jobs.GenerateIndexJob;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class NewIndexWizard extends Wizard implements INewWizard {

	private final IndexerWizardPage indexPage = new IndexerWizardPage();

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		Object firstSelElem = selection.getFirstElement();
		if (firstSelElem instanceof IFolder) {
			File dir = ((IFolder) firstSelElem).getLocation()
				.toFile();
			indexPage.setBaseDir(dir);
		}
	}

	@Override
	public void addPages() {
		addPage(indexPage);
	}

	@Override
	public boolean performFinish() {
		File baseDir = indexPage.getBaseDir();
		Path basePath = baseDir.toPath();
		List<Path> paths = indexPage.getInputPaths();
		Set<File> inputFiles = new HashSet<>(paths.size());
		for (Path path : paths)
			inputFiles.add(basePath.resolve(path)
				.toFile());

		// Setup index config
		Boolean compressed = Boolean.valueOf(indexPage.getOutputStyle() == IndexFormatStyle.COMPRESSED);

		// Create the job
		GenerateIndexJob job = new GenerateIndexJob(inputFiles, indexPage.getOutputFile(), baseDir.toURI(), compressed,
			null);
		job.setUser(true);
		job.schedule();

		return true;
	}

}
