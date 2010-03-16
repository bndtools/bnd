package name.neilbartlett.eclipse.bndtools.editor.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import name.neilbartlett.eclipse.bndtools.editor.model.VersionedClause;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;

import aQute.bnd.plugin.Central;
import aQute.lib.osgi.Constants;

public class RepoBundleSelectionWizardPage extends WizardPage {
	
	private Collection<? super VersionedClause> selectedBundles = new ArrayList<VersionedClause>();
	private TreeViewer selectionViewer;
	private TableViewer selectedViewer;

	protected RepoBundleSelectionWizardPage(String pageName) {
		super(pageName);
	}
	
	public void setSelectedBundles(Collection<? super VersionedClause> selectedBundles) {
		this.selectedBundles = selectedBundles;
	}

	public void createControl(Composite parent) {
		setTitle("Edit Bundles");
		
		// Create controls
		Composite composite = new Composite(parent, SWT.NONE);
		
		new Label(composite, SWT.NONE).setText("Available Bundles:");
		new Label(composite, SWT.NONE); // Spacer
		new Label(composite, SWT.NONE).setText("Selected Bundles:");
		
		Tree selectionTree = new Tree(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		selectionViewer = new TreeViewer(selectionTree);
		selectionViewer.setLabelProvider(new RepositoryTreeLabelProvider());
		selectionViewer.setContentProvider(new RepositoryTreeContentProvider());
		selectionViewer.setAutoExpandLevel(2);
		
		final Button addButton = new Button(composite, SWT.PUSH);
		addButton.setText("Add -->");
		addButton.setEnabled(false);
		
		Table selectedTable = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		selectedTable.setHeaderVisible(true);
		selectedTable.setLinesVisible(false);
		
		TableColumn col;
		
		col = new TableColumn(selectedTable, SWT.NONE);
		col.setText("Bundle");
		col.setWidth(150);
		
		col = new TableColumn(selectedTable, SWT.NONE);
		col.setText("Version");
		col.setWidth(100);

		selectedViewer = new TableViewer(selectedTable);
		selectedViewer.setContentProvider(new ArrayContentProvider());
		selectedViewer.setLabelProvider(new VersionedClauseLabelProvider());

		final Button removeButton = new Button(composite, SWT.PUSH);
		removeButton.setText("<-- Remove");
		removeButton.setEnabled(false);

		// Filter
		selectionViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if(element instanceof RepositoryBundle) {
					String bsn = ((RepositoryBundle) element).getBsn();
					for (Object selectedItem : selectedBundles) {
						if(selectedItem instanceof VersionedClause) {
							VersionedClause bundle = (VersionedClause) selectedItem;
							if(bundle.getName().equals(bsn)) {
								return false;
							}
						}
					}
				}
				return true;
			}
		});

		// Load data
		try {
			selectionViewer.setInput(Central.getWorkspace());
		} catch (Exception e) {
			setErrorMessage("Error querying repostory configuration.");
		}
		selectedViewer.setInput(selectedBundles);

		// Listeners
		selectionViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			// Enable add button when a bundle or bundle version is selected on the left
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) selectionViewer.getSelection();
				for(Iterator<?> iter = sel.iterator(); iter.hasNext(); ) {
					Object element = iter.next();
					if(element instanceof RepositoryBundle || element instanceof RepositoryBundleVersion) {
						addButton.setEnabled(true);
						return;
					}
				}
				addButton.setEnabled(false);
			}
		});
		selectedViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			// Enable the remove button when a bundle is selected on the right
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection sel = selectedViewer.getSelection();
				removeButton.setEnabled(!sel.isEmpty());
			}
		});
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		selectionViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				doAdd();
			}
		});
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});
		selectedViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				doRemove();
			}
		});

		// Layout
		GridLayout layout;
		GridData gd;
		layout = new GridLayout(3, false);
		composite.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
		gd.widthHint = 250;
		selectionTree.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
		gd.widthHint = 250;
		selectedTable.setLayoutData(gd);

		addButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		setControl(composite);
	}
	
	void doAdd() {
		IStructuredSelection selection = (IStructuredSelection) selectionViewer.getSelection();
		List<VersionedClause> adding = new ArrayList<VersionedClause>(selection.size());
		for(Iterator<?> iter = selection.iterator(); iter.hasNext(); ) {
			Object item = iter.next();
			if(item instanceof RepositoryBundle) {
				String bsn = ((RepositoryBundle) item).getBsn();
				adding.add(new VersionedClause(bsn, new HashMap<String, String>()));
			} else if(item instanceof RepositoryBundleVersion) {
				RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) item;
				Map<String,String> attribs = new HashMap<String, String>();
				attribs.put(Constants.VERSION_ATTRIBUTE, bundleVersion.getVersion().toString());
				adding.add(new VersionedClause(bundleVersion.getBundle().getBsn(), attribs));
			}
		}
		if(!adding.isEmpty()) {
			selectedBundles.addAll(adding);
			selectedViewer.add((Object[]) adding.toArray(new Object[adding.size()]));
			selectionViewer.refresh();
		}
	}
	void doRemove() {
		IStructuredSelection selection = (IStructuredSelection) selectedViewer.getSelection();
		selectedBundles.removeAll(selection.toList());
		selectedViewer.remove(selection.toArray());
		selectionViewer.refresh();
	}
}
