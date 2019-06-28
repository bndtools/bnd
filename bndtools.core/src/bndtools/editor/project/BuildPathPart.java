package bndtools.editor.project;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.Constants;
import bndtools.model.repo.DependencyPhase;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class BuildPathPart extends RepositoryBundleSelectionPart {

	public BuildPathPart(Composite parent, FormToolkit toolkit, int style) {
		super(Constants.BUILDPATH, DependencyPhase.Build, parent, toolkit, style);
	}

	@Override
	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Build Path");
		section.setDescription("The selected bundles will be added to the project build path for compilation.");
		super.createSection(section, toolkit);
	}

	@Override
	protected int getTableHeightHint() {
		return 50;
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
	protected void setSelectionWizardTitleAndMessage(RepoBundleSelectionWizard wizard) {
		wizard.setSelectionPageTitle("Project Build Path");
		wizard.setSelectionPageDescription("Select bundles to be added to the project build path for compilation.");
	}

}
