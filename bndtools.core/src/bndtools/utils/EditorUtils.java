package bndtools.utils;

import java.lang.reflect.InvocationTargetException;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;

public class EditorUtils {
	public static boolean saveEditorIfDirty(final IEditorPart editor, String dialogTitle, String message) {
		if (editor.isDirty()) {
			if (MessageDialog.openConfirm(editor.getEditorSite()
				.getShell(), dialogTitle, message)) {
				IRunnableWithProgress saveRunnable = monitor -> editor.doSave(monitor);
				IWorkbenchWindow window = editor.getSite()
					.getWorkbenchWindow();
				try {
					window.run(false, false, saveRunnable);
				} catch (InvocationTargetException e1) {} catch (InterruptedException e1) {
					Thread.currentThread()
						.interrupt();
				}
			}
		}
		return !editor.isDirty();
	}

	public static final IFormPart findPartByClass(IManagedForm form, Class<? extends IFormPart> clazz) {
		IFormPart[] parts = form.getParts();
		for (IFormPart part : parts) {
			if (clazz.isInstance(part))
				return part;
		}
		return null;
	}

	/**
	 * Creates a help button with icon and tool-tip.
	 *
	 * @param url
	 * @param tooltipText
	 */
	public static final Action createHelpButton(String url, String tooltipText) {
		Action btn = new Action("Help", IAction.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				Program.launch(url);
			}
		};
		btn.setEnabled(true);
		btn.setToolTipText(tooltipText);
		btn.setImageDescriptor(Icons.desc("help"));

		return btn;
	}

	/**
	 * Creates a help button with help-icon, text and tool-tip.
	 *
	 * @param url
	 * @param buttonText
	 * @param tooltipText
	 */
	public static final ActionContributionItem createHelpButtonWithText(String url, String buttonText, String tooltipText) {
		Action btn = createHelpButton(url, tooltipText);
		btn.setText(buttonText);

		// the ActionContributionItem is required to display text below the icon
		// of the button
		ActionContributionItem helpContrib = new ActionContributionItem(btn);
		helpContrib.setMode(ActionContributionItem.MODE_FORCE_TEXT);

		return helpContrib;
	}
}
