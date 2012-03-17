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


import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.api.IBndModel;
import bndtools.editor.components.ComponentsBlock;
import bndtools.model.clauses.ServiceComponent;
import bndtools.utils.MessageHyperlinkAdapter;

public class ComponentsPage extends FormPage {

    private final Image componentsImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/component.gif").createImage();
	private final ComponentsBlock block = new ComponentsBlock();
	private final IBndModel model;

    public static final IFormPageFactory FACTORY = new IFormPageFactory() {
        public IFormPage createPage(ExtendedFormEditor editor, IBndModel model, String id) throws IllegalArgumentException {
            return new ComponentsPage(editor, model, id, "Components");
        }

        public boolean supportsMode(Mode mode) {
            return mode == Mode.bundle;
        }
    };

	public ComponentsPage(FormEditor editor, IBndModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(model);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.decorateFormHeading(form.getForm());
		form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

		form.setText("Components");
		block.createContent(managedForm);
	}

	public void setSelectedComponent(ServiceComponent component) {
		block.setSelectedComponent(component);
	}

	@Override
	public void dispose() {
	    super.dispose();
	    componentsImg.dispose();
	};
}
