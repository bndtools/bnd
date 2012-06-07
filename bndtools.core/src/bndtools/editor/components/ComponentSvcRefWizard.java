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
package bndtools.editor.components;

import java.util.Set;


import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.Wizard;

import bndtools.model.clauses.ComponentSvcReference;

public class ComponentSvcRefWizard extends Wizard {

	private final ComponentSvcRefWizardPage mainPage;
	
	private final ComponentSvcReference serviceRef;
	
	// Store a copy of the unedited object to restore from in case of cancellation.
	private final ComponentSvcReference pristine;

	public ComponentSvcRefWizard(Set<String> existingNames, IJavaProject javaProject, String componentClassName) {
		this(new ComponentSvcReference(), existingNames, javaProject, componentClassName);
	}
	
	public ComponentSvcRefWizard(ComponentSvcReference serviceRef, Set<String> existingNames, IJavaProject javaProject, String componentClassName) {
		this.serviceRef = serviceRef;
		
		this.pristine = serviceRef.clone();
		this.mainPage = new ComponentSvcRefWizardPage(serviceRef, "main", existingNames, javaProject, componentClassName);
	}
	
	@Override
	public void addPages() {
		addPage(mainPage);
	}
	
	@Override
	public boolean performCancel() {
		serviceRef.copyFrom(pristine);
		return true;
	}

	@Override
	public boolean performFinish() {
		return true;
	}
	
	public ComponentSvcReference getResult() {
		return serviceRef;
	}
}
