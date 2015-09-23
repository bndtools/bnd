package org.bndtools.core.ui.wizards.shared;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.templating.Template;
import org.bndtools.templating.load.WorkspaceTemplateLoader;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

import aQute.lib.io.IO;
import bndtools.Plugin;
import bndtools.central.Central;

public class RepoTemplateSelectionWizardPage extends WizardPage {

    public static final String PROP_TEMPLATE = "template";

    private final static ILog log = Plugin.getDefault().getLog();

    protected final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);
    private final String templateType;
    private final IWorkbench workbench;

    private final Map<Template,Image> loadedImages = new IdentityHashMap<>();

    private Tree tree;
    private TreeViewer viewer;
    private RepoTemplateContentProvider contentProvider;
    private ScrolledFormText txtDescription;

    private Template selected = null;

    private boolean shown = false;

    public RepoTemplateSelectionWizardPage(String pageName, String templateType, IWorkbench workbench) {
        super(pageName);
        this.templateType = templateType;
        this.workbench = workbench;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && !shown) {
            shown = true;
            loadTemplates();
        }
    }

    /*
     * Don't allow wizard to complete before this page is shown
     */
    @Override
    public boolean isPageComplete() {
        return shown && selected != null && super.isPageComplete();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public void createControl(Composite parent) {
        GridData gd;

        Composite composite = new Composite(parent, SWT.NULL);
        setControl(composite);

        composite.setLayout(new GridLayout(1, false));

        new Label(composite, SWT.NONE).setText("Select Template:");

        tree = new Tree(composite, SWT.BORDER | SWT.FULL_SELECTION);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 100;
        tree.setLayoutData(gd);

        viewer = new TreeViewer(tree);
        contentProvider = new RepoTemplateContentProvider(false);
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(new RepoTemplateLabelProvider(loadedImages));

        new Label(composite, SWT.NONE).setText("Description:");

        Composite cmpDescription = new Composite(composite, SWT.BORDER);
        cmpDescription.setBackground(tree.getBackground());

        txtDescription = new ScrolledFormText(cmpDescription, SWT.V_SCROLL | SWT.H_SCROLL, false);
        FormText formText = new FormText(txtDescription, SWT.NO_FOCUS);
        txtDescription.setFormText(formText);
        txtDescription.setBackground(tree.getBackground());
        formText.setBackground(tree.getBackground());
        formText.setForeground(tree.getForeground());
        formText.setFont("fixed", JFaceResources.getTextFont());
        formText.setFont("italic", JFaceResources.getFontRegistry().getItalic(""));

        GridData gd_cmpDescription = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd_cmpDescription.heightHint = 100;
        cmpDescription.setLayoutData(gd_cmpDescription);

        GridLayout layout_cmpDescription = new GridLayout(1, false);
        cmpDescription.setLayout(layout_cmpDescription);

        GridData gd_txtDescription = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        gd_txtDescription.heightHint = 100;
        txtDescription.setLayoutData(gd_txtDescription);

        Hyperlink linkRetina = new Hyperlink(composite, SWT.NONE);
        linkRetina.setText("Why is this text blurred?");
        linkRetina.setUnderlined(true);
        linkRetina.setForeground(JFaceColors.getHyperlinkText(getShell().getDisplay()));
        linkRetina.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                Object element = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                setTemplate(element instanceof Template ? (Template) element : null);
                getContainer().updateButtons();
            }
        });
        viewer.addOpenListener(new IOpenListener() {
            @Override
            public void open(OpenEvent event) {
                Object element = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                setTemplate(element instanceof Template ? (Template) element : null);
                getContainer().updateButtons();
                IWizardPage nextPage = getNextPage();
                if (nextPage != null)
                    getContainer().showPage(nextPage);
            }
        });
        linkRetina.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent ev) {
                try {
                    IWorkbenchBrowserSupport browser = workbench.getBrowserSupport();
                    browser.getExternalBrowser().openURL(new URL("https://github.com/bndtools/bndtools/wiki/Blurry-Form-Text-on-High-Resolution-Displays"));
                } catch (Exception e) {
                    log.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Browser open error", e));
                }
            }
        });
    }

    protected void loadTemplates() {
        final Display display = getContainer().getShell().getDisplay();
        IRunnableWithProgress searchRunnable = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    WorkspaceTemplateLoader templateLoader = new WorkspaceTemplateLoader(Central.getWorkspace());
                    final List<Template> templates = templateLoader.findTemplates(templateType);
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            viewer.setInput(templates);
                            viewer.expandAll();

                            Template first = contentProvider.getFirstTemplate();
                            viewer.setSelection(first != null ? new StructuredSelection(first) : StructuredSelection.EMPTY, true);

                            IconLoaderJob iconLoaderJob = new IconLoaderJob(templates, viewer, loadedImages, 5);
                            iconLoaderJob.setSystem(true);
                            iconLoaderJob.schedule(0);
                        }
                    });
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            }
        };
        try {
            getContainer().run(true, true, searchRunnable);
        } catch (InterruptedException e) {
            // runnable cancelled, clear tree
            viewer.setInput(new Object[0]);
        } catch (InvocationTargetException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to load templates", e));
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        for (Entry<Template,Image> entry : loadedImages.entrySet()) {
            Image img = entry.getValue();
            if (!img.isDisposed())
                img.dispose();
        }
    }

    public void setTemplate(final Template template) {
        Template old = this.selected;
        this.selected = template;
        propSupport.firePropertyChange(PROP_TEMPLATE, old, template);

        Job updateDescJob = new UpdateDescriptionJob(template, txtDescription);
        updateDescJob.setSystem(true);
        updateDescJob.schedule();
    }

    public Template getTemplate() {
        return selected;
    }

    private static final class UpdateDescriptionJob extends Job {
        private final Template template;
        private final ScrolledFormText control;

        private UpdateDescriptionJob(Template template, ScrolledFormText control) {
            super("update description");
            this.template = template;
            this.control = control;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            String tmp = "<form>No description available</form>";
            if (template != null) {
                URI uri = template.getDescriptionText();
                if (uri != null) {
                    try {
                        URLConnection conn = uri.toURL().openConnection();
                        conn.setUseCaches(false);
                        tmp = IO.collect(conn.getInputStream());
                    } catch (IOException e) {
                        log.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading template description text.", e));
                    }
                }
            }

            final String text = tmp;
            if (control != null && !control.isDisposed()) {
                control.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!control.isDisposed())
                            control.setText(text);
                    }
                });
            }

            return Status.OK_STATUS;
        }
    }

}
