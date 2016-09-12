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

import org.bndtools.templating.Category;
import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.bndtools.utils.jface.ProgressRunner;
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
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.promise.Promise;

import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.libg.tuple.Pair;
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
    private Image defaultTemplateImage;

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
        setImageDescriptor(Plugin.imageDescriptorFromPlugin("icons/bndtools-wizban.png")); //$NON-NLS-1$

        GridData gd;

        Composite composite = new Composite(parent, SWT.NULL);
        setControl(composite);

        composite.setLayout(new GridLayout(1, false));

        new Label(composite, SWT.NONE).setText("Select Template:");

        tree = new Tree(composite, SWT.BORDER | SWT.FULL_SELECTION);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 100;
        tree.setLayoutData(gd);

        defaultTemplateImage = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/template.gif").createImage(parent.getDisplay());

        viewer = new TreeViewer(tree);
        contentProvider = new RepoTemplateContentProvider(false);
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(new RepoTemplateLabelProvider(loadedImages, defaultTemplateImage));
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
                if (nextPage != null && selected != null)
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
        txtDescription.getFormText().addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent ev) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL((String) ev.getHref()));
                } catch (Exception ex) {
                    log.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Browser open error", ex));
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
            SubMonitor monitor = SubMonitor.convert(progress);
            try {
                final Set<Template> templates = new LinkedHashSet<>();

                // Fire all the template loaders and get their promises
                List<ServiceReference<TemplateLoader>> templateLoaderSvcRefs = new ArrayList<>(context.getServiceReferences(TemplateLoader.class, null));
                monitor.beginTask("Loading templates...", templateLoaderSvcRefs.size());
                Collections.sort(templateLoaderSvcRefs);
                List<Pair<String,Promise< ? extends Collection<Template>>>> promises = new LinkedList<>();
                for (ServiceReference<TemplateLoader> templateLoaderSvcRef : templateLoaderSvcRefs) {
                    String label = (String) templateLoaderSvcRef.getProperty(Constants.SERVICE_DESCRIPTION);
                    if (label == null)
                        label = (String) templateLoaderSvcRef.getProperty(ComponentConstants.COMPONENT_NAME);
                    if (label == null)
                        label = String.format("Template Loader service ID " + templateLoaderSvcRef.getProperty(Constants.SERVICE_ID));

                    TemplateLoader templateLoader = context.getService(templateLoaderSvcRef);
                    try {
                        Promise< ? extends Collection<Template>> promise = templateLoader.findTemplates(templateType, new Processor());
                        promises.add(new Pair<String,Promise< ? extends Collection<Template>>>(label, promise));
                    } finally {
                        context.ungetService(templateLoaderSvcRef);
                    }
                }

                // Force the promises in sequence
                for (Pair<String,Promise< ? extends Collection<Template>>> namedPromise : promises) {
                    String name = namedPromise.getFirst();
                    SubMonitor childMonitor = monitor.newChild(1, SubMonitor.SUPPRESS_NONE);
                    childMonitor.beginTask(name, 1);
                    try {
                        Throwable failure = namedPromise.getSecond().getFailure();
                        if (failure != null)
                            Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to load from template loader: " + name, failure));
                        else {
                            Collection<Template> loadedTemplates = namedPromise.getSecond().getValue();
                            templates.addAll(loadedTemplates);
                        }
                    } catch (InterruptedException e) {
                        Plugin.getDefault().getLog().log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, "Interrupted while loading from template loader: " + name, e));
                    }
                }

                // Add empty template if provided
                if (emptyTemplate != null)
                    templates.add(emptyTemplate);

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
                // Restore the original message to the page
                if (!shell.isDisposed())
                    shell.getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            setMessage(originalMessage);
                        }
                    });
                if (progress != null)
                    progress.done();
            }
        }

    }

    protected void loadTemplates() {
        final String oldMessage = getMessage();
        final Shell shell = getShell();

        setMessage("Loading templates...");
        try {
            ProgressRunner.execute(true, new LoadTemplatesJob(shell, oldMessage), getContainer(), getContainer().getShell().getDisplay());
        } catch (InvocationTargetException e) {
            Throwable exception = e.getTargetException();
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading templates.", exception));
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        if (!defaultTemplateImage.isDisposed())
            defaultTemplateImage.dispose();

        for (Entry<Template,Image> entry : loadedImages.entrySet()) {
            Image img = entry.getValue();
            if (!img.isDisposed())
                img.dispose();
        }

        if (selected != null) {
            try {
                selected.close();
            } catch (IOException e) {
                Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Problem cleaning up template content", e));
            }
        }
    }

    private void setTemplates(final Collection<Template> templates) {
        viewer.setInput(templates);
        viewer.expandAll();

        Template templateToSelect = null;

        if (viewer.getFilters().length == 0) {
            templateToSelect = contentProvider.getFirstTemplate();
        } else {
            for (Object element : contentProvider.getElements(null)) {
                if (element instanceof Category) {
                    Object[] filteredTemplates = latestFilter.filter(viewer, element, contentProvider.getChildren(element));
                    if (filteredTemplates.length > 0) {
                        templateToSelect = (Template) filteredTemplates[0];
                        break;
                    }
                } else {
                    templateToSelect = (Template) element;
                    break;
                }
            }
        }

        if (templateToSelect == null) {
            return;
        }

        viewer.setSelection(new StructuredSelection(templateToSelect));
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
