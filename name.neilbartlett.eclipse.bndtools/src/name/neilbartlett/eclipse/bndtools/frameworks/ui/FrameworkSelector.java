package name.neilbartlett.eclipse.bndtools.frameworks.ui;

import static name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel.r4_0;
import static name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel.r4_1;
import static name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel.r4_2;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class FrameworkSelector {
	
	public static final String PROP_USE_SPEC_LEVEL = "useSpecLevel";
	public static final String PROP_SELECTED_SPEC_LEVEL = "selectedSpecLevel";
	public static final String PROP_SELECTED_FRAMEWORK = "selectedFramework";
	public static final String PROP_ERROR_MESSAGE = "errorMessage";

	private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	
	private List<IFrameworkInstance> installedFrameworks;
	private final Map<OSGiSpecLevel, List<IFrameworkInstance>> specMappings = new HashMap<OSGiSpecLevel, List<IFrameworkInstance>>();
	
	private boolean useSpec = true;
	private IFrameworkInstance selectedFramework = null;
	private OSGiSpecLevel selectedSpecLevel;
	private String errorMessage = null;
	
	private Composite composite;
	private Table table;
	private CheckboxTableViewer viewer;
	
	public void createControl(final Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		
		final Button btnUseSpecLevel = new Button(composite, SWT.RADIO);
		btnUseSpecLevel.setText("OSGi Specification");
		btnUseSpecLevel.setSelection(useSpec);
		
		Button btnUseInstance = new Button(composite, SWT.RADIO);
		btnUseInstance.setText("Installed Framework");
		btnUseInstance.setSelection(!useSpec);
		
		table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		
		Link configFrameworksLink = new Link(composite, SWT.NONE);
		configFrameworksLink.setText("<a>Configure frameworks</a>");
		
		viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new OSGiFrameworkLabelProvider(parent.getDisplay(), specMappings));
		
		// Initialise
		loadFrameworkInstances();
		updateUI();
		
		// Events
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if(event.getChecked()) {
					viewer.setAllChecked(false);
					viewer.setChecked(event.getElement(), true);
					
					setSelection(event.getElement());
				}
			}
		});
		Listener btnUseSpecListener = new Listener() {
			public void handleEvent(Event event) {
				useSpec = btnUseSpecLevel.getSelection();
				updateUI();
			}
		};
		btnUseSpecLevel.addListener(SWT.Selection, btnUseSpecListener);
		btnUseInstance.addListener(SWT.Selection, btnUseSpecListener);
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
		configFrameworksLink.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
	}
	
	
	private void loadFrameworkInstances() {
		installedFrameworks = FrameworkPreferencesInitializer.loadFrameworkInstanceList();
		specMappings.clear();
		for (IFrameworkInstance instance : installedFrameworks) {
			OSGiSpecLevel specLevel = instance.getOSGiSpecLevel();
			List<IFrameworkInstance> mapping = specMappings.get(specLevel);
			if(mapping == null) {
				mapping = new LinkedList<IFrameworkInstance>();
				specMappings.put(specLevel, mapping);
			}
			mapping.add(instance);
		}
	}
	
	private void showSelectionInViewer() {
		viewer.setAllChecked(false);
		if(useSpec && selectedSpecLevel != null) {
			viewer.setChecked(selectedSpecLevel, true);
		} else if(selectedFramework != null) {
			IPath instancePath = selectedFramework.getInstancePath();
			for (IFrameworkInstance installedInstance : installedFrameworks) {
				if(instancePath.equals(installedInstance.getInstancePath())) {
					viewer.setChecked(installedInstance, true);
					return;
				}
			}
			
			// Wasn't found
			setSelection(null);
		}
	}
	
	private void updateUI() {
		if(useSpec) {
			viewer.setInput(new OSGiSpecLevel[] { r4_0, r4_1, r4_2 });
		} else {
			viewer.setInput(installedFrameworks);
		}
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
	
	public void setUseSpecLevel(boolean useSpec) {
		this.useSpec = useSpec;
	}
	
	public boolean isUseSpecLevel() {
		return useSpec;
	}
	
	public OSGiSpecLevel getSelectedSpecLevel() {
		return selectedSpecLevel;
	}
	
	public IFrameworkInstance getSelectedFramework() {
		return selectedFramework;
	}

	public void setSelection(Object selection) {
		if(selection instanceof OSGiSpecLevel) {
			OSGiSpecLevel specLevel = (OSGiSpecLevel) selection;
			
			OSGiSpecLevel oldValue = this.selectedSpecLevel;
			this.selectedSpecLevel = specLevel;
			propertySupport.firePropertyChange(PROP_SELECTED_SPEC_LEVEL, oldValue, specLevel);
		} else if(selection instanceof IFrameworkInstance) {
			IFrameworkInstance instance = (IFrameworkInstance) selection;
			
			IFrameworkInstance oldValue = this.selectedFramework;
			this.selectedFramework = instance;
			propertySupport.firePropertyChange(PROP_SELECTED_FRAMEWORK, oldValue, instance);
		}
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
