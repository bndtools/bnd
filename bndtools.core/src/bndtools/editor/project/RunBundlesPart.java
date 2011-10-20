package bndtools.editor.project;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.build.Project;
import aQute.lib.osgi.Constants;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.VersionedClause;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class RunBundlesPart extends RepositoryBundleSelectionPart {

    private Image wizardImg;

	public RunBundlesPart(Composite parent, FormToolkit toolkit, int style) {
		super(Constants.RUNBUNDLES, parent, toolkit, style);
	}
	@Override
	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Run Bundles");
		section.setDescription("The selected bundles will be added to the runtime framework. NB: This project's own bundles are automatically included.");
		super.createSection(section, toolkit);
	}
	@Override
	protected void fillToolBar(ToolBar toolbar) {
	    createAddItemTool(toolbar);
	    this.removeItemTool = createRemoveItemTool(toolbar);
	}
    @Override
	protected GridData getTableLayoutData() {
		GridData gd = super.getTableLayoutData();
		gd.heightHint = 75;
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
    protected RepoBundleSelectionWizard createBundleSelectionWizard(Project project, List<VersionedClause> bundles) throws Exception {
        // Need to get the project from the input model...
        RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(project, bundles);
        setSelectionWizardTitleAndMessage(wizard);

        return wizard;
    }

    void setSelectionWizardTitleAndMessage(RepoBundleSelectionWizard wizard) {
        wizard.setSelectionPageTitle("Project Run Path");
        wizard.setSelectionPageDescription("Select bundles to be added to the project build path for compilation.");
    }

    @Override
    public void dispose() {
        if (wizardImg != null)
            wizardImg.dispose();
        super.dispose();
    }
}
