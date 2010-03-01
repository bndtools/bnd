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
package name.neilbartlett.eclipse.bndtools.editor.exports;

import name.neilbartlett.eclipse.bndtools.editor.model.ExportedPackage;
import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import name.neilbartlett.eclipse.bndtools.editor.pkgpatterns.AnalyseToolbarAction;
import name.neilbartlett.eclipse.bndtools.editor.pkgpatterns.PkgPatternsDetailsPage;

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

public class ExportPatternsBlock extends MasterDetailsBlock {

	private ExportPatternsListPart listPart;

	@Override
	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();
		
		Composite container = toolkit.createComposite(parent);
		
		listPart = new ExportPatternsListPart(container, toolkit, Section.TITLE_BAR | Section.EXPANDED);
		managedForm.addPart(listPart);
		
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setLayout(new GridLayout(1, false));
		listPart.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
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
		PkgPatternsDetailsPage page = new PkgPatternsDetailsPage(listPart, "Export Pattern Details");
		detailsPart.registerPage(ExportedPackage.class, page);
	}
	void setSelectedExport(ExportedPackage export) {
		listPart.setSelectedClause(export);
	}
}
