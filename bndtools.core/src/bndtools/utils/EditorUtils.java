/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.utils;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;

public class EditorUtils {
    public static boolean saveEditorIfDirty(final IEditorPart editor, String dialogTitle, String message) {
        if (editor.isDirty()) {
            if (MessageDialog.openConfirm(editor.getEditorSite().getShell(), dialogTitle, message)) {
                IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        editor.doSave(monitor);
                    }
                };
                IWorkbenchWindow window = editor.getSite().getWorkbenchWindow();
                try {
                    window.run(false, false, saveRunnable);
                } catch (InvocationTargetException e1) {} catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return !editor.isDirty();
    }

    public static final IFormPart findPartByClass(IManagedForm form, Class< ? extends IFormPart> clazz) {
        IFormPart[] parts = form.getParts();
        for (IFormPart part : parts) {
            if (clazz.isInstance(part))
                return part;
        }
        return null;
    }
}
