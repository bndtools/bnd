package bndtools.wizards.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
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
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

import bndtools.Plugin;
import bndtools.api.IProjectTemplate;
import bndtools.utils.BundleUtils;
import bndtools.utils.FileUtils;
import bndtools.utils.PriorityConfigurationElementCompator;

public class TemplateSelectionWizardPage extends WizardPage {

    static final String PROP_TEMPLATE = "template";
    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private Table table;
    private TableViewer viewer;

    private IConfigurationElement[] elements;

    private IProjectTemplate selectedTemplate = null;
    private boolean shown = false;
    private Browser browser;

    private boolean programmaticBrowserChange = false;
    private Label lblBrowserStatus;

    /**
     * Create the wizard.
     */
    public TemplateSelectionWizardPage() {
        super("wizardPage");
        setTitle("Project Templates");
        setDescription("");
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(1, false));

        new Label(container, SWT.NONE).setText("Select Template:");

        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_table.heightHint = 200;
        table.setLayoutData(gd_table);

        viewer = new TableViewer(table);

        Label lblNewLabel = new Label(container, SWT.NONE);
        lblNewLabel.setText("Description:");

        browser = new Browser(container, SWT.BORDER);
        GridData gd_browser = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_browser.heightHint = 150;
        browser.setLayoutData(gd_browser);

        lblBrowserStatus = new Label(container, SWT.NONE);
        lblBrowserStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new TemplateLabelProvider(parent.getDisplay()));

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                IConfigurationElement element = (IConfigurationElement) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                setSelectionFromConfigElement(element);
                updateUI();
            }
        });
        browser.addLocationListener(new LocationListener() {
            public void changing(LocationEvent event) {
                if (!programmaticBrowserChange) {
                    event.doit = false;
                    IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
                    try {
                        IWebBrowser externalBrowser = browserSupport.getExternalBrowser();
                        externalBrowser.openURL(new URL(event.location));
                    } catch (PartInitException e) {
                        Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening external browser.", e));
                    } catch (MalformedURLException e) {
                        // Ignore
                    }
                }
            }
            public void changed(LocationEvent event) {
            }
        });

        loadData();
        updateUI();
    }

    private void updateUI() {
        if (selectedTemplate == null) {
            setPageComplete(false);
            setMessage("Select a template", IMessageProvider.INFORMATION);
        } else {
            setPageComplete(true);
            setMessage(null);
        }
    }

    private void loadData() {
        elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "projectTemplates");
        Arrays.sort(elements, new PriorityConfigurationElementCompator(false));
        viewer.setInput(elements);

        if (elements.length > 0) {
            setSelectionFromConfigElement(elements[0]);

            ISelection selection = new StructuredSelection(elements[0]);
            viewer.setSelection(selection);
        }

    }

    public IProjectTemplate getTemplate() {
        return selectedTemplate;
    }

    void setSelectionFromConfigElement(IConfigurationElement element) {
        String error = null;
        try {
            showTemplateDescription(element);
            IProjectTemplate oldTemplate = selectedTemplate;
            selectedTemplate = element != null ? (IProjectTemplate) element.createExecutableExtension("class") : null;
            propSupport.firePropertyChange(PROP_TEMPLATE, oldTemplate, selectedTemplate);
        } catch (CoreException e) {
            error = e.getMessage();
        }
        setErrorMessage(error);
    }

    private void showTemplateDescription(IConfigurationElement element) {
        String browserText = "";
        if (element != null) {
            browserText = "No description available.";
            String htmlAttr = element.getAttribute("docHtml");
            if (htmlAttr != null) {
                String bsn = element.getContributor().getName();
                Bundle bundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), bsn, null);
                if (bundle != null) {
                    URL htmlUrl = bundle.getResource(htmlAttr);
                    try {
                        byte[] bytes = FileUtils.readFully(htmlUrl.openStream());
                        browserText = new String(bytes);
                    } catch (IOException e) {
                        Plugin.logError("Error reading project template description HTML.", e);
                    }
                }
            }
        }

        try {
            programmaticBrowserChange = true;
            browser.setText(browserText);
        } finally {
            programmaticBrowserChange = false;
        }

    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            shown  = true;

            StatusTextListener statusTextListener = new StatusTextListener() {
                public void changed(StatusTextEvent event) {
                    if (!programmaticBrowserChange) {
                        String message;
                        if (event.text == null || event.text.length() == 0 || "about:blank".equalsIgnoreCase(event.text))
                            message = "";
                        else
                            message = MessageFormat.format("Open page \"{0}\" in an external browser.", event.text);
                        lblBrowserStatus.setText(message);
                    }
                }
            };
            browser.addStatusTextListener(statusTextListener);
        }
    }

    @Override
    public boolean isPageComplete() {
        return shown && super.isPageComplete();
    }

    private static final class TemplateLabelProvider extends StyledCellLabelProvider {

        private final Image img;

        public TemplateLabelProvider(Device device) {
            img = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/template.gif").createImage(device);
        }

        @Override
        public void update(ViewerCell cell) {
            IConfigurationElement element = (IConfigurationElement) cell.getElement();
            cell.setText(element.getAttribute("name"));
            cell.setImage(img);
        }

        @Override
        public void dispose() {
            super.dispose();
            img.dispose();
        }
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
