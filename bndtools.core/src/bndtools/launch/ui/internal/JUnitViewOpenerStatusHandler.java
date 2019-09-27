package bndtools.launch.ui.internal;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import bndtools.perspective.BndPerspective;

public class JUnitViewOpenerStatusHandler implements IStatusHandler {
	private static final ILogger logger = Logger.getLogger(JUnitViewOpenerStatusHandler.class);

	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		Runnable runnable = () -> {
			try {
				IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
				window.getActivePage()
					.showView(BndPerspective.VIEW_ID_JUNIT_RESULTS, null, IWorkbenchPage.VIEW_VISIBLE);
			} catch (PartInitException e) {
				logger.logError("Error showing JUnit Results view", e);
			}
		};
		Display display = PlatformUI.getWorkbench()
			.getDisplay();
		if (display.getThread() == Thread.currentThread())
			runnable.run();
		else
			display.syncExec(runnable);

		return null;
	}

}
