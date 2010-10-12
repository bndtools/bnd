package bndtools.wizards.workspace;

import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class InitialiseCnfStartupParticipant implements Runnable {

    public void run() {
        InitialiseCnfProjectWizard.showIfNeeded(false, true, new IShellProvider() {
            public Shell getShell() {
                return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            }
        });
    }

}
