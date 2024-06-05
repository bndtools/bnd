package org.bndtools.elph.importer;

import java.util.stream.Stream;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ImportLibertyProjectsWizard extends Wizard implements IImportWizard {	
	final Config config = new Config();
	
	public ImportLibertyProjectsWizard() {		
		setWindowTitle("Import Liberty Projects");
	}

	@Override
	public void addPages() {
		if (!config.getOlRepoPath().isPresent()) addPage(new LocateRepoPage(config));
		addPage(new ImportPage(config));
	}

	@Override
	public void init(IWorkbench arg0, IStructuredSelection arg1) {}
	
	@Override
	public boolean performFinish() {
		System.out.println("*** PERFORM FINISH ***");
		Stream.of(getPages())
				.reduce((a,b) -> b)          // this gets the last element
				.map(ImportPage.class::cast) // which is the ImportPage
				.get()                       // and must be present!
				.importAllProjects();
		return true;
	}
}
