package name.neilbartlett.eclipse.bndtools.classpath.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import name.neilbartlett.eclipse.bndtools.classpath.BundleDependency;
import name.neilbartlett.eclipse.bndtools.classpath.ExportedBundle;
import name.neilbartlett.eclipse.bndtools.classpath.WorkspaceRepositoryClasspathContainerInitializer;
import name.neilbartlett.eclipse.bndtools.project.BndProjectProperties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.osgi.framework.Constants;

import aQute.libg.version.VersionRange;

public class WorkspaceBundlesClasspathPage extends WizardPage implements IClasspathContainerPage, IClasspathContainerPageExtension {

	private Table table;
	private TableViewer viewer;
	
	private IJavaProject project;
	private BndProjectProperties projectProperties;
	private List<BundleDependency> dependencies = null;
	private Map<BundleDependency, ExportedBundle> bindings;

	public WorkspaceBundlesClasspathPage() {
		super("workspaceBundlesClasspathPath");
	}
	public boolean finish() {
		// TODO Auto-generated method stub
		return false;
	}
	public IClasspathEntry getSelection() {
		// TODO Auto-generated method stub
		return null;
	}
	public void setSelection(IClasspathEntry containerEntry) {
		if(table != null && !table.isDisposed()) {
			viewer.setInput(dependencies);
		}
	}
	public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
		this.project = project;
		
		projectProperties = new BndProjectProperties(project.getProject());
		try {
			projectProperties.load();
			dependencies = projectProperties.getBundleDependencies();
			bindings = WorkspaceRepositoryClasspathContainerInitializer.getInstance().getBindingsForProject(project.getProject());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void createControl(Composite parent) {
		setTitle("Imported Workspace Bundles");
		
		Composite composite = new Composite(parent, SWT.NONE);
		
		Label lblTitle = new Label(composite, SWT.NONE);
		lblTitle.setText("Import Bundles:");
		
		table = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		table.setHeaderVisible(true);
		TableColumn col;
		
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(150);
		col.setText("Symbolic-Name");
		
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(100);
		col.setText("Version");
		
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(200);
		col.setText("Bound Location");
		
		viewer = new TableViewer(table);
		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new BundleDependencyLabelProvider());
		viewer.setColumnProperties(new String[] { Constants.BUNDLE_SYMBOLICNAME, Constants.BUNDLE_VERSION, "location" });
		
		viewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				BundleDependency dep1 = (BundleDependency) e1;
				BundleDependency dep2 = (BundleDependency) e2;
				
				return dep1.compareTo(dep2);
			}
		});
		viewer.setCellModifier(new ICellModifier() {
			public void modify(Object element, String property, Object value) {
				if(element instanceof Item) {
					element = ((Item) element).getData();
				}
				BundleDependency dependency = (BundleDependency) element;
				BundleDependency newDep = null;
				
				if(value != null) {
					if(Constants.BUNDLE_SYMBOLICNAME.equals(property)) {
						newDep = new BundleDependency((String) value, dependency.getVersionRange());
					} else if(Constants.BUNDLE_VERSION.equals(property)) {
						newDep = new BundleDependency(dependency.getSymbolicName(), (VersionRange) value);
					}
				}
				
				if(newDep != null) {
					dependencies.remove(dependency);
					viewer.remove(dependency);
					dependencies.add(newDep);
					viewer.add(newDep);
					
					try {
						projectProperties.setBundleDependencies(dependencies);
						projectProperties.save();
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			public Object getValue(Object element, String property) {
				BundleDependency dependency = (BundleDependency) element;
				if(Constants.BUNDLE_SYMBOLICNAME.equals(property)) {
					return dependency.getSymbolicName();
				} else if(Constants.BUNDLE_VERSION.equals(property)) {
					return dependency.getVersionRange();
				} else if("location".equals(property)){
					return bindings.get(dependency);
				}
				return null;
			}
			public boolean canModify(Object element, String property) {
				return Constants.BUNDLE_SYMBOLICNAME.equals(property) || Constants.BUNDLE_VERSION.equals(property);
			}
		});
		CellEditor[] editors = new CellEditor[3];
		editors[0] = new TextCellEditor(table);
		editors[1] = new TextCellEditor(table) {
			@Override
			protected void doSetValue(Object value) {
				VersionRange range = (VersionRange) value;
				if(range == null) range = new VersionRange("0");
				super.doSetValue(range.toString());
			};
			@Override
			protected Object doGetValue() {
				String text = (String) super.doGetValue();
				try {
					return new VersionRange(text);
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
		};
		viewer.setCellEditors(editors);
		
		Button btnAdd = new Button(composite, SWT.PUSH);
		btnAdd.setText("Browse...");
		
		Button btnAddBlank = new Button(composite, SWT.PUSH);
		btnAddBlank.setText("Add");
		
		final Button btnRemove = new Button(composite, SWT.PUSH);
		btnRemove.setText("Remove");
		btnRemove.setEnabled(false);
		
		// Init
		viewer.setInput(dependencies);
		
		// Listeners
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				btnRemove.setEnabled(!viewer.getSelection().isEmpty());
			}
		});
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		btnAddBlank.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				BundleDependency newDep = new BundleDependency("", new VersionRange("0"));
				dependencies.add(newDep);
				try {
					projectProperties.setBundleDependencies(dependencies);
					projectProperties.save();
					viewer.add(newDep);
					viewer.editElement(newDep, 0);
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});
		
		//Layout
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(2, false));
		lblTitle.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnAddBlank.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		setControl(composite);
	}
	void doAdd() {
		List<BundleDependency> adding = new ArrayList<BundleDependency>();

		ExportedBundleSelectionDialog selectionDlg = new ExportedBundleSelectionDialog(getShell());
		if(selectionDlg.open() == Window.OK) {
			IStructuredSelection selection = (IStructuredSelection) selectionDlg.getSelection();
			Iterator<?> iter = selection.iterator();
			while(iter.hasNext()) {
				ExportedBundle export = (ExportedBundle) iter.next();
				adding.add(new BundleDependency(export.getSymbolicName(), new VersionRange(export.getVersion().toString())));
			}
		}
		if(!adding.isEmpty()) {
			dependencies.addAll(adding);
			viewer.add(adding.toArray(new Object[adding.size()]));
			try {
				projectProperties.setBundleDependencies(dependencies);
				projectProperties.save();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	void doRemove() {
		List<?> toRemove = ((IStructuredSelection) viewer.getSelection()).toList();
		
		dependencies.removeAll(toRemove);
		viewer.remove(toRemove.toArray());
		
		try {
			projectProperties.setBundleDependencies(dependencies);
			projectProperties.save();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	class BundleDependencyLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			BundleDependency dependency = (BundleDependency) cell.getElement();
			ExportedBundle bundle = bindings.get(dependency);
			
			if(cell.getColumnIndex() == 0) {
				String name = dependency.getSymbolicName();
				cell.setText(name);
			} else if(cell.getColumnIndex() == 1) {
				StyledString styledString = new StyledString(dependency.getVersionRange().toString(), StyledString.COUNTER_STYLER);
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			} else if(cell.getColumnIndex() == 2){
				if(bundle != null) {
					String boundPath = bundle.getPath().makeRelative().toString();
					cell.setText(boundPath);
				}
			}
		}
	}
}