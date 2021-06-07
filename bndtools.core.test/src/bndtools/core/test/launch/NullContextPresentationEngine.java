package bndtools.core.test.launch;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.internal.workbench.swt.IEventLoopAdvisor;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;

/**
 * IPresentationEngine implementation that doesn't render anything or store any
 * state. All it does is spin the event loop. This engine is useful for testing
 * non-GUI parts of an Eclipse instance. If you need test GUI elements with a
 * headless display, something else will be necessary to store the state of the
 * rendered components. The skeleton of this engine has come from
 * {@link org.eclipse.e4.ui.internal.workbench.swt.PartRenderingEngine}.
 */
public class NullContextPresentationEngine implements IPresentationEngine {

	public static final String	EARLY_STARTUP_HOOK	= "runEarlyStartup";

	@Inject
	IEventBroker				eventBroker;

	@Override
	public Object createGui(MUIElement element, Object parentWidget, IEclipseContext parentContext) {
		return null;
	}

	@Override
	public Object createGui(MUIElement element) {
		return null;
	}

	@Override
	public void removeGui(MUIElement element) {}

	protected MApplication theApp;

	@Override
	public Object run(final MApplicationElement uiRoot, final IEclipseContext runContext) {
		final Display display;
		if (runContext.get(Display.class) != null) {
			display = runContext.get(Display.class);
		} else {
			display = Display.getDefault();
			runContext.set(Display.class, display);
		}
		if (!(uiRoot instanceof MApplication)) {
			throw new IllegalStateException("Should not be called with a uiRoot of this type: " + uiRoot.getClass());
		}
		theApp = (MApplication) uiRoot;
		IApplicationContext ac = theApp.getContext()
			.get(IApplicationContext.class);
		if (ac != null) {
			ac.applicationRunning();
			if (eventBroker != null) {
				eventBroker.post(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, theApp);
			}
		}
		// allow any early startup extensions to run
		Runnable earlyStartup = (Runnable) runContext.get(EARLY_STARTUP_HOOK);
		if (earlyStartup != null) {
			earlyStartup.run();
		}

		IEventLoopAdvisor advisor = runContext.getActiveLeaf()
			.get(IEventLoopAdvisor.class);
		if (advisor == null) {
			advisor = new IEventLoopAdvisor() {
				@Override
				public void eventLoopIdle(Display display) {
					display.sleep();
				}

				@Override
				public void eventLoopException(Throwable exception) {}
			};
		}
		final IEventLoopAdvisor finalAdvisor = advisor;
		display.setErrorHandler(e -> {
			// If e is one of the exception types that are generally
			// recoverable, hand it to the event loop advisor
			if (e instanceof LinkageError || e instanceof AssertionError) {
				handle(e, finalAdvisor);
			} else {
				// Otherwise, rethrow it
				throw e;
			}
		});
		display.setRuntimeExceptionHandler(e -> handle(e, finalAdvisor));
		// Spin the event loop until someone disposes the display
		while (theApp != null && !display.isDisposed()) {
			try {
				if (!display.readAndDispatch()) {
					runContext.processWaiting();
					advisor.eventLoopIdle(display);
				}
			} catch (ThreadDeath th) {
				throw th;
			} catch (Exception ex) {
				handle(ex, advisor);
			} catch (Error err) {
				handle(err, advisor);
			}
		}
		return IApplication.EXIT_OK;
	}

	private void handle(Throwable ex, IEventLoopAdvisor advisor) {
		try {
			advisor.eventLoopException(ex);
		} catch (Throwable t) {
			if (t instanceof ThreadDeath) {
				throw (ThreadDeath) t;
			}

			// couldn't handle the exception, print to console
			t.printStackTrace();
		}
	}

	@Override
	public void stop() {}

	@Override
	public void focusGui(MUIElement element) {}
}
