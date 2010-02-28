package name.neilbartlett.eclipse.bndtools.editor.components;

import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class MapContentProvider implements IStructuredContentProvider {

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object inputElement) {
		Map<?, ?> map = (Map<?, ?>) inputElement;
		Set<?> keySet = map.keySet();
		
		return (Object[]) keySet.toArray(new Object[keySet.size()]);
	}
}
