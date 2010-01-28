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
package name.neilbartlett.eclipse.bndtools.classpath.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import name.neilbartlett.eclipse.bndtools.classpath.FrameworkClasspathContainer;
import name.neilbartlett.eclipse.bndtools.classpath.FrameworkClasspathContainerInitializer;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
import name.neilbartlett.eclipse.bndtools.frameworks.ui.FrameworkSelector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

public class FrameworkClasspathPage extends WizardPage implements
		IClasspathContainerPage {
	
	private final FrameworkSelector selector = new FrameworkSelector();
	private boolean includeAnnotations = true;

	public FrameworkClasspathPage() {
		super("frameworkClasspathPage");
	}

	@Override
	public boolean isPageComplete() {
		boolean useSpec = selector.isUseSpecLevel();
		if(useSpec && selector.getSelectedSpecLevel() == null) return false;
		if(!useSpec && selector.getSelectedFramework() == null) return false;
		return selector.getErrorMessage() == null;
	}
	
	public boolean finish() {
		return isPageComplete();
	}

	public IClasspathEntry getSelection() {
		FrameworkClasspathContainer classpathContainer;
		if(selector.isUseSpecLevel()) {
			classpathContainer = FrameworkClasspathContainer.createForSpecLevel(selector.getSelectedSpecLevel(), includeAnnotations);
		} else {
			classpathContainer = FrameworkClasspathContainer.createForSpecificFramework(selector.getSelectedFramework(), includeAnnotations);
		}
		if(classpathContainer == null)
			return null;
			
		IPath path = FrameworkClasspathContainerInitializer.createPathForContainer(classpathContainer);
		return JavaCore.newContainerEntry(path);
	}

	public void setSelection(IClasspathEntry containerEntry) {
		if(containerEntry == null) {
			selector.setSelection(null);
		} else {
			IPath containerPath = containerEntry.getPath();
			FrameworkClasspathContainer classpathContainer = FrameworkClasspathContainerInitializer.createClasspathContainerForPath(containerPath);
			OSGiSpecLevel specLevel = classpathContainer.getSpecLevel();
			if(specLevel != null) {
				selector.setUseSpecLevel(true);
				selector.setSelection(specLevel);
			} else {
				selector.setUseSpecLevel(false);
				selector.setSelection(classpathContainer.getFrameworkInstance());
			}
			includeAnnotations = classpathContainer.isUseAnnotations();
		}
	}
	
	public void createControl(Composite parent) {
		setTitle("OSGi Framework");
		
		Composite composite = new Composite(parent, SWT.NONE);
		
		Group grpFramework = new Group(composite, SWT.NONE);
		grpFramework.setText("Installed Frameworks");
		selector.createControl(grpFramework);
		Control selectorControl = selector.getControl();
		
		Group grpExtras = new Group(composite, SWT.NONE);
		grpExtras.setText("Extra Compilation Libraries");
		final Button annotationsCheck = new Button(grpExtras, SWT.CHECK);
		annotationsCheck.setText("Include Bnd Annotations library");
		
		// Initialise
		annotationsCheck.setSelection(includeAnnotations);
		
		// Events
		selector.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				getContainer().updateButtons();
				getContainer().updateMessage();
			}
		});
		annotationsCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				includeAnnotations = annotationsCheck.getSelection();
			}
		});

		// Layout
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(1, false));
		
		GridData grpFrameworkLayoutData = new GridData(GridData.FILL_HORIZONTAL);
		grpFrameworkLayoutData.heightHint = 200;
		grpFramework.setLayoutData(grpFrameworkLayoutData);
		
		grpFramework.setLayout(new GridLayout(1, false));
		selectorControl.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		grpExtras.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grpExtras.setLayout(new GridLayout(1, false));
		
		setControl(composite);
	}
	
	@Override
	public String getErrorMessage() {
		return selector.getErrorMessage();
	}
}
