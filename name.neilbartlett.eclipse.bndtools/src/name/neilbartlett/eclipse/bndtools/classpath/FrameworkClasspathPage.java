package name.neilbartlett.eclipse.bndtools.classpath;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.prefs.frameworks.FrameworkPreferencesInitializer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class FrameworkClasspathPage extends WizardPage implements
		IClasspathContainerPage {
	
	private Table table;
	private CheckboxTableViewer viewer;
	
	private List<IFrameworkInstance> installedFrameworks;
	private IFrameworkInstance selectedFramework = null;
	
	private AtomicBoolean processingCheckEvent = new AtomicBoolean(false);


	public FrameworkClasspathPage() {
		super("frameworkClasspathPage");
	}

	public boolean finish() {
		return selectedFramework != null;
	}

	public IClasspathEntry getSelection() {
		IPath path = new Path(FrameworkClasspathContainerInitializer.FRAMEWORK_CONTAINER_ID);
		path = path.append(selectedFramework.getFrameworkId());
		
		IPath instancePath = selectedFramework.getInstancePath();
		String encodedPath;
		try {
			encodedPath = URLEncoder.encode(instancePath.toString(), "UTF-8");  //$NON-NLS-1$
			path = path.append(encodedPath);
			return JavaCore.newContainerEntry(path);
		} catch (UnsupportedEncodingException e) {
			// TODO
			e.printStackTrace();
			return null;
		}
	}

	public void setSelection(IClasspathEntry containerEntry) {
		try {
			if(containerEntry == null) {
				selectedFramework = null;
			} else {
				IPath containerPath = containerEntry.getPath();
				selectedFramework = FrameworkClasspathContainerInitializer.getFrameworkInstanceForContainerPath(containerPath);
			}
			
			if(viewer != null && table != null && !table.isDisposed()) {
				showSelectionInViewer();
			}
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Error", "Unable to set classpath selection", e.getStatus());
		}
	}
	
	private void showSelectionInViewer() {
		viewer.setAllChecked(false);
		if(selectedFramework != null) {
			IPath instancePath = selectedFramework.getInstancePath();
			for (IFrameworkInstance installedInstance : installedFrameworks) {
				if(instancePath.equals(installedInstance.getInstancePath())) {
					viewer.setChecked(installedInstance, true);
				}
			}
		}
	}

	public void createControl(Composite parent) {
		setTitle("OSGi Framework");
		setMessage("Select an OSGi framework instance.");
		
		Composite composite = new Composite(parent, SWT.NONE);
		new Label(composite, SWT.NONE).setText("Installed OSGi Frameworks:");
		
		table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn col;
		col = new TableColumn(table, SWT.NONE);
		col.setText("Name");
		col.setWidth(300);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText("Path");
		col.setWidth(300);
		
		viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new FrameworkInstanceLabelProvider(parent.getDisplay()));
		
		// Initialise
		loadFrameworkInstances();
		updateUI();
		
		// Events
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if(processingCheckEvent.compareAndSet(false, true)) {
					try {
						if(event.getChecked()) {
							viewer.setAllChecked(false);
							viewer.setChecked(event.getElement(), true);
							
							selectedFramework = (IFrameworkInstance) event.getElement();
						}
					} finally {
						processingCheckEvent.set(false);
					}
				}
			}
		});
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		setControl(composite);
	}

	private void updateUI() {
		viewer.setInput(installedFrameworks);
		showSelectionInViewer();
		
		String error = null;
		if(installedFrameworks == null || installedFrameworks.isEmpty()) {
			error = "No OSGi frameworks are installed";
		}
		
		setErrorMessage(error);
	}

	private void loadFrameworkInstances() {
		installedFrameworks = FrameworkPreferencesInitializer.loadFrameworkInstanceList();
	}

}
