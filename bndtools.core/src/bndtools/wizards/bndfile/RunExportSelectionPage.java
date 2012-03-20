package bndtools.wizards.bndfile;

import java.util.HashMap;
import java.util.Map;

import org.bndtools.core.utils.jface.ConfigElementLabelProvider;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import aQute.bnd.build.Project;
import bndtools.api.IBndModel;

public class RunExportSelectionPage extends WizardSelectionPage {
    
    private final IConfigurationElement[] elements;
    private final IBndModel model;
    private final Project bndProject;
    
    private final Map<IConfigurationElement, IWizardNode> nodeCache = new HashMap<IConfigurationElement, IWizardNode>();
    
    private Table table;
    private TableViewer viewer;
    
    protected RunExportSelectionPage(String pageName, IConfigurationElement[] elements, IBndModel model, Project bndProject) {
        super(pageName);
        setDescription("Select a wizard for exporting this Run Descriptor");
        setTitle("Export Wizard Selection");
        this.elements = elements;
        this.model = model;
        this.bndProject = bndProject;
    }

    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        setControl(container);
        container.setLayout(new GridLayout(1, false));

        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new ConfigElementLabelProvider(table.getDisplay(), null));
        
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection sel = viewer.getSelection();
                if (sel.isEmpty())
                    setSelectedNode(null);
                else {
                    IConfigurationElement elem = (IConfigurationElement) ((IStructuredSelection) sel).getFirstElement();
                    IWizardNode node = nodeCache.get(elem);
                    if (node == null) {
                        node = new RunExportWizardNode(getShell(), elem, model, bndProject);
                        nodeCache.put(elem, node);
                    }
                    setSelectedNode(node);
                }
            }
        });
        viewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                IWizardPage nextPage = getNextPage();
                if (nextPage != null)
                    getContainer().showPage(nextPage);
            }
        });

        viewer.setInput(elements);
    }
    
}