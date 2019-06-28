package bndtools.model.resolution;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;

public class CapReqMapContentProvider implements ITreeContentProvider {

	private static final Object[]		EMPTY			= new Object[0];

	private static final String[]		NAMESPACE_ORDER	= new String[] {
		BundleNamespace.BUNDLE_NAMESPACE, IdentityNamespace.IDENTITY_NAMESPACE, HostNamespace.HOST_NAMESPACE,
		PackageNamespace.PACKAGE_NAMESPACE
	};

	private static final Set<String>	NAMESPACES;

	private final Comparator<Object>	comparator		= new CapReqComparator();

	static {
		NAMESPACES = new HashSet<>();
		for (String s : NAMESPACE_ORDER)
			NAMESPACES.add(s);
	}

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldValue, Object newValue) {}

	@Override
	public Object[] getElements(Object input) {
		List<Object[]> arrays = new LinkedList<>();

		@SuppressWarnings("unchecked")
		Map<String, List<Object>> map = (Map<String, List<Object>>) input;

		// Add entries for our preferred ordering of namespaces
		for (String namespace : NAMESPACE_ORDER) {
			List<Object> listForNs = map.get(namespace);
			if (listForNs != null) {
				Object[] array = listForNs.toArray();
				Arrays.sort(array, comparator);
				arrays.add(array);
			}
		}

		// Now the rest in any order
		for (Entry<String, List<Object>> entry : map.entrySet()) {
			// Skip if namespace is a member of the namespaces we have already
			// added.
			if (NAMESPACES.contains(entry.getKey()))
				continue;

			List<Object> listForNs = entry.getValue();
			Object[] array = listForNs.toArray();
			Arrays.sort(array, comparator);
			arrays.add(array);
		}

		return flatten(arrays);
	}

	private Object[] flatten(List<Object[]> arrays) {
		// Iterate over once to count the lengths
		int length = 0;
		for (Object[] array : arrays) {
			length += array.length;
		}
		Object[] result = new Object[length];

		// Iterate again to flatten out the arrays
		int position = 0;
		for (Object[] array : arrays) {
			System.arraycopy(array, 0, result, position, array.length);
			position += array.length;
		}
		return result;
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
			Collection<? extends Object> requirers = ((RequirementWrapper) parent).requirers;
			if (requirers != null)
				result = requirers.toArray();
		}
		return result;
	}

}
