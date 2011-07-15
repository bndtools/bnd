package bndtools.editor.project;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import aQute.lib.osgi.Constants;
import bndtools.Plugin;
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
	    createWizardItemtool(toolbar);
	    this.removeItemTool = createRemoveItemTool(toolbar);
	}
	protected ToolItem createWizardItemtool(ToolBar toolbar) {
	    if (wizardImg == null)
	        wizardImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/wand.png").createImage();

        ToolItem tool = new ToolItem(toolbar, SWT.PUSH);

        tool.setImage(wizardImg);
        tool.setToolTipText("Add Bundle");
        tool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddWizard();
            }
        });

        return tool;
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
	protected RepoBundleSelectionWizard createBundleSelectionWizard(Project project, List<VersionedClause> bundles) throws Exception {
        // Need to get the project from the input model...
	    RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(project, bundles, false);
        setSelectionWizardTitleAndMessage(wizard);

	    return wizard;
	}
    void setSelectionWizardTitleAndMessage(RepoBundleSelectionWizard wizard) {
        wizard.setSelectionPageTitle("Project Run Path");
        wizard.setSelectionPageDescription("Select bundles to be added to the project build path for compilation.");
    }
    private void doAddWizard() {
        Project project = getProject();
        try {
            RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(project, getBundles(), true);
            setSelectionWizardTitleAndMessage(wizard);

            WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);
            if (dialog.open() == Window.OK) {
                setBundles(wizard.getSelectedBundles());
            }
        } catch (Exception e) {
            ErrorDialog.openError(getSection().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening bundle resolver wizard.", e));
        }
    }
	@Override
	public void dispose() {
	    if (wizardImg != null)
	        wizardImg.dispose();
	    super.dispose();
	}
}
