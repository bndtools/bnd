package bndtools.wizards.project;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.core.ui.wizards.shared.TemplateSelectionWizardPage;
import org.bndtools.templating.Template;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.wizards.workspace.WorkspaceSetupWizard;

class ProjectTemplateSelectionWizardPage extends TemplateSelectionWizardPage {

	private Image warningImg = null;

	ProjectTemplateSelectionWizardPage(String pageName, String templateType, Template emptyTemplate) {
		super(pageName, templateType, emptyTemplate);
	}

	@Override
	protected Control createHeaderControl(Composite parent) {
		Composite composite;

		Workspace workspace = Central.getWorkspaceIfPresent();
		if (workspace == null || workspace.isDefaultWorkspace()) {
			composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.marginBottom = 15;
			layout.verticalSpacing = 15;
			composite.setLayout(layout);

			Label lblWarning = new Label(composite, SWT.NONE);
			warningImg = Icons.desc("warning.big")
				.createImage(parent.getDisplay());
			lblWarning.setImage(warningImg);

			Link link = new Link(composite, SWT.NONE);
			link.setText(
				"WARNING! The bnd workspace has not been configured. <a href=\"#workspace\">Create a workspace first</a> \n or configure a <a href=\"#prefs\">Template Repository</a> in Bndtools Preferences.");
			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent ev) {
					WizardDialog dialog = (WizardDialog) getContainer();
					dialog.close();

					if ("#workspace".equals(ev.text)) {
						// We are going to open the New Workspace wizard in a
						// new dialog.
						// If that wizard completes successfully then we can
						// reopen the New Project wizard.

						WorkspaceSetupWizard workspaceWizard = new WorkspaceSetupWizard();
						workspaceWizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
						dialog = new WizardDialog(dialog.getShell(), workspaceWizard);

						if (Window.OK == dialog.open()) {
							try {
								NewBndProjectWizard projectWizard = new NewBndProjectWizardFactory().create();
								projectWizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
								new WizardDialog(dialog.getShell(), projectWizard).open();
							} catch (CoreException e) {
								Plugin.getDefault()
									.getLog()
									.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
										"Unable to open New Bnd Project wizard", e));
							}
						}
					} else if ("#prefs".equals(ev.text)) {
						// Open the preference dialog with the template
						// repositories page open.
						// We can't reopen the New Project wizard after because
						// we don't know that the user changed
						// anything.
						PreferenceDialog prefsDialog = PreferencesUtil.createPreferenceDialogOn(getShell(),
							"bndtools.prefPages.repos", null, null);
						prefsDialog.open();
					}
				}
			});
			GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			link.setLayoutData(gd);

			// ControlDecoration decor = new ControlDecoration(link, SWT.LEFT,
			// composite);
			// decor.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());

			Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		} else {
			// There is a workspace, just return null (no controls will be
			// inserted into the panel).
			composite = null;
		}
		return composite;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (warningImg != null && !warningImg.isDisposed())
			warningImg.dispose();
	}

}
