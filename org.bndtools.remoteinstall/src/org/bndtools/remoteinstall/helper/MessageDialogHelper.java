package org.bndtools.remoteinstall.helper;

import static bndtools.Plugin.PLUGIN_ID;
import static org.eclipse.core.runtime.IStatus.ERROR;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.exceptions.Exceptions;

public final class MessageDialogHelper {

    private MessageDialogHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static void showMessage(final String message, final String title) {
        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MessageDialog.openInformation(shell, title, message);
    }

    public static void showConfirm(final String message, final String title) {
        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MessageDialog.openConfirm(shell, title, message);
    }

    public static void showError(final String message, final String title, final Throwable throwable) {
        final Shell  shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final String trace = Exceptions.toString(throwable);

        final List<Status> children = new ArrayList<>();
        for (final String line : trace.split(System.lineSeparator())) {
            children.add(new Status(ERROR, PLUGIN_ID, line));
        }
        final MultiStatus ms = new MultiStatus(PLUGIN_ID, ERROR, children.toArray(new Status[0]),
                throwable.getLocalizedMessage(), throwable);
        ErrorDialog.openError(shell, title, message, ms);
    }

}
