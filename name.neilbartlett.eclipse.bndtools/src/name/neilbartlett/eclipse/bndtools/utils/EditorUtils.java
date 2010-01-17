package name.neilbartlett.eclipse.bndtools.utils;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

public class EditorUtils {
	public static boolean saveEditorIfDirty(final IEditorPart editor, String dialogTitle, String message) {
		if(editor.isDirty()) {
			if(MessageDialog.openConfirm(editor.getEditorSite().getShell(), dialogTitle, message)) {
				IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						editor.doSave(monitor);
					}
				};
				IWorkbenchWindow window = editor.getSite().getWorkbenchWindow();
				try {
					window.run(false, false, saveRunnable);
				} catch (InvocationTargetException e1) {
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
		}
		return !editor.isDirty();
	}
}
