package bndtools.editor.project;

import java.io.File;
import java.util.List;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.osgi.Constants;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.VersionedClause;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class RunBundlesPart extends RepositoryBundleSelectionPart {
	public RunBundlesPart(Composite parent, FormToolkit toolkit, int style) {
		super(Constants.RUNBUNDLES, parent, toolkit, style);
	}
	@Override
	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Run Bundles");
		section.setDescription("The selected bundles will be added to the runtime framework.");
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
		model.setRunBundles(bundles);
	}
	@Override
	protected List<VersionedClause> loadFromModel(BndEditModel model) {
		return model.getRunBundles();
	}
	@Override
	protected RepoBundleSelectionWizard createBundleSelectionWizard(List<VersionedClause> bundles) {
        // Need to get the project from the input model...
        Project project = null;
        try {
            BndEditModel model = (BndEditModel) getManagedForm().getInput();
            File projectDir = model.getBndResource().getProject().getLocation().toFile();
            project = Workspace.getProject(projectDir);
        } catch (Exception e) {
            Plugin.logError("Error getting project from editor model", e);
        }

	    RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(project, bundles, true);

        wizard.setSelectionPageTitle("Project Run Path");
        wizard.setSelectionPageDescription("Select bundles to be added to the project build path for compilation.");

	    return wizard;
	}
}
