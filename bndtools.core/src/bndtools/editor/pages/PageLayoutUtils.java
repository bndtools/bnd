package bndtools.editor.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

public final class PageLayoutUtils {

	static GridData createExpanded() {
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 50;
		gd.heightHint = 50;
		return gd;
	}

	static GridData createCollapsed() {
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		return gd;
	}

}
