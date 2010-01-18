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
import name.neilbartlett.eclipse.bndtools.utils.MessageHyperlinkAdapter;

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

public class OverviewFormPage extends FormPage {

	private final BndEditModel model;

	public OverviewFormPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(model);

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
		
		BuildSectionPart buildPart = new BuildSectionPart(pnlBasicColumn, toolkit, Section.TITLE_BAR);
		managedForm.addPart(buildPart);
		
		Composite pnlPackagesColumn = toolkit.createComposite(body);
		
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
	}
}
