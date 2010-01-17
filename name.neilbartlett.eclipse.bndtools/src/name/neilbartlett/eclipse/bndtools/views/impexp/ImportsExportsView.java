package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;

public class ImportsExportsView extends ViewPart {
	
	public static String VIEW_ID = "name.neilbartlett.eclipse.bndtools.impExpView";

	private Tree tree = null;
	private TreeViewer viewer;
	
	@Override
	public void createPartControl(Composite parent) {
		tree = new Tree(parent, SWT.FULL_SELECTION | SWT.MULTI);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		TreeColumn col;
		col = new TreeColumn(tree, SWT.NONE);
		col.setText("Package");
		col.setWidth(300);
		
		col = new TreeColumn(tree, SWT.NONE);
		col.setText("Version");
		col.setWidth(100);
		
		col = new TreeColumn(tree, SWT.NONE);
		col.setText("Attributes");
		col.setWidth(200);
		
		viewer = new TreeViewer(tree);
		viewer.setContentProvider(new ImportsExportsTreeContentProvider());
		viewer.setLabelProvider(new ImportsExportsTreeLabelProvider());
		viewer.setAutoExpandLevel(2);
	}

	@Override
	public void setFocus() {
	}
	
	public void setInput(Map<String, Map<String,String>> imports, Map<String, Map<String,String>> exports) {
		if(tree != null && !tree.isDisposed()) {
			viewer.setInput(new ImportsAndExports(imports, exports));
		}
	}
}
