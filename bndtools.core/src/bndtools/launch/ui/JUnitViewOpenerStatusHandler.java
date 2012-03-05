package bndtools.launch.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import bndtools.Plugin;
import bndtools.perspective.BndPerspective;

public class JUnitViewOpenerStatusHandler implements IStatusHandler {

    public Object handleStatus(IStatus status, Object source) throws CoreException {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    window.getActivePage().showView(BndPerspective.VIEW_ID_JUNIT_RESULTS, null, IWorkbenchPage.VIEW_VISIBLE);
                } catch (PartInitException e) {
                    Plugin.logError("Error showing JUnit Results view", e);
                }
            }
        };
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (display.getThread() == Thread.currentThread())
            runnable.run();
        else
            display.syncExec(runnable);

        return null;
    }

}
