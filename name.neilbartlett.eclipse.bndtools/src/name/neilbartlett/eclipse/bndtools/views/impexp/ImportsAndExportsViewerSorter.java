package name.neilbartlett.eclipse.bndtools.views.impexp;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class ImportsAndExportsViewerSorter extends ViewerSorter {
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if(e1 instanceof String) {
			String s1 = (String) e1;
			if(e2 instanceof String) {
				String s2 = (String) e2;
				// Yes this is backwards! We want "Import Packages" before "Export Packages"
				return s2.compareTo(s1);
			}
			return 0;
		}
		
		@SuppressWarnings("unchecked")
		HeaderClause clause1 = (HeaderClause) e1;
		if(e2 instanceof String) {
			return 0;
		}
		
		@SuppressWarnings("unchecked")
		HeaderClause clause2 = (HeaderClause) e2;
		return clause1.getName().compareTo(clause2.getName());
	}
}
