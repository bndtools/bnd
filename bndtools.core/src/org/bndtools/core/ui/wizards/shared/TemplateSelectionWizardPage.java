package org.bndtools.core.ui.wizards.shared;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.bndtools.utils.progress.ProgressMonitorReporter;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledFormText;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import aQute.lib.io.IO;
import bndtools.Plugin;

public class TemplateSelectionWizardPage extends WizardPage {

    public static final String PROP_TEMPLATE = "template";

    private static final String NO_HELP_CONTENT = "<form>No help content available</form>";

    private final ILog log = Plugin.getDefault().getLog();

    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);
    private final String templateType;
    private final Template emptyTemplate;

    private final Map<Template,Image> loadedImages = new IdentityHashMap<>();

    private Tree tree;
    private TreeViewer viewer;
    private RepoTemplateContentProvider contentProvider;
    private final LatestTemplateFilter latestFilter = new LatestTemplateFilter();
    private Button btnLatestOnly;
    private ScrolledFormText txtDescription;

    private Template selected = null;

    private boolean shown = false;

    public TemplateSelectionWizardPage(String pageName, String templateType, Template emptyTemplate) {
        super(pageName);
        this.templateType = templateType;
        this.emptyTemplate = emptyTemplate;
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
        viewer.addFilter(latestFilter);
        setTemplates(emptyTemplate != null ? Collections.singletonList(emptyTemplate) : Collections.<Template> emptyList());

        btnLatestOnly = new Button(composite, SWT.CHECK);
        btnLatestOnly.setText("Show latest versions only");
        btnLatestOnly.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        btnLatestOnly.setSelection(true);

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

        GridData gd_cmpDescription = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd_cmpDescription.heightHint = 100;
        cmpDescription.setLayoutData(gd_cmpDescription);

        GridLayout layout_cmpDescription = new GridLayout(1, false);
        cmpDescription.setLayout(layout_cmpDescription);

        GridData gd_txtDescription = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
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
        btnLatestOnly.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean latestOnly = btnLatestOnly.getSelection();
                if (latestOnly)
                    viewer.addFilter(latestFilter);
                else
                    viewer.removeFilter(latestFilter);
            }
        });
        linkRetina.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent ev) {
                try {
                    IWorkbenchBrowserSupport browser = PlatformUI.getWorkbench().getBrowserSupport();
                    browser.getExternalBrowser().openURL(new URL("https://github.com/bndtools/bndtools/wiki/Blurry-Form-Text-on-High-Resolution-Displays"));
                } catch (Exception e) {
                    log.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Browser open error", e));
                }
            }
        });
    }

    private class LoadTemplatesJob implements IRunnableWithProgress {

        private final Shell shell;
        private final String originalMessage;

        private final BundleContext context = FrameworkUtil.getBundle(LoadTemplatesJob.class).getBundleContext();

        public LoadTemplatesJob(Shell shell, String originalMessage) {
            this.shell = shell;
            this.originalMessage = originalMessage;
        }

        @Override
        public void run(IProgressMonitor progress) throws InvocationTargetException {
            SubMonitor monitor = SubMonitor.convert(progress, 3);
            monitor.setTaskName("Loading Templates...");
            try {
                final Set<Template> templates = new LinkedHashSet<>();
                final List<String> errors = new LinkedList<>();

                List<ServiceReference<TemplateLoader>> templateLoaderSvcRefs = new ArrayList<>(context.getServiceReferences(TemplateLoader.class, null));
                Collections.sort(templateLoaderSvcRefs);

                for (ServiceReference<TemplateLoader> templateLoaderSvcRef : templateLoaderSvcRefs) {
                    TemplateLoader templateLoader = context.getService(templateLoaderSvcRef);
                    try {
                        ProgressMonitorReporter reporter = new ProgressMonitorReporter(monitor.newChild(1, SubMonitor.SUPPRESS_NONE), "Finding templates");
                        templates.addAll(templateLoader.findTemplates(templateType, reporter));
                        errors.addAll(reporter.getErrors());
                    } finally {
                        context.ungetService(templateLoaderSvcRef);
                    }
                }

                // Add empty template if provided
                if (emptyTemplate != null)
                    templates.add(emptyTemplate);

                // Log errors
                for (String error : errors)
                    Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, error, null));

                // Display results
                Control control = viewer.getControl();
                if (control != null && !control.isDisposed()) {
                    control.getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            setTemplates(templates);
                            IconLoaderJob iconLoaderJob = new IconLoaderJob(templates, viewer, loadedImages, 5);
                            iconLoaderJob.setSystem(true);
                            iconLoaderJob.schedule(0);
                        }
                    });
                }

            } catch (InvalidSyntaxException ex) {
                throw new InvocationTargetException(ex);
            } finally {
                monitor.done();
                // Restore the original message to the page
                if (!shell.isDisposed())
                    shell.getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            setMessage(originalMessage);
                        }
                    });
            }
        }

    }

    protected void loadTemplates() {
        final String oldMessage = getMessage();
        final Shell shell = getShell();

        setMessage("Loading templates...");
        try {
            getContainer().run(true, true, new LoadTemplatesJob(shell, oldMessage));
        } catch (InterruptedException e) {
            // ignore
        } catch (InvocationTargetException e) {
            Throwable exception = e.getTargetException();
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading templates.", exception));
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

    private void setTemplates(final Collection<Template> templates) {
        viewer.setInput(templates);
        viewer.expandAll();

        Template first = contentProvider.getFirstTemplate();
        viewer.setSelection(first != null ? new StructuredSelection(first) : StructuredSelection.EMPTY, true);
    }

    public void setTemplate(final Template template) {
        Template old = this.selected;
        this.selected = template;
        propSupport.firePropertyChange(PROP_TEMPLATE, old, template);

        if (template != null) {
            txtDescription.setText(String.format("<form>Loading help content for template '%s'...</form>", template.getName()));
            Job updateDescJob = new UpdateDescriptionJob(template, txtDescription);
            updateDescJob.setSystem(true);
            updateDescJob.schedule();
        } else {
            txtDescription.setText(NO_HELP_CONTENT);
        }
    }

    public Template getTemplate() {
        return selected;
    }

    private final class UpdateDescriptionJob extends Job {
        private final Template template;
        private final ScrolledFormText control;

        private UpdateDescriptionJob(Template template, ScrolledFormText control) {
            super("update description");
            this.template = template;
            this.control = control;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            String tmp = NO_HELP_CONTENT;
            if (template != null) {
                URI uri = template.getHelpContent();
                if (uri != null) {
                    try {
                        URLConnection conn = uri.toURL().openConnection();
                        conn.setUseCaches(false);
                        tmp = IO.collect(conn.getInputStream());
                    } catch (IOException e) {
                        log.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading template help content.", e));
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
