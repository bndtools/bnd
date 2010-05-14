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


import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.ExportedPackage;
import bndtools.utils.MessageHyperlinkAdapter;

public class BundleContentPage extends FormPage {

	private final BundleContentBlock block = new BundleContentBlock();
	private final BndEditModel model;

	public BundleContentPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		managedForm.setInput(model);

		ScrolledForm scrolledForm = managedForm.getForm();
		scrolledForm.setText("Bundle Content");

		Form form = scrolledForm.getForm();
		toolkit.decorateFormHeading(form);
		form.addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

		block.createContent(managedForm);
	}

	public void setSelectedExport(ExportedPackage export) {
		block.setSelectedExport(export);
	}
	public void setSelectedPrivatePkg(String pkg) {
	    block.setSelectedPrivatePkg(pkg);
	}
}
