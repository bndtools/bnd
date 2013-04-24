package org.bndtools.core.views.jpm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.model.repo.ContinueSearchElement;

public class JPMBrowserView extends ViewPart implements ISelectionListener {

    private static final String HTTPS_URL = "https://www.jpm4j.org/";
    private static final String HTTP_URL = "http://www.jpm4j.org/";
    private static final String SEARCH_PREFIX = "https://www.jpm4j.org/#!/search?q=";

    private final ImageDescriptor backImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/back.png");
    private final ImageDescriptor forwardImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/forward.png");

    private final Pattern stripWildcardsPattern = Pattern.compile("[\\*\\s]*([^\\*]*)");

    private Browser browser;
    private Action backAction;
    private Action forwardAction;
    private ISelectionService selectionService;

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

        selectionService = getViewSite().getWorkbenchWindow().getSelectionService();
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

        IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
        tbm.add(backAction);
        tbm.add(forwardAction);
    }

    @Override
    public void setFocus() {}

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
                    setSearchFilter(cont.getFilter());
                    break;
                }
            }
        }
    }

    public void setSearchFilter(String filter) {
        Matcher matcher = stripWildcardsPattern.matcher(filter);
        boolean found = matcher.find();
        if (found)
            filter = matcher.group(1);

        try {
            String url = SEARCH_PREFIX + URLEncoder.encode(filter, "UTF-8");
            String current = browser.getUrl();
            if (!url.equals(current))
                browser.setUrl(url);
        } catch (UnsupportedEncodingException e) {
            // stupid Java
        }
    }

}
