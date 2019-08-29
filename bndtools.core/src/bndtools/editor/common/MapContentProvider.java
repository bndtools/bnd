package bndtools.editor.common;

import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class MapContentProvider implements IStructuredContentProvider {

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	@Override
	public Object[] getElements(Object inputElement) {
		Map<?, ?> map = (Map<?, ?>) inputElement;
		Set<?> keySet = map.keySet();

		return keySet.toArray();
	}
}
