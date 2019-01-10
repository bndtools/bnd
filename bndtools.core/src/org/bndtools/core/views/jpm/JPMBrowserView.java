package org.bndtools.core.views.jpm;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.model.repo.ContinueSearchElement;
import bndtools.preferences.JpmPreferences;

public class JPMBrowserView extends ViewPart implements ISelectionListener {

    private final ImageDescriptor backImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/back.png");
    private final ImageDescriptor forwardImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/forward.png");

    private boolean external = false;
    private URI externalUri = null;

    private Browser browser;
    private Action backAction;
    private Action forwardAction;
    private ISelectionService selectionService;

    @Override
    public void createPartControl(Composite parent) {
        StackLayout stack = new StackLayout();
        parent.setLayout(stack);

        JpmPreferences prefs = new JpmPreferences();
        if (prefs.getBrowserSelection() == JpmPreferences.PREF_BROWSER_EXTERNAL) {
            external = true;
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));
            new Label(composite, SWT.NONE).setText("Searchable Repository is configured to open in an external browser.");
            Hyperlink linkToPrefs = new Hyperlink(composite, SWT.NONE);
            linkToPrefs.setText("Open Preference Page");
            linkToPrefs.setUnderlined(true);
            linkToPrefs.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(HyperlinkEvent e) {
                    PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getViewSite().getShell(), "bndtools.prefPages.jpm", new String[] {
                        "bndtools.prefPages.jpm"
                    }, null);
                    dialog.open();
                }
            });
            // linkToPrefs.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
            stack.topControl = composite;
        } else {
            if (prefs.getBrowserSelection() == JpmPreferences.PREF_BROWSER_PLATFORM_DEFAULT) {
                browser = new Browser(parent, SWT.NONE);
                stack.topControl = browser;
            } else if (prefs.getBrowserSelection() == JpmPreferences.PREF_BROWSER_WEBKIT) {
                browser = new Browser(parent, SWT.WEBKIT);
                stack.topControl = browser;
            } else if (prefs.getBrowserSelection() == JpmPreferences.PREF_BROWSER_MOZILLA) {
                browser = new Browser(parent, SWT.MOZILLA);
                stack.topControl = browser;
            }

            createActions();

            // Prevent navigation away from JPM4J.org, and redirect from HTTP back to HTTPS
            browser.addLocationListener(new LocationAdapter() {
                @Override
                public void changing(LocationEvent event) {
                    setContentDescription(event.location);
                    /*
                     * if (event.location.startsWith(HTTPS_URL)) return; if (event.location.startsWith(HTTP_URL))
                     * event.location = event.location.replaceFirst(HTTP_URL, HTTP_URL); else event.doit = false;
                     */
                }
            });
        }
        selectionService = getViewSite().getWorkbenchWindow()
            .getSelectionService();
        selectionService.addSelectionListener(this);
        handleWorkbenchSelection(selectionService.getSelection());
    }

    @Override
    public void dispose() {
        if (selectionService != null)
            selectionService.removeSelectionListener(this);
    }

    private void createActions() {
        backAction = new Action("Back", backImg) {
            @Override
            public void run() {
                browser.back();
            }
        };

        forwardAction = new Action("Forward", forwardImg) {
            @Override
            public void run() {
                browser.forward();
            }
        };

        IToolBarManager tbm = getViewSite().getActionBars()
            .getToolBarManager();
        tbm.add(backAction);
        tbm.add(forwardAction);
    }

    @Override
    public void setFocus() {}

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        handleWorkbenchSelection(selection);
    }

    private void handleWorkbenchSelection(ISelection selection) {
        if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
            @SuppressWarnings("rawtypes")
            Iterator iter = ((IStructuredSelection) selection).iterator();
            while (iter.hasNext()) {
                Object obj = iter.next();
                if (obj instanceof ContinueSearchElement) {
                    ContinueSearchElement cont = (ContinueSearchElement) obj;

                    try {
                        setSearchURI(cont.browse());
                    } catch (Exception e) {
                        Plugin.getDefault()
                            .error("Failed to get browse URL for repository", e);
                    }
                    break;
                }
            }
        }
    }

    public void setSearchURI(URI uri) {
        try {
            if (external) {
                if (!uri.equals(externalUri)) {
                    externalUri = uri;
                    getViewSite().getWorkbenchWindow()
                        .getWorkbench()
                        .getBrowserSupport()
                        .getExternalBrowser()
                        .openURL(uri.toURL());
                }
            } else {
                String current = browser.getUrl();
                String urlStr = uri.toString();
                if (!urlStr.equals(current)) {
                    browser.setUrl(urlStr);
                    setContentDescription(urlStr);
                }
            }
        } catch (PartInitException e) {
            ErrorDialog.openError(getViewSite().getShell(), "Error", "Error", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unable to open URL in browser", e));
        } catch (MalformedURLException e) {
            ErrorDialog.openError(getViewSite().getShell(), "Error", "Error", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Invalid URL", e));
        }
    }

}
