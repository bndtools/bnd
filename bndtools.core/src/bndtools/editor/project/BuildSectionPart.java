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
package bndtools.editor.project;


import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.MakeBundleWithRefreshAction;
import bndtools.Plugin;
import bndtools.utils.EditorUtils;

public class BuildSectionPart extends SectionPart {

	private MakeBundleWithRefreshAction buildBundleAction;
	private Image jarImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/jar_obj.gif").createImage();

	public BuildSectionPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);

		buildBundleAction = new MakeBundleWithRefreshAction();
		createSection(getSection(), toolkit);
	}

	void createSection(final Section section, FormToolkit toolkit) {
		section.setText("Build");

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		ImageHyperlink lnkBuild = toolkit.createImageHyperlink(composite, SWT.LEFT);
		lnkBuild.setText("Build the Bundle");
		lnkBuild.setImage(jarImg);

		lnkBuild.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				IFormPage page = (IFormPage) getManagedForm().getContainer();
				FormEditor editor = page.getEditor();
				if(EditorUtils.saveEditorIfDirty(editor, "Build Bundle", "The editor content must be saved before building.")) {
					buildBundleAction.run(null);
				} else {
					MessageDialog.openError(section.getShell(), "Build Bundle", "Bundle not built due to error during save.");
				}
			}
		});

		composite.setLayout(new GridLayout(1, false));
	}
	FormEditor getEditor() {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		return page.getEditor();
	}
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		IFormPage page = (IFormPage) form.getContainer();
		buildBundleAction.setActiveEditor(null, page.getEditor());
	}
	@Override
	public void dispose() {
		super.dispose();
		jarImg.dispose();
	}
}
