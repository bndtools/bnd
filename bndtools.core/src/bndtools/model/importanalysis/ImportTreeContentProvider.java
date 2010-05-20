package bndtools.model.importanalysis;

import java.util.Collection;

import org.eclipse.jface.viewers.Viewer;


/*
 * This inheritance is backwards! Technical debt here, need to refactor
 */
public class ImportTreeContentProvider extends ImportsExportsTreeContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
        Collection<?> collection = (Collection<?>) inputElement;
        return collection.toArray(new Object[collection.size()]);
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
}
