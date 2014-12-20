package bndtools.model.resolution;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;

public class CapReqMapContentProvider implements ITreeContentProvider {

    private static final Object[] EMPTY = new Object[0];

    @Override
    public void dispose() {}

    @Override
    public void inputChanged(Viewer viewer, Object oldValue, Object newValue) {}

    @Override
    public Object[] getElements(Object input) {
        List<Object> result = new LinkedList<Object>();

        @SuppressWarnings("unchecked")
        Map<String,List<Object>> originalMap = (Map<String,List<Object>>) input;

        // Take a copy so we can order entries explicitly
        HashMap<String,List<Object>> map = new HashMap<String,List<Object>>(originalMap);
        append(result, map.remove(BundleNamespace.BUNDLE_NAMESPACE));
        append(result, map.remove(IdentityNamespace.IDENTITY_NAMESPACE));
        append(result, map.remove(HostNamespace.HOST_NAMESPACE));
        append(result, map.remove(PackageNamespace.PACKAGE_NAMESPACE));

        // Now the rest in any order
        for (Entry<String,List<Object>> entry : map.entrySet())
            append(result, entry.getValue());

        return result.toArray(new Object[result.size()]);
    }

    private static void append(List<Object> mainList, List< ? > newEntries) {
        assert mainList != null;
        if (newEntries != null)
            mainList.addAll(newEntries);
    }

    @Override
    public Object getParent(Object object) {
        return null;
    }

    @Override
    public boolean hasChildren(Object object) {
        boolean children = false;

        if (object instanceof RequirementWrapper) {
            RequirementWrapper rw = (RequirementWrapper) object;
            children = rw.requirers != null && !rw.requirers.isEmpty();
        }

        return children;
    }

    @Override
    public Object[] getChildren(Object parent) {
        Object[] result = EMPTY;
        if (parent instanceof RequirementWrapper) {
            Collection< ? extends Object> requirers = ((RequirementWrapper) parent).requirers;
            if (requirers != null)
                result = requirers.toArray(new Object[requirers.size()]);
        }
        return result;
    }

}
