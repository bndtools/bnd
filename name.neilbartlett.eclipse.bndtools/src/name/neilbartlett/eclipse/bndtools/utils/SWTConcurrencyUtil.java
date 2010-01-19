package name.neilbartlett.eclipse.bndtools.utils;

import org.eclipse.swt.widgets.Display;

public class SWTConcurrencyUtil {

	public static void execForDisplay(Display display, Runnable op) {
		if(display != null && !display.isDisposed()) {
			if(display.getThread() == Thread.currentThread()) {
				op.run();
			} else {
				display.asyncExec(op);
			}
		}
	}

}
