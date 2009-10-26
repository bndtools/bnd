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
package name.neilbartlett.eclipse.jareditor.internal;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JARContentPage extends FormPage implements IFormPart {
	
	private JARContentMasterDetailsBlock contentMasterDetails = new JARContentMasterDetailsBlock();
	private Image titleImg;
	
	public JARContentPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.decorateFormHeading(form.getForm());

		form.setText("JAR File Viewer");
		titleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Constants.PLUGIN_ID, "/icons/jar_obj.gif").createImage(form.getDisplay());
		form.setImage(titleImg);
		
		contentMasterDetails.createContent(managedForm);
	}

	public void commit(boolean onSave) {
	}

	public void initialize(IManagedForm form) {
	}

	public boolean isStale() {
		return false;
	}

	public void refresh() {
		
	}

	public boolean setFormInput(Object input) {
		contentMasterDetails.setMasterPartInput(input);
		return false;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		titleImg.dispose();
	}
}
