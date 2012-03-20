package bndtools.wizards.shared;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bndtools.core.utils.jface.ConfigElementLabelProvider;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ScrolledFormText;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

import bndtools.Plugin;
import bndtools.utils.BundleUtils;
import bndtools.utils.FileUtils;
import bndtools.utils.PriorityConfigurationElementCompator;

public abstract class AbstractTemplateSelectionWizardPage extends WizardPage {

    public static final String PROP_ELEMENT = "selectedElement";
    protected final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private Table table;
    private TableViewer viewer;
    private ScrolledFormText txtDescription;

    private IConfigurationElement[] elements;
    private IConfigurationElement selectedElement = null;

    private boolean shown = false;

    protected AbstractTemplateSelectionWizardPage(String pageName) {
        super(pageName);
    }

    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(1, false));

        new Label(container, SWT.NONE).setText("Select Template:");

        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_table.heightHint = 100;
        table.setLayoutData(gd_table);

        viewer = new TableViewer(table);

        Label lblNewLabel = new Label(container, SWT.NONE);
        lblNewLabel.setText("Description:");

        txtDescription = new ScrolledFormText(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL, true);
        txtDescription.setBackground(table.getBackground());
        txtDescription.getFormText().setBackground(table.getBackground());
        txtDescription.getFormText().setForeground(table.getForeground());

        GridData gd_txtDescription = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_txtDescription.heightHint = 100;
        txtDescription.setLayoutData(gd_txtDescription);

        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new ConfigElementLabelProvider(parent.getDisplay(), "icons/template.gif"));

        loadData();

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                IConfigurationElement element = (IConfigurationElement) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                setSelectionFromConfigElement(element);
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
                    Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening external browser.", e));
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
        Arrays.sort(elements, new PriorityConfigurationElementCompator(false));
        viewer.setInput(elements);

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
            String htmlAttr = element.getAttribute("doc");
            if (htmlAttr != null) {
                String bsn = element.getContributor().getName();
                Bundle bundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), bsn, null);
                if (bundle != null) {
                    URL htmlUrl = bundle.getResource(htmlAttr);
                    try {
                        byte[] bytes = FileUtils.readFully(htmlUrl.openStream());
                        browserText = new String(bytes);
                    } catch (IOException e) {
                        Plugin.logError("Error reading template description document.", e);
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
            shown  = true;
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