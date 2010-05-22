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
package bndtools.editor.pages;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.imports.VersionPolicyPart;
import bndtools.editor.model.BndEditModel;
import bndtools.utils.MessageHyperlinkAdapter;

public class PoliciesPage extends FormPage {

	private final BndEditModel model;

	public PoliciesPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		managedForm.setInput(model);

		ScrolledForm scrolledForm = managedForm.getForm();
		scrolledForm.setText("Import Patterns");

		Form form = scrolledForm.getForm();
		toolkit.decorateFormHeading(form);
		form.addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

		Composite body = form.getBody();

		VersionPolicyPart versionPolicyPart = new VersionPolicyPart(body, toolkit, Section.EXPANDED | Section.TITLE_BAR);
		managedForm.addPart(versionPolicyPart);

		GridLayout layout;
		GridData gd;

		layout = new GridLayout(2, true);
		body.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		versionPolicyPart.getSection().setLayoutData(gd);
	}

}
