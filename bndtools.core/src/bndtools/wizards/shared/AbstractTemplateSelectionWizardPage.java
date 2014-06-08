package bndtools.wizards.shared;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.core.ui.ConfigElementLabelProvider;
import org.bndtools.utils.eclipse.CategorisedConfigurationElementComparator;
import org.bndtools.utils.eclipse.CategorisedPrioritisedConfigurationElementTreeContentProvider;
import org.bndtools.utils.osgi.BundleUtils;
import org.bndtools.utils.workspace.FileUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ScrolledFormText;
import org.osgi.framework.Bundle;

import bndtools.Plugin;

public abstract class AbstractTemplateSelectionWizardPage extends WizardPage {
    private static final ILogger logger = Logger.getLogger(AbstractTemplateSelectionWizardPage.class);

    public static final String PROP_ELEMENT = "selectedElement";
    protected final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private Tree tree;
    private TreeViewer viewer;
    private ScrolledFormText txtDescription;

    private IConfigurationElement[] elements;
    private IConfigurationElement selectedElement = null;

    private boolean shown = false;

    protected AbstractTemplateSelectionWizardPage(String pageName) {
        super(pageName);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(1, false));

        new Label(container, SWT.NONE).setText("Select Template:");

        tree = new Tree(container, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_table.heightHint = 100;
        tree.setLayoutData(gd_table);

        viewer = new TreeViewer(tree);

        Label lblNewLabel = new Label(container, SWT.NONE);
        lblNewLabel.setText("Description:");

        Composite cmpDescription = new Composite(container, SWT.BORDER);
        cmpDescription.setBackground(tree.getBackground());

        txtDescription = new ScrolledFormText(cmpDescription, SWT.V_SCROLL | SWT.H_SCROLL, false);
        FormText formText = new FormText(txtDescription, SWT.NO_FOCUS);
        txtDescription.setFormText(formText);
        txtDescription.setBackground(tree.getBackground());
        formText.setBackground(tree.getBackground());
        formText.setForeground(tree.getForeground());

        GridData gd_cmpDescription = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd_cmpDescription.heightHint = 100;
        cmpDescription.setLayoutData(gd_cmpDescription);

        GridLayout layout_cmpDescription = new GridLayout(1, false);
        cmpDescription.setLayout(layout_cmpDescription);

        GridData gd_txtDescription = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_txtDescription.heightHint = 100;
        txtDescription.setLayoutData(gd_txtDescription);

        viewer.setContentProvider(new CategorisedPrioritisedConfigurationElementTreeContentProvider(true));
        viewer.setLabelProvider(new ConfigElementLabelProvider(parent.getDisplay(), "icons/template.gif"));

        loadData();

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                Object selected = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                if (selected instanceof IConfigurationElement)
                    setSelectionFromConfigElement((IConfigurationElement) selected);
                else
                    setSelectionFromConfigElement(null);
                updateUI();
            }
        });

        txtDescription.getFormText().addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent event) {
                IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
                try {
                    IWebBrowser externalBrowser = browserSupport.getExternalBrowser();
                    externalBrowser.openURL(new URL((String) event.getHref()));
                } catch (PartInitException e) {
                    logger.logError("Error opening external browser.", e);
                } catch (MalformedURLException e) {
                    // Ignore
                }
            }
        });

        updateUI();
    }

    private void updateUI() {
        if (selectedElement == null) {
            setPageComplete(false);
            setMessage("Select a template", IMessageProvider.INFORMATION);
        } else {
            setPageComplete(true);
            setMessage(null);
        }
    }

    protected abstract IConfigurationElement[] loadConfigurationElements();

    private void loadData() {
        elements = loadConfigurationElements();
        Arrays.sort(elements, new CategorisedConfigurationElementComparator(true));

        viewer.setInput(elements);
        viewer.expandAll();
    }

    private void setSelectionFromConfigElement(IConfigurationElement element) {
        showTemplateDescription(element);

        IConfigurationElement old = this.selectedElement;
        this.selectedElement = element;
        propSupport.firePropertyChange(PROP_ELEMENT, old, element);
    }

    private void showTemplateDescription(IConfigurationElement element) {
        String browserText = "";
        if (element != null) {
            browserText = "<form>No description available.</form>";
            String name = element.getAttribute("name");
            String htmlAttr = element.getAttribute("doc");
            if (htmlAttr != null) {
                String bsn = element.getContributor().getName();
                Bundle bundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), bsn, null);
                if (bundle != null) {
                    URL htmlUrl = bundle.getResource(htmlAttr);
                    if (htmlUrl == null)
                        browserText = String.format("<form>No description for %s.</form>", name);
                    else
                        try {
                            byte[] bytes = FileUtils.readFully(htmlUrl.openStream());
                            browserText = new String(bytes, "UTF-8");
                        } catch (IOException e) {
                            logger.logError("Error reading template description document.", e);
                        }
                }
            }
        }
        txtDescription.setText(browserText);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible && !shown) {
            shown = true;
            if (elements.length > 0) {
                setSelectionFromConfigElement(elements[0]);

                ISelection selection = new StructuredSelection(elements[0]);
                viewer.setSelection(selection);
            }
        }
    }

    @Override
    public boolean isPageComplete() {
        return shown && super.isPageComplete();
    }

    public IConfigurationElement getSelectedElement() {
        return selectedElement;
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
}