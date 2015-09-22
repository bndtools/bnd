package org.bndtools.core.ui.wizards.shared;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.bndtools.templating.Template;
import org.bndtools.templating.load.WorkspaceTemplateLoader;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

import bndtools.Plugin;
import bndtools.central.Central;

public class RepoTemplateSelectionWizardPage extends WizardPage {

    public static final String PROP_TEMPLATE = "template";

    protected final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);
    private final String templateType;

    private Tree tree;
    private TreeViewer viewer;
    private RepoTemplateContentProvider contentProvider;
    private ScrolledFormText txtDescription;

    private Template selected = null;

    private boolean shown = false;

    public RepoTemplateSelectionWizardPage(String pageName, String templateType) {
        super(pageName);
        this.templateType = templateType;
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
        viewer.setLabelProvider(new RepoTemplateLabelProvider());

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

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                Object element = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                setTemplate(element instanceof Template ? (Template) element : null);
                getContainer().updateButtons();
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

    public void setTemplate(Template template) {
        Template old = this.selected;
        this.selected = template;
        propSupport.firePropertyChange(PROP_TEMPLATE, old, template);
    }

    public Template getTemplate() {
        return selected;
    }
}
