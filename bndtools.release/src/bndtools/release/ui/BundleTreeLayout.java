package bndtools.release.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

public class BundleTreeLayout extends Layout {

	Point sashExtent, buttonExtent; // the cached sizes

	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean changed) {
		Control [] children = composite.getChildren();
	     if (changed || sashExtent == null || buttonExtent == null) {
	    	 sashExtent = children[0].computeSize(SWT.DEFAULT, calculateSashHeight(composite, children[0], hHint), false);
	    	 buttonExtent = children[1].computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
	     }

	     int height = sashExtent.y + 10 + buttonExtent.y;
	     int width = Math.max(sashExtent.x,  buttonExtent.x);

	     return new Point(width + 2, height + 2);
	}

	@Override
	protected void layout(Composite composite, boolean changed) {
		 Control [] children = composite.getChildren();
	     if (changed || sashExtent == null || buttonExtent == null) {
	    	 sashExtent = children[0].computeSize(SWT.DEFAULT, calculateSashHeight(composite, children[0], SWT.DEFAULT), false);
	         buttonExtent = children[1].computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
	     }
	     children[0].setBounds(1, 1, sashExtent.x, sashExtent.y);
	     children[1].setBounds(1, sashExtent.y + 10, buttonExtent.x, buttonExtent.y);
	}
	
	private int calculateSashHeight(Composite parent, Control sashForm, int hHint) {
		if (hHint > 0) {
			return hHint;
		}
		return Math.min(getNextParentSize(parent), sashForm.computeSize(SWT.DEFAULT, SWT.DEFAULT, false).x);
	}
	
	private int getNextParentSize(Composite parent) {
		if (parent == null) {
			return 600;
		}
		int parentSize = parent.getSize().x;
		while (parentSize == 0) {
			parentSize = getNextParentSize(parent.getParent());
		}
		return parentSize;
	}
}
