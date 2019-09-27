package org.bndtools.utils.swt;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class SWTConcurrencyUtil {

	public static void execForControl(Control control, boolean async, Runnable op) {
		if (control != null && !control.isDisposed())
			execForDisplay(control.getDisplay(), async, op);
	}

	public static void execForDisplay(Display display, boolean async, Runnable op) {
		if (display != null && !display.isDisposed()) {
			if (display.getThread() == Thread.currentThread()) {
				op.run();
			} else {
				if (async)
					display.asyncExec(op);
				else
					display.syncExec(op);
			}
		}
	}

}
