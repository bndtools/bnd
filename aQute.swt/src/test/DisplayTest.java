package test;

import junit.framework.*;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.*;

public class DisplayTest extends TestCase {
	BundleContext	context	= FrameworkUtil.getBundle(DisplayTest.class).getBundleContext();
	Shell			shell;

	public void testDisplay() throws Exception {
		ServiceTracker tracker = new ServiceTracker(context, Display.class.getName(), null);
		tracker.open();
		final Display display = (Display) tracker.waitForService(10000000);
		try {
			assertNotNull(display);
			display.syncExec(new Runnable() {
				public void run() {
					shell = new Shell(display);
					Text hello = new Text(shell, SWT.CENTER);
					hello.setText("hello");
					hello.pack();
					shell.pack();
					shell.open();
					
					assertTrue( shell.isVisible());
				}
			});
			Thread.sleep(10000);
		} finally {
			tracker.close();
		}
	}

}
