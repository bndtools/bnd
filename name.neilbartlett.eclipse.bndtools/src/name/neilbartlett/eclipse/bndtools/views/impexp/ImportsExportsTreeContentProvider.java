package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ImportsExportsTreeContentProvider implements ITreeContentProvider {
	
	static final Object IMPORTS_PLACEHOLDER = new String("_imports_placeholder");
	static final Object EXPORTS_PLACEHOLDER = new String("_exports_placeholder");
	
	private ImportsAndExports importsAndExports = null;

	public Object[] getChildren(Object parentElement) {
		Map<String, Map<String, String>> result;
		if(parentElement == IMPORTS_PLACEHOLDER)
			result = importsAndExports.imports;
		else
			result = importsAndExports.exports;
		
		Set<Entry<String,Map<String,String>>> entrySet = result.entrySet();
		return entrySet.toArray(new Entry[entrySet.size()]);
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return element == IMPORTS_PLACEHOLDER || element == EXPORTS_PLACEHOLDER;
	}

	public Object[] getElements(Object inputElement) {
		return new Object[] { IMPORTS_PLACEHOLDER, EXPORTS_PLACEHOLDER };
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.importsAndExports = (ImportsAndExports) newInput;
	}
}
