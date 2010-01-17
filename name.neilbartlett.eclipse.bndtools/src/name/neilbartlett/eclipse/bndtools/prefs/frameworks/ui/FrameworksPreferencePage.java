package name.neilbartlett.eclipse.bndtools.prefs.frameworks.ui;

import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.ui.FrameworkInstanceLabelProvider;
import name.neilbartlett.eclipse.bndtools.prefs.frameworks.FrameworkPreferencesInitializer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;

public class FrameworksPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private List<IFrameworkInstance> instances;
	
	private IWorkbench workbench;
	private Shell parentShell;
	private TableViewer viewer;
	private Button btnAdd;
	private Button btnRemove;

	public FrameworksPreferencePage() {
		super("OSGi Frameworks", null);
	}

	@Override
	protected Control createContents(Composite parent) {
		parentShell = parent.getShell();
		
		noDefaultAndApplyButton();
		Composite composite = new Composite(parent, SWT.NONE);
		
		new Label(composite, SWT.NONE).setText("Installed OSGi Frameworks:");
		new Label(composite, SWT.NONE); // Spacer
		
		Table table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn col;
		col = new TableColumn(table, SWT.NONE);
		col.setText("Name");
		col.setWidth(300);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText("Path");
		col.setWidth(300);
		
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new FrameworkInstanceLabelProvider(parent.getDisplay()));
		
		btnAdd = new Button(composite, SWT.PUSH);
		btnAdd.setText("Add");
		
		btnRemove = new Button(composite, SWT.PUSH);
		btnRemove.setText("Remove");
		
		// Initialise
		loadFrameworkInstanceList();
		
		// Events
		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doAddFrameworkInstance();
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doRemoveFrameworkInstance();
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		
		composite.setLayout(new GridLayout(2, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		return composite;
	}
	
	public void init(IWorkbench workbench) {
		this.workbench = workbench;
	}
	
	void loadFrameworkInstanceList() {
		instances = FrameworkPreferencesInitializer.loadFrameworkInstanceList();
		viewer.setInput(instances);
	}
	
	private void updateButtons() {
		btnRemove.setEnabled(!viewer.getSelection().isEmpty());
	}
	
	private void doAddFrameworkInstance() {
		CreateFrameworkInstanceWizard wizard = new CreateFrameworkInstanceWizard(instances);
		WizardDialog dialog = new WizardDialog(parentShell, wizard);
		dialog.open();
		loadFrameworkInstanceList();
	}
	
	private void doRemoveFrameworkInstance() {
		Object selected = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
		
		instances.remove(selected);
		try {
			FrameworkPreferencesInitializer.saveFrameworkInstancesList(instances);
		} catch (BackingStoreException e) {
			IStatus status = new Status(ERROR, Plugin.PLUGIN_ID, 0, "Error saving preferences.", e);
			ErrorDialog.openError(getShell(), "Error", null, status);
		}
		loadFrameworkInstanceList();
	}
}
