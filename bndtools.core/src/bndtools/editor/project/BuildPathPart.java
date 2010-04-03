package bndtools.editor.project;

import java.util.List;


import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.VersionedClause;

import aQute.lib.osgi.Constants;

public class BuildPathPart extends RepositoryBundleSelectionPart {
	protected BuildPathPart(Composite parent, FormToolkit toolkit, int style) {
		super(Constants.BUILDPATH, parent, toolkit, style);
	}
	@Override
	protected void createSection(Section section, FormToolkit toolkit) {
		section.setText("Build Path");
		section.setDescription("The selected bundles will be added to the project build path for compilation.");
		super.createSection(section, toolkit);
	}
	@Override
	protected GridData getTableLayoutData() {
		GridData gd = super.getTableLayoutData();
		gd.heightHint = 175;
		return gd;
	}
	@Override
	protected void saveToModel(BndEditModel model, List<VersionedClause> bundles) {
		model.setBuildPath(bundles);
	}
	@Override
	protected List<VersionedClause> loadFromModel(BndEditModel model) {
		return model.getBuildPath();
	}
	@Override
	protected void customizeWizard(RepoBundleSelectionWizard wizard) {
		WizardPage bundlePage = wizard.getBundleSelectionPage();
		bundlePage.setTitle("Project Build Path");
		bundlePage.setDescription("Select bundles to be added to the project build path for compilation.");
	}
}