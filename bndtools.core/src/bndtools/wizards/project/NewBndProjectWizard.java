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
package bndtools.wizards.project;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

import bndtools.api.IProjectTemplate;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.BndProject;

class NewBndProjectWizard extends AbstractNewBndProjectWizard {

    private final TemplateSelectionWizardPage templatePage = new TemplateSelectionWizardPage();

    NewBndProjectWizard(NewBndProjectWizardPageOne pageOne, NewJavaProjectWizardPageTwo pageTwo) {
        super(pageOne, pageTwo);
    }

    @Override
    public void addPages() {
        addPage(pageOne);
        addPage(templatePage);
        addPage(pageTwo);
    }

    /**
     * Generate the new Bnd model for the project. This implementation simply
     * returns an empty Bnd model.
     *
     * @param monitor
     */
    @Override
    protected BndEditModel generateBndModel(IProgressMonitor monitor) {
        BndEditModel model = super.generateBndModel(monitor);

        IProjectTemplate template = templatePage.getTemplate();
        if (template != null) {
            template.modifyInitialBndModel(model);
        }

        return model;
    }

    /**
     * Allows for an IProjectTemplate to modify the new Bnd project
     *
     * @param monitor
     */
    @Override
    protected BndProject generateBndProject(IProgressMonitor monitor) {
        BndProject proj = super.generateBndProject(monitor);

        IProjectTemplate template = templatePage.getTemplate();
        if (template != null) {
            template.modifyInitialBndProject(proj);
        }

        return proj;
    }

}
