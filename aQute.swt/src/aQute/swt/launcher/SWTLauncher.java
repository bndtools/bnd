package aQute.swt.launcher;

import java.util.*;

import org.eclipse.swt.widgets.*;
import org.osgi.service.component.*;

import aQute.bnd.annotation.component.*;

/**
 * Register a Display class for use by SWT programs.
 * 
 * This component uses the facility from the launcher that it can run
 * 
 * @author aqute
 * 
 */
@Component(immediate = true) public class SWTLauncher {
	volatile Display	display;
	volatile boolean	alive	= true;

	protected void activate(final ComponentContext context) {
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put("main.thread", "true");
		context.getBundleContext().registerService(Runnable.class.getName(), new Runnable() {
			public void run() {
				display = Display.getDefault();
				context.getBundleContext().registerService(Display.class.getName(), display, null);
				alive = true;
				try {
					while (alive) {
						if (!display.readAndDispatch())
							display.sleep();
					}
				} finally {
					display.dispose();
				}
			}
		}, properties);
	}

	protected void deactivate(ComponentContext context) {
		alive = false;
		display.wake();
	}
}
