package org.bndtools.utils.jface;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class StatusTreeContentProvider implements ITreeContentProvider {

    public Object[] getElements(Object inputElement) {
        IStatus rootStatus = (IStatus) inputElement;
        if (rootStatus.isMultiStatus()) {
            return rootStatus.getChildren();
        }
        return new Object[] {
            rootStatus
        };
    }

    public void dispose() {}

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

    public Object[] getChildren(Object parentElement) {
        IStatus status = (IStatus) parentElement;
        return status.getChildren();
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object element) {
        return element instanceof IStatus && ((IStatus) element).isMultiStatus();
    }

}
