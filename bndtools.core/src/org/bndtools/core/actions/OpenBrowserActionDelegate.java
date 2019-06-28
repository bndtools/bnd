package org.bndtools.core.actions;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class OpenBrowserActionDelegate implements IWorkbenchWindowActionDelegate {
	private static final ILogger	logger	= Logger.getLogger(OpenBrowserActionDelegate.class);

	private IWorkbenchWindow		window;

	@Override
	public void run(IAction action) {
		try {
			IWorkbenchBrowserSupport browserSupport = window.getWorkbench()
				.getBrowserSupport();
			IWebBrowser browser = browserSupport.createBrowser(6, null, null, null);
			browser.openURL(null);
		} catch (Exception e) {
			logger.logError("Error opening browser", e);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {}

	@Override
	public void dispose() {}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}
