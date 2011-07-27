package bndtools.editor.project;

import java.util.List;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.build.Project;
import aQute.bnd.service.OBRResolutionMode;
import aQute.lib.osgi.Constants;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.VersionedClause;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class BuildPathPart extends RepositoryBundleSelectionPart {
	public BuildPathPart(Composite parent, FormToolkit toolkit, int style) {
		super(Constants.BUILDPATH, parent, toolkit, style);
	}
	@Override
	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Build Path");
		section.setDescription("The selected bundles will be added to the project build path for compilation.");
		super.createSection(section, toolkit);
	}
	@Override
	protected GridData getTableLayoutData() {
		GridData gd = super.getTableLayoutData();
		gd.heightHint = 200;
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
    protected RepoBundleSelectionWizard createBundleSelectionWizard(Project project, List<VersionedClause> bundles) throws Exception {
        RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(project, bundles, false, new OBRResolutionMode[] { OBRResolutionMode.build, OBRResolutionMode.runtime });

        wizard.setSelectionPageTitle("Project Build Path");
        wizard.setSelectionPageDescription("Select bundles to be added to the project build path for compilation.");

        return wizard;
    }
}
