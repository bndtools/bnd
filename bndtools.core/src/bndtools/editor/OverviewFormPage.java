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
package bndtools.editor;


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

import bndtools.editor.model.BndEditModel;
import bndtools.utils.MessageHyperlinkAdapter;

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
		final Composite pnlBasicColumn = toolkit.createComposite(body);
		
		GeneralInfoPart basicSection = new GeneralInfoPart(pnlBasicColumn, toolkit, Section.TITLE_BAR);
		managedForm.addPart(basicSection);
		
		ClassPathPart classPathPart = new ClassPathPart(pnlBasicColumn, toolkit, Section.TITLE_BAR | Section.TWISTIE | Section.DESCRIPTION);
		managedForm.addPart(classPathPart);
		
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
		
		gd = new GridData(SWT.FILL, SWT.FILL, false, true);
		pnlBasicColumn.setLayoutData(gd);
		
		layout = new GridLayout(1, false);
		pnlBasicColumn.setLayout(layout);

		basicSection.getSection().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		classPathPart.getSection().setLayoutData(gd);
		
		buildPart.getSection().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		gd = new GridData(SWT.FILL, SWT.FILL, false, true);
		pnlPackagesColumn.setLayoutData(gd);
		
		layout = new GridLayout(1, false);
		pnlPackagesColumn.setLayout(layout);
		privatePackagesPart.getSection().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}
}
