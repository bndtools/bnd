package org.bndtools.utils.jface;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class StatusTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		IStatus rootStatus = (IStatus) inputElement;
		if (rootStatus.isMultiStatus()) {
			return rootStatus.getChildren();
		}
		return new Object[] {
			rootStatus
		};
	}

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	@Override
	public Object[] getChildren(Object parentElement) {
		IStatus status = (IStatus) parentElement;
		return status.getChildren();
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof IStatus && ((IStatus) element).isMultiStatus();
	}

}
