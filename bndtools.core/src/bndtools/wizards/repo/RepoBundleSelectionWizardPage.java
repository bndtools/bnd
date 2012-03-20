package bndtools.wizards.repo;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import aQute.lib.osgi.Constants;
import aQute.libg.header.Attrs;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.model.clauses.VersionedClause;
import bndtools.model.clauses.VersionedClauseLabelProvider;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.model.repo.RepositoryTreeLabelProvider;
import bndtools.model.repo.RepositoryUtils;

public class RepoBundleSelectionWizardPage extends WizardPage {

    public static final String PROP_SELECTION = "selection";
    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private final Map<String,VersionedClause> selectedBundles = new LinkedHashMap<String,VersionedClause>();

	TreeViewer availableViewer;
	Text selectionSearchTxt;

	TableViewer selectedViewer;

    Button addButton;
    Button removeButton;

    ViewerFilter alreadySelectedFilter = new ViewerFilter() {
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            String search = selectionSearchTxt.getText().toLowerCase();

            String bsn = null;
            if(element instanceof RepositoryBundle) {
                bsn = ((RepositoryBundle) element).getBsn();
            } else if (element instanceof ProjectBundle) {
                bsn = ((ProjectBundle) element).getBsn();
            }

            if(bsn != null) {
                if(search.length() > 0 && bsn.toLowerCase().indexOf(search) == -1) {
                    return false;
                }
                return !selectedBundles.containsKey(bsn);
            }
            return true;
        }
    };

    protected RepoBundleSelectionWizardPage() {
		super("bundleSelectionPage");
	}

	public void setSelectedBundles(Collection<VersionedClause> selectedBundles) {
	    for (VersionedClause clause : selectedBundles) {
            this.selectedBundles.put(clause.getName(), clause);
        }
	}

	public List<VersionedClause> getSelectedBundles() {
	    return new ArrayList<VersionedClause>(selectedBundles.values());
	}

	Control createAvailableBundlesPanel(Composite parent) {
	    Composite panel = new Composite(parent, SWT.NONE);
	    new Label(panel, SWT.NONE).setText("Available Bundles:");
	    selectionSearchTxt = new Text(panel, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
	    selectionSearchTxt.setMessage("filter bundle name");

	    final Tree availableTree = new Tree(panel, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
	    availableViewer = new TreeViewer(availableTree);
	    availableViewer.setLabelProvider(new RepositoryTreeLabelProvider());
	    availableViewer.setContentProvider(new RepositoryTreeContentProvider());
	    availableViewer.setAutoExpandLevel(2);

	    availableViewer.setFilters(new ViewerFilter[] { alreadySelectedFilter });

	    // Load data
        try {
            refreshBundleList();
        } catch (Exception e) {
            setErrorMessage("Error querying repository configuration.");
            Plugin.logError("Error querying repository configuration.", e);
        }

        // Listeners
        selectionSearchTxt.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN)
                    availableTree.setFocus();
            }
        });
        selectionSearchTxt.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                availableViewer.setFilters(new ViewerFilter[] { alreadySelectedFilter });
            }
        });
        availableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            // Enable add button when a bundle or bundle version is selected on the left
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection sel = (IStructuredSelection) availableViewer.getSelection();
                for(Iterator<?> iter = sel.iterator(); iter.hasNext(); ) {
                    Object element = iter.next();
                    if(element instanceof RepositoryBundle || element instanceof RepositoryBundleVersion || element instanceof ProjectBundle) {
                        addButton.setEnabled(true);
                        return;
                    }
                }
                addButton.setEnabled(false);
            }
        });
        availableViewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                doAdd();
            }
        });


	    GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, false);
        panel.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        selectionSearchTxt.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 300;
        gd.widthHint = 250;
        availableTree.setLayoutData(gd);

        return panel;
	}

	Control createSelectedBundlesPanel(Composite parent) {
	    Composite panel = new Composite(parent, SWT.NONE);
	    new Label(panel, SWT.NONE).setText("Selected Bundles:");
        Table selectedTable = new Table(panel, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

        selectedViewer = new TableViewer(selectedTable);
        selectedViewer.setContentProvider(new MapValuesContentProvider());
        selectedViewer.setLabelProvider(new VersionedClauseLabelProvider());

        selectedViewer.setInput(selectedBundles);

        selectedViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            // Enable the remove button when a bundle is selected on the right
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection sel = selectedViewer.getSelection();
                removeButton.setEnabled(!sel.isEmpty());
            }
        });
        selectedViewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                doRemove();
            }
        });


        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, false);
        panel.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 300;
        gd.widthHint = 250;
        selectedTable.setLayoutData(gd);

        return panel;
	}

	public void createControl(Composite parent) {
		// Create controls
		Composite composite = new Composite(parent, SWT.NONE);

		Control leftPanel = createAvailableBundlesPanel(composite);
		Composite middlePanel = new Composite(composite, SWT.NONE);
		Control rightPanel = createSelectedBundlesPanel(composite);

		addButton = new Button(middlePanel, SWT.PUSH);
		addButton.setText("Add -->");
		addButton.setEnabled(false);

		removeButton = new Button(middlePanel, SWT.PUSH);
		removeButton.setText("<-- Remove");
		removeButton.setEnabled(false);

		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});

		// LAYOUT
		GridLayout layout;

		layout = new GridLayout(3, false);
		layout.horizontalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		leftPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		middlePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
        rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.marginWidth = 0;
        middlePanel.setLayout(layout);

		addButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, true));
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));

		setControl(composite);
	}

    protected void refreshBundleList() throws Exception {
        Central.getWorkspace().refresh();
        availableViewer.setInput(RepositoryUtils.listRepositories(true));
    }

    void doAdd() {
        IStructuredSelection selection = (IStructuredSelection) availableViewer.getSelection();
        List<VersionedClause> adding = new ArrayList<VersionedClause>(selection.size());
        for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
            Object item = iter.next();
            if (item instanceof RepositoryBundle) {
                adding.add(RepositoryUtils.convertRepoBundle((RepositoryBundle) item));
            } else if (item instanceof RepositoryBundleVersion) {
                adding.add(RepositoryUtils.convertRepoBundleVersion((RepositoryBundleVersion) item));
            } else if (item instanceof ProjectBundle) {
                String bsn = ((ProjectBundle) item).getBsn();
                Attrs attribs = new Attrs();
                attribs.put(Constants.VERSION_ATTRIBUTE, "latest");
                adding.add(new VersionedClause(bsn, attribs));
            }
        }
        if (!adding.isEmpty()) {
            for (VersionedClause clause : adding) {
                selectedBundles.put(clause.getName(), clause);
            }
            selectedViewer.add(adding.toArray(new Object[adding.size()]));
            availableViewer.refresh();
            propSupport.firePropertyChange(PROP_SELECTION, null, selectedBundles);
        }
    }

    void doRemove() {
        IStructuredSelection selection = (IStructuredSelection) selectedViewer.getSelection();
        for (Object clause : selection.toList()) {
            selectedBundles.remove(((VersionedClause) clause).getName());
        }
        selectedViewer.remove(selection.toArray());
        availableViewer.refresh();
        propSupport.firePropertyChange(PROP_SELECTION, null, selectedBundles);
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

class MapValuesContentProvider implements IStructuredContentProvider {

    public Object[] getElements(Object inputElement) {
        Map<?, ?> map = (Map<?, ?>) inputElement;

        Collection<?> values = map.values();
        return values.toArray(new Object[values.size()]);
    }
    public void dispose() {
    }
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
}