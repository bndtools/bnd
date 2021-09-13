package org.bndtools.remoteinstall.command;

import static org.bndtools.remoteinstall.helper.MessageDialogHelper.showMessage;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleHandler_Dialog_MessageExecutionException;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleHandler_Dialog_TitleExecutionException;
import static org.eclipse.jface.window.Window.CANCEL;

import java.io.File;

import org.bndtools.remoteinstall.dto.RemoteRuntimeConfiguration;
import org.bndtools.remoteinstall.wizard.InstallBundleWizard;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = IHandler2.class)
public final class InstallHandler extends AbstractHandler {

    @Reference
    private InstallCommand command;

    @Reference
    private InstallBundleWizard wizard;

    @Override
    public Object execute(final ExecutionEvent execEvent) throws ExecutionException {
        final Shell        shell        = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final WizardDialog dialog       = new WizardDialog(shell, wizard);
        final int          dialogResult = dialog.open();
        if (dialogResult == CANCEL) {
            return false;
        }
        final File                       jarFile = getJarFile();
        final RemoteRuntimeConfiguration config  = wizard.getConfiguration();
        if (jarFile == null) {
            showMessage(InstallBundleHandler_Dialog_MessageExecutionException,
                    InstallBundleHandler_Dialog_TitleExecutionException);
            return false;
        }
        command.execute(config.host, config.port, jarFile, config.timeout);
        return null;
    }

    private File getJarFile() {
        final ISelectionService service   = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
        final ISelection        selection = service.getSelection();

        if (selection instanceof IStructuredSelection) {
            final Object selected = ((IStructuredSelection) selection).getFirstElement();
            return Platform.getAdapterManager().getAdapter(selected, IFile.class).getLocation().toFile();
        }
        return null;
    }

}
