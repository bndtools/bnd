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
package bndtools.editor.contents;


import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.GeneralInfoPart;
import bndtools.editor.PrivatePackagesPart;
import bndtools.editor.model.ExportedPackage;
import bndtools.editor.pkgpatterns.AnalyseToolbarAction;
import bndtools.editor.pkgpatterns.PkgPatternsDetailsPage;

public class BundleContentBlock extends MasterDetailsBlock {

	private ExportPatternsListPart exportsPart;
    private PrivatePackagesPart privatePkgPart;

	@Override
	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();

		Composite container = toolkit.createComposite(parent);

		GeneralInfoPart bundleInfoPart = new GeneralInfoPart(container, toolkit, Section.TITLE_BAR | Section.TWISTIE);
		managedForm.addPart(bundleInfoPart);

		privatePkgPart = new PrivatePackagesPart(container, toolkit, Section.TITLE_BAR | Section.EXPANDED);
		managedForm.addPart(privatePkgPart);

		exportsPart = new ExportPatternsListPart(container, toolkit, Section.TITLE_BAR | Section.EXPANDED);
		managedForm.addPart(exportsPart);

		// LAYOUT
		GridData gd;
		GridLayout layout;

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		container.setLayoutData(gd);

        layout = new GridLayout(1, false);
        container.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        bundleInfoPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        exportsPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        privatePkgPart.getSection().setLayoutData(gd);
	}

	@Override
	protected void createToolBarActions(final IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();

		AnalyseToolbarAction analyseAction = new AnalyseToolbarAction((IFormPage) managedForm.getContainer());
		analyseAction.setToolTipText("Analyse Imports/Exports");

		form.getToolBarManager().add(analyseAction);
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		PkgPatternsDetailsPage page = new PkgPatternsDetailsPage(exportsPart, "Export Pattern Details");
		detailsPart.registerPage(ExportedPackage.class, page);
	}

	void setSelectedExport(ExportedPackage export) {
		exportsPart.getSelectionProvider().setSelection(new StructuredSelection(export));
	}

    public void setSelectedPrivatePkg(String pkg) {
        privatePkgPart.getSelectionProvider().setSelection(new StructuredSelection(pkg));
    }
}
