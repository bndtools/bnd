package name.neilbartlett.eclipse.bndtools.prefs.frameworks.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
import name.neilbartlett.eclipse.bndtools.frameworks.ui.OSGiFrameworkLabelProvider;
import name.neilbartlett.eclipse.bndtools.prefs.frameworks.FrameworkPreferencesInitializer;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferredInstancesPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {
	
	private OSGiSpecLevel selectedSpecLevel = null;

	private Table tableSpecLevels;
	private Table tableFrameworks;
	private TableViewer viewerSpecLevels;
	private CheckboxTableViewer viewerFrameworks;

	public PreferredInstancesPreferencePage() {
	}

	public PreferredInstancesPreferencePage(String title) {
		super(title);
	}

	public PreferredInstancesPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		
		Composite composite = new Composite(parent, SWT.NONE);
		new Label(composite, SWT.NONE).setText("OSGi Specification Levels:");
		tableSpecLevels = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
		new Label(composite, SWT.NONE).setText("Compatible Frameworks:");
		tableFrameworks = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
		
		// Viewers
		viewerSpecLevels = new TableViewer(tableSpecLevels);
		viewerSpecLevels.setContentProvider(new ArrayContentProvider());
		viewerSpecLevels.setLabelProvider(new OSGiFrameworkLabelProvider(parent.getDisplay(), null));
		viewerSpecLevels.setInput(EnumSet.allOf(OSGiSpecLevel.class));
		
		viewerFrameworks = new CheckboxTableViewer(tableFrameworks);
		viewerFrameworks.setContentProvider(new ArrayContentProvider());
		viewerFrameworks.setLabelProvider(new OSGiFrameworkLabelProvider(parent.getDisplay(), null));
		updateFrameworkList();
		
		// Listeners
		viewerSpecLevels.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selectedSpecLevel = (OSGiSpecLevel) ((IStructuredSelection) viewerSpecLevels.getSelection()).getFirstElement();
				updateFrameworkList();
			}
		});
		viewerFrameworks.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if(event.getChecked()) {
					viewerFrameworks.setAllChecked(false);
					viewerFrameworks.setChecked(event.getElement(), true);
					
					FrameworkPreferencesInitializer.savePreferredFrameworkMapping(selectedSpecLevel, (IFrameworkInstance) event.getElement());
				}
			}
		});
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
		tableSpecLevels.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableFrameworks.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return composite;
	}
	
	void updateFrameworkList() {
		if(selectedSpecLevel == null) {
			viewerFrameworks.setInput(null);
		} else {
			List<IFrameworkInstance> instanceList = FrameworkPreferencesInitializer.loadFrameworkInstanceList();
			List<IFrameworkInstance> filtered = new ArrayList<IFrameworkInstance>(instanceList.size());
			for (IFrameworkInstance instance : instanceList) {
				if(instance.getOSGiSpecLevel() == selectedSpecLevel)
					filtered.add(instance);
			}
			viewerFrameworks.setInput(filtered);
			
			IFrameworkInstance preferredInstance = FrameworkPreferencesInitializer.loadPreferredFrameworkMapping(selectedSpecLevel);
			if(preferredInstance != null) {
				viewerFrameworks.setCheckedElements(new Object[] { preferredInstance });
			} else {
				viewerFrameworks.setAllChecked(false);
			}
		}
	}

	public void init(IWorkbench workbench) {
	}
}
