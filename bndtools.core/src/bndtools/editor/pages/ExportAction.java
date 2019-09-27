package bndtools.editor.pages;

import org.bndtools.api.RunMode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.bnd.build.Run;
import aQute.bnd.build.model.BndEditModel;
import bndtools.Plugin;
import bndtools.launch.util.LaunchUtils;
import bndtools.wizards.bndfile.RunExportSelectionWizard;

public class ExportAction extends Action {

	private final Shell						parentShell;
	private final IEditorPart				editor;
	private final BndEditModel				model;

	private final IConfigurationElement[]	configElems;

	public ExportAction(Shell parentShell, IEditorPart editor, BndEditModel model) {
		super("Export", SWT.RIGHT);
		this.parentShell = parentShell;
		this.editor = editor;
		this.model = model;

		configElems = Platform.getExtensionRegistry()
			.getConfigurationElementsFor(Plugin.PLUGIN_ID, "runExportWizards");
	}

	boolean shouldEnable() {
		return configElems != null && configElems.length > 0;
	}

	@Override
	public void run() {
		if (configElems == null || configElems.length == 0)
			return;

		if (editor.isDirty()) {
			if (MessageDialog.openConfirm(parentShell, "Export",
				"The editor content must be saved before exporting. Save now?")) {
				try {
					editor.getSite()
						.getWorkbenchWindow()
						.run(false, false, monitor -> editor.doSave(monitor));
				} catch (Exception e) {
					ErrorDialog.openError(parentShell, "Error", null,
						new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error during save.", e));
					return;
				}
			} else {
				return;
			}
		}

		IFile targetResource = ResourceUtil.getFile(editor.getEditorInput());
		try {
			Run run = LaunchUtils.createRun(targetResource, RunMode.EXPORT);

			RunExportSelectionWizard wizard = new RunExportSelectionWizard(configElems, model, run);
			WizardDialog dialog = new WizardDialog(parentShell, wizard);
			dialog.open();

			LaunchUtils.endRun(run);

		} catch (Exception e) {
			ErrorDialog.openError(parentShell, "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error deriving Bnd project.", e));
		}

	}
}
