package org.bndtools.core.views.jpm;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class JPMBrowserView extends ViewPart {

    private static final String HTTPS_URL = "https://www.jpm4j.org/";
    private static final String HTTP_URL = "http://www.jpm4j.org/";

    private final ImageDescriptor backImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/back.png");
    private final ImageDescriptor forwardImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/forward.png");

    private Browser browser;
    private Action backAction;
    private Action forwardAction;

    @Override
    public void createPartControl(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
        createActions();

        // Prevent navigation away from JPM4J.org, and redirect from HTTP back to HTTPS
        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent event) {
                if (event.location.startsWith(HTTPS_URL))
                    return;
                if (event.location.startsWith(HTTP_URL))
                    event.location = event.location.replaceFirst(HTTP_URL, HTTP_URL);
                else
                    event.doit = false;
            }
        });

        browser.setUrl(HTTPS_URL);
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

        IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
        tbm.add(backAction);
        tbm.add(forwardAction);
    }

    @Override
    public void setFocus() {}

}
