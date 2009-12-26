package name.neilbartlett.eclipse.bndtools.frameworks.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.classpath.FrameworkInstanceLabelProvider;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.prefs.frameworks.FrameworkPreferencesInitializer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class FrameworkSelector {
	
	public static final String PROP_SELECTED_FRAMEWORK = "selectedFramework";
	public static final String PROP_ERROR_MESSAGE = "errorMessage";

	private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	
	private List<IFrameworkInstance> installedFrameworks;
	private IFrameworkInstance selectedFramework = null;
	private String errorMessage = null;
	
	private Composite composite;
	private Table table;
	private CheckboxTableViewer viewer;
	
	public void createControl(final Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		//new Label(composite, SWT.NONE).setText("Installed OSGi Frameworks:");
		
		table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		Link configFrameworksLink = new Link(composite, SWT.NONE);
		configFrameworksLink.setText("<a>Configure frameworks</a>");
		
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
				if(event.getChecked()) {
					viewer.setAllChecked(false);
					viewer.setChecked(event.getElement(), true);
					
					setSelectedFramework((IFrameworkInstance) event.getElement());
				}
			}
		});
		configFrameworksLink.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(parent.getShell(), Plugin.ID_FRAMEWORKS_PREF_PAGE, new String[] { Plugin.ID_FRAMEWORKS_PREF_PAGE }, null);
				dialog.open();
				loadFrameworkInstances();
				updateUI();
			}
		});
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	
	private void loadFrameworkInstances() {
		installedFrameworks = FrameworkPreferencesInitializer.loadFrameworkInstanceList();
	}
	
	private void showSelectionInViewer() {
		viewer.setAllChecked(false);
		if(selectedFramework != null) {
			IPath instancePath = selectedFramework.getInstancePath();
			for (IFrameworkInstance installedInstance : installedFrameworks) {
				if(instancePath.equals(installedInstance.getInstancePath())) {
					viewer.setChecked(installedInstance, true);
					return;
				}
			}
			
			// Wasn't found
			setSelectedFramework(null);
		}
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
	
	public Control getControl() {
		return composite;
	}

	public IFrameworkInstance getSelectedFramework() {
		return selectedFramework;
	}

	public void setSelectedFramework(IFrameworkInstance selectedFramework) {
		IFrameworkInstance oldSelection = this.selectedFramework;
		this.selectedFramework = selectedFramework;
		propertySupport.firePropertyChange(PROP_SELECTED_FRAMEWORK, oldSelection, selectedFramework);
		
		if(viewer != null && table != null && !table.isDisposed()) {
			showSelectionInViewer();
		}
	}
	
	private void setErrorMessage(String errorMessage) {
		String oldErrorMessage = this.errorMessage;
		this.errorMessage = errorMessage;
		propertySupport.firePropertyChange(PROP_ERROR_MESSAGE, oldErrorMessage, errorMessage);
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}

	// Property Change Support Delegate Methods
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}


	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(propertyName, listener);
	}


	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}


	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(propertyName, listener);
	}
}
