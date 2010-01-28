/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.frameworks.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
import name.neilbartlett.eclipse.bndtools.prefs.frameworks.FrameworkPreferencesInitializer;
import name.neilbartlett.eclipse.bndtools.utils.SWTConcurrencyUtil;

import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class FrameworkSelector {
	
	public static final String PROP_SELECTION = "selection";
	public static final String PROP_ERROR_MESSAGE = "errorMessage";

	private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	private final AtomicBoolean updating = new AtomicBoolean(false);
	
	private List<IFrameworkInstance> installedFrameworks;
	private final Map<OSGiSpecLevel, List<IFrameworkInstance>> specMappings = new HashMap<OSGiSpecLevel, List<IFrameworkInstance>>();
	
	private boolean useSpec = true;
	private IFrameworkInstance selectedFramework = null;
	private OSGiSpecLevel selectedSpecLevel;
	private String errorMessage = null;
	
	private Display display;
	private Composite composite;
	private Button btnUseSpecLevel;
	private Button btnUseInstance;
	private Table table;
	private CheckboxTableViewer viewer;
	
	public void createControl(final Composite parent) {
		this.display = parent.getDisplay();
		composite = new Composite(parent, SWT.NONE);
		
		btnUseSpecLevel = new Button(composite, SWT.RADIO);
		btnUseSpecLevel.setText("OSGi Specification");
		
		btnUseInstance = new Button(composite, SWT.RADIO);
		btnUseInstance.setText("Installed Framework");
		
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
				} else {
					setSelection(null);
				}
			}
		});
		Listener btnUseSpecListener = new Listener() {
			public void handleEvent(Event event) {
				if(!updating.get()) {
					useSpec = btnUseSpecLevel.getSelection();
					propertySupport.firePropertyChange(PROP_SELECTION, null, null);
					updateUI();
				}
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
		composite.setLayout(new GridLayout(2, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
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
			/*
			IPath instancePath = selectedFramework.getInstancePath();
			for (IFrameworkInstance installedInstance : installedFrameworks) {
				if(instancePath.equals(installedInstance.getInstancePath())) {
					viewer.setChecked(installedInstance, true);
					return;
				}
			}
			// Wasn't found
			setSelection(null);
			*/
			viewer.setChecked(selectedFramework, true);
		} else {
			viewer.setAllChecked(false);
		}
	}
	
	private void updateUI() {
		if(updating.compareAndSet(false, true)) {
			try {
				btnUseSpecLevel.setSelection(useSpec);
				btnUseInstance.setSelection(!useSpec);
				
				if(useSpec) {
					viewer.setInput(EnumSet.allOf(OSGiSpecLevel.class));
				} else {
					viewer.setInput(installedFrameworks);
				}
				showSelectionInViewer();
			} finally {
				updating.set(false);
			}
		}
	}
	
	public Control getControl() {
		return composite;
	}
	
	public void setUseSpecLevel(boolean useSpec) {
		this.useSpec = useSpec;
		SWTConcurrencyUtil.execForDisplay(display, new Runnable() {
			public void run() {
				updateUI();
			}
		});
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
		String error = null;
		if(selection == null) {
			if(useSpec)
				this.selectedSpecLevel = null;
			else
				this.selectedFramework = null;
			propertySupport.firePropertyChange(PROP_SELECTION, null, null);
		} else if(selection instanceof OSGiSpecLevel) {
			OSGiSpecLevel specLevel = (OSGiSpecLevel) selection;
			this.selectedSpecLevel = specLevel;
			propertySupport.firePropertyChange(PROP_SELECTION, null, null);
			
			IFrameworkInstance instance = FrameworkPreferencesInitializer.getFrameworkInstance(specLevel);
			if(instance == null) {
				error = "No framework instances are available for the selected specification level.";
			}
		} else if(selection instanceof IFrameworkInstance) {
			IFrameworkInstance instance = (IFrameworkInstance) selection;
			this.selectedFramework = instance;
			propertySupport.firePropertyChange(PROP_SELECTION, null, null);
			
			IStatus status = instance.getStatus();
			if(!status.isOK()) {
				error = status.getMessage();
			}
		}
		if(viewer != null && table != null && !table.isDisposed()) {
			showSelectionInViewer();
		}
		setErrorMessage(error);
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
