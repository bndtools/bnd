package bndtools.utils;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
}
