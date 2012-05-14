package bndtools.model.obr;

import org.apache.felix.bundlerepository.Resolver;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ResolutionFailureFlatContentProvider implements ITreeContentProvider {

    public Object[] getElements(Object input) {
        Resolver resolver = (Resolver) input;
        return resolver != null ? resolver.getUnsatisfiedRequirements() : new Object[0];
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getChildren(Object parentElem) {
        return null;
    }

    public Object getParent(Object elem) {
        return null;
    }

    public boolean hasChildren(Object elem) {
        return false;
    }

}
