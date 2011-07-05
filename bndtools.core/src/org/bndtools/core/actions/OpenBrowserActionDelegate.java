package org.bndtools.core.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import bndtools.Plugin;

public class OpenBrowserActionDelegate implements IWorkbenchWindowActionDelegate {

    private IWorkbenchWindow window;

    public void run(IAction action) {
        try {
            IWorkbenchBrowserSupport browserSupport = window.getWorkbench().getBrowserSupport();
            IWebBrowser browser = browserSupport.createBrowser(6, null, null, null);
            browser.openURL(null);
        } catch (Exception e) {
            Plugin.logError("Error opening browser", e);
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
        this.window = window;
    }

}
