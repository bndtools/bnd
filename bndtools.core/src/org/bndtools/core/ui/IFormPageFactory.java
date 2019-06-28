package org.bndtools.core.ui;

import org.eclipse.ui.forms.editor.IFormPage;

import aQute.bnd.build.model.BndEditModel;

public interface IFormPageFactory {

	public enum Mode {
		project,
		bndrun,
		bundle,
		workspace
	}

	IFormPage createPage(ExtendedFormEditor editor, BndEditModel model, String id) throws IllegalArgumentException;

	boolean supportsMode(Mode mode);
}
