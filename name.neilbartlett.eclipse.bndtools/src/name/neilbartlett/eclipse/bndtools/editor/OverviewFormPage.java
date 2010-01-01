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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
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

		// Create Controls
		Composite body = form.getBody();
		GeneralInfoPart bundleDetailsSection = new GeneralInfoPart(body, toolkit);
		managedForm.addPart(bundleDetailsSection);
		ExportedPackagesPart exportedPackagesPart = new ExportedPackagesPart(body, toolkit);
		managedForm.addPart(exportedPackagesPart);
		PrivatePackagesPart privatePackagesPart = new PrivatePackagesPart(body, toolkit);
		managedForm.addPart(privatePackagesPart);

		// Layout
		body.setLayoutData(new TableWrapData(TableWrapData.FILL));

		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = 10;
		layout.topMargin = 5;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.numColumns = 2;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);
		
		bundleDetailsSection.getSection().setLayoutData(new TableWrapData(SWT.FILL, SWT.TOP, 2, 1));
	}
	
	@Override
	public void initialize(FormEditor editor) {
		super.initialize(editor);
		bndModel = ((BndEditor) editor).getBndModel();
	}


}
