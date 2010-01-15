/*******************************************************************************
 * Copyright (c) 2009 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.editor;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class OverviewFormPage extends FormPage {

	private BndEditModel bndModel;

	public OverviewFormPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	public OverviewFormPage(String id, String title) {
		super(id, title);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(bndModel);

		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		form.setText("General");
		toolkit.decorateFormHeading(form.getForm());
		form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter());

		// Create Controls
		Composite body = form.getBody();
		Composite pnlBasicColumn = toolkit.createComposite(body);
		
		GeneralInfoPart basicSection = new GeneralInfoPart(pnlBasicColumn, toolkit, Section.TITLE_BAR);
		managedForm.addPart(basicSection);
		
		BuildFormPart buildPart = new BuildFormPart(pnlBasicColumn, toolkit, Section.TITLE_BAR);
		managedForm.addPart(buildPart);
		
		Composite pnlPackagesColumn = toolkit.createComposite(body);
		ExportedPackagesPart exportedPackagesPart = new ExportedPackagesPart(pnlPackagesColumn, toolkit, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED | Section.DESCRIPTION);
		managedForm.addPart(exportedPackagesPart);
		
		PrivatePackagesPart privatePackagesPart = new PrivatePackagesPart(pnlPackagesColumn, toolkit, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED | Section.DESCRIPTION);
		managedForm.addPart(privatePackagesPart);
		
//		IncludedResourcesPart includedResourcesPart = new IncludedResourcesPart(body, toolkit, Section.TITLE_BAR | Section.TWISTIE | Section.DESCRIPTION);
//		includedResourcesPart.getSection().setExpanded(false);
//		managedForm.addPart(includedResourcesPart);

		// Layout
//		TableWrapLayout layout = new TableWrapLayout();
//		layout.bottomMargin = 10;
//		layout.topMargin = 5;
//		layout.leftMargin = 10;
//		layout.rightMargin = 10;
//		layout.numColumns = 2;
//		layout.horizontalSpacing = 10;
		GridLayout layout;
		GridData gd;
		
		layout = new GridLayout(2, false);
		body.setLayout(layout);
		
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		pnlBasicColumn.setLayoutData(gd);
		
		layout = new GridLayout(1, false);
		pnlBasicColumn.setLayout(layout);

		basicSection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		buildPart.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		pnlPackagesColumn.setLayoutData(gd);
		
		layout = new GridLayout(1, false);
		pnlPackagesColumn.setLayout(layout);
		privatePackagesPart.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		exportedPackagesPart.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	
	@Override
	public void initialize(FormEditor editor) {
		super.initialize(editor);
		bndModel = ((BndEditor) editor).getBndModel();
	}


}
