package org.bndtools.core.resolve.ui;

import java.util.Collection;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class UnresolvedRequirementsContentProvider implements ITreeContentProvider {

    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof Object[])
            return (Object[]) inputElement;

        if (inputElement instanceof Collection< ? >) {
            Collection< ? > coll = (Collection< ? >) inputElement;
            return coll.toArray(new Object[coll.size()]);
        }

        return null;
    }

    public void dispose() {
        // TODO Auto-generated method stub

    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // TODO Auto-generated method stub

    }

    public Object[] getChildren(Object parentElement) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getParent(Object element) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasChildren(Object element) {
        // TODO Auto-generated method stub
        return false;
    }

}
