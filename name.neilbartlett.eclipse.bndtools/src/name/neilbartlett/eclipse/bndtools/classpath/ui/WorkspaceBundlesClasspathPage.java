package name.neilbartlett.eclipse.bndtools.classpath.ui;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import name.neilbartlett.eclipse.bndtools.UIConstants;
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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
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
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

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
		col.setWidth(200);
		col.setText("Symbolic Name");
		
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(200);
		col.setText("Binding");
		
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new BundleDependencyLabelProvider());
		
		Button btnAdd = new Button(composite, SWT.PUSH);
		btnAdd.setText("Add");
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
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		setControl(composite);
	}
	void doAdd() {
		ElementListSelectionDialog selectionDialog = new ElementListSelectionDialog(getShell(), new LabelProvider() {
			@Override
			public String getText(Object element) {
				ExportedBundle export = (ExportedBundle) element;
				return export.getSymbolicName() + " " + export.getVersion();
			}
		});
		List<ExportedBundle> exports = WorkspaceRepositoryClasspathContainerInitializer.getInstance().getAllWorkspaceExports();
		selectionDialog.setElements(exports.toArray(new Object[exports.size()]));
		
		if(selectionDialog.open() == Window.OK) {
			
		}
	}
	void doRemove() {
		List<?> toRemove = ((IStructuredSelection) viewer.getSelection()).toList();
		
		dependencies.removeAll(toRemove);
		viewer.remove(toRemove.toArray());
		
		projectProperties.setBundleDependencies(dependencies);
		try {
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
				StyledString styledString = new StyledString(dependency.getSymbolicName());
				styledString.append("; " + dependency.getVersionRange().toString(), StyledString.COUNTER_STYLER);
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			} else if(cell.getColumnIndex() == 1){
				if(bundle != null) {
					String boundPath = bundle.getPath().makeRelative().toString();
					cell.setText(boundPath);
				} else {
					StyledString styledString = new StyledString("No matching bundle found", UIConstants.ERROR_STYLER);
					cell.setText(styledString.getString());
					cell.setStyleRanges(styledString.getStyleRanges());
				}
			}
		}
	}
}

