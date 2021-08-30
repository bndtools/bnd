package org.bndtools.api.editor;

import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.build.model.BndEditModel;

public interface IBndEditor {

	void doSave(IProgressMonitor monitor);

	/**
	 * Commit the active page to the edit model or if no active page, commit all
	 * pages
	 */
	void commitDirtyPages();

	BndEditModel getModel();

}
