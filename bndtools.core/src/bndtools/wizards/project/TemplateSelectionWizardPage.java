package bndtools.wizards.project;

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
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
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.api.IProjectTemplate;
import bndtools.utils.PriorityConfigurationElementCompator;

public class TemplateSelectionWizardPage extends WizardPage {

    private Table table;
    private TableViewer viewer;

    private IConfigurationElement[] elements;

    private IProjectTemplate selectedTemplate = null;
    private boolean shown = false;

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
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        viewer = new TableViewer(table);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new TemplateLabelProvider(parent.getDisplay()));

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                IConfigurationElement element = (IConfigurationElement) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                setSelectionFromConfigElement(element);
                updateUI();
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
            selectedTemplate = element != null ? (IProjectTemplate) element.createExecutableExtension("class") : null;
        } catch (CoreException e) {
            error = e.getMessage();
        }
        setErrorMessage(error);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            shown  = true;
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

}
