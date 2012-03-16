package bndtools.wizards.bndfile;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

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
        viewer.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                IConfigurationElement element = (IConfigurationElement) cell.getElement();
                
                String name = element.getAttribute("name");
                cell.setText(name);
            }
        });
        
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection sel = viewer.getSelection();
                if (sel.isEmpty())
                    setSelectedNode(null);
                else {
                    IConfigurationElement elem = (IConfigurationElement) ((IStructuredSelection) sel).getFirstElement();
                    IWizardNode node = nodeCache.get(elem);
                    if (node == null) {
                        node = new RunExportWizardNode(elem, model, bndProject);
                        nodeCache.put(elem, node);
                    }
                    setSelectedNode(node);
                }
            }
        });

        viewer.setInput(elements);
    }
    
}
