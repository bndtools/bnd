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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;

import bndtools.Plugin;
import bndtools.editor.completion.BndSourceViewerConfiguration;

public class BndSourceEditorPage extends TextEditor implements IFormPage {
    
    private final Image icon;

	private final BndEditor formEditor;
	private final String id;
	private String lastLoaded;

	private int index;

    private final PropertyChangeListener propChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            refresh();
            lastLoaded = getDocument().get();
        }
    };

	private Control control;

	public BndSourceEditorPage(String id, BndEditor formEditor) {
		this.id = id;
		this.formEditor = formEditor;
		setSourceViewerConfiguration(new BndSourceViewerConfiguration(getSharedColors()));

		formEditor.getBndModel().addPropertyChangeListener(propChangeListener);
		ImageDescriptor iconDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/page_white_text.png");
        icon = iconDescriptor.createImage();
	}

	@Override
	public void dispose() {
		this.formEditor.getBndModel().removePropertyChangeListener(propChangeListener);
		super.dispose();
		icon.dispose();
	}

	public boolean canLeaveThePage() {
		return true;
	}

	public FormEditor getEditor() {
		return formEditor;
	}

	public String getId() {
		return id;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public IManagedForm getManagedForm() {
		return null;
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		Control[] children = parent.getChildren();
		control = children[children.length - 1];
	}

	public Control getPartControl() {
		return control;
	}

	public void initialize(FormEditor formEditor) {
	}

	public boolean isActive() {
		return this.equals(formEditor.getActivePageInstance());
	}

	public boolean isEditor() {
		return true;
	}

	public boolean selectReveal(Object object) {
		if (object instanceof IMarker) {
			IDE.gotoMarker(this, (IMarker) object);
			return true;
		}
		return false;
	}

	public void setActive(boolean active) {
		if(!active) {
			commit(false);
		}
	}

	void commit(boolean onSave) {
		try {
			// Only commit changes to the model if the document text has
			// actually changed since we switched to the page; this prevents us
			// losing selection in the Components and Imports tabs.
			// We can't use the dirty flag for this because "undo" will clear
			// the dirty flag.
			IDocument doc = getDocument();
			String currentContent = doc.get();
			if(!currentContent.equals(lastLoaded))
				formEditor.getBndModel().loadFrom(getDocument());
		} catch (IOException e) {
			Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading model from document.", e));
		}
	}

	void refresh() {
		IDocument document = getDocument();
		formEditor.getBndModel().saveChangesTo(document);
	}

	private IDocument getDocument() {
		IDocumentProvider docProvider = getDocumentProvider();
		IEditorInput input = getEditorInput();
		IDocument doc = docProvider.getDocument(input);
		return doc;
	}
	
	@Override
	public Image getTitleImage() {
	    return icon;
	}
}
