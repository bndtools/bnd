package bndtools.editor.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

import bndtools.Plugin;

public class PluginClassSelectionPage extends WizardPage {

    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private Table table;
    private TableViewer viewer;
    private ScrolledFormText txtDescription;

    private IConfigurationElement selectedElement;
    private boolean programmaticChange = false;
    

    public PluginClassSelectionPage() {
        super("pluginClassSelection");
    }

    public void createControl(Composite parent) {
        setTitle("Plug-in Type");
        setDescription("Select from one of the following known plug-in types.");

        // Create controls
        Composite composite = new Composite(parent, SWT.NONE);
        table = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER);
        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setSorter(new PluginClassSorter());
        viewer.setLabelProvider(new PluginDeclarationLabelProvider());
        
        // txtDescription = new ScrolledFormText(composite, true);

        // Load data
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "bndPlugins");
        viewer.setInput(elements);

        // Listeners
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                if (!programmaticChange) {
                    try {
                        programmaticChange = true;
                        IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
                        setSelectedElement((IConfigurationElement) sel.getFirstElement());
                    } finally {
                        programmaticChange = false;
                    }
                }
            }
        });
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                getContainer().showPage(getNextPage());
            }
        });

        // Layout
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, false);
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        table.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd.heightHint = 150;
        // txtDescription.setLayoutData(gd);
        
        setControl(composite);
    }

    public IConfigurationElement getSelectedElement() {
        return selectedElement;
    }

    public void setSelectedElement(IConfigurationElement selectedElement) {
        IConfigurationElement old = this.selectedElement;
        this.selectedElement = selectedElement;
        if (Display.getCurrent() != null && table != null && !table.isDisposed()) {
            if (!programmaticChange) {
                try {
                    programmaticChange = true;
                    if (selectedElement == null)
                        viewer.setSelection(StructuredSelection.EMPTY);
                    else
                        viewer.setSelection(new StructuredSelection(selectedElement), true);
                } finally {
                    programmaticChange = false;
                }
            }
            // updateDescription();
            validate();
            getContainer().updateButtons();
        }
        propSupport.firePropertyChange("selectedElement", old, selectedElement);
    }
    
    private void updateDescription() {
        String description = null;
        if (selectedElement != null)
            description = selectedElement.getValue();
        
        if (description == null)
            description = "";
        txtDescription.setText(description);
    }

    private void validate() {
        String warning = null;
        
        if (selectedElement != null) {
            String deprecationMsg = selectedElement.getAttribute("deprecated");
            if (deprecationMsg != null)
                warning = "Plugin is deprecated: " + deprecationMsg;
        }
        
        setMessage(warning, IMessageProvider.WARNING);
    }

    @Override
    public boolean isPageComplete() {
        return selectedElement != null;
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
