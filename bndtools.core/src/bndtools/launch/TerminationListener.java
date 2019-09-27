package bndtools.launch;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;

/**
 * Utility for executing code after the termination of all the processes created
 * by a launch. It should be created and registered with
 * {@link DebugPlugin#addDebugEventListener(IDebugEventSetListener)}. On
 * termination of all processes, the {@link Runnable} supplied in the
 * constructor will be invoked, and then this listener will unregister itself
 * from the {@link DebugPlugin}. It must not be reused for another launch.
 *
 * @author Neil Bartlett
 */
public class TerminationListener implements IDebugEventSetListener {

	private final ILaunch		launch;
	private final Runnable		onTerminate;

	private final Set<IProcess>	terminatedProcesses	= new HashSet<>();

	public TerminationListener(ILaunch launch, Runnable onTerminate) {
		this.launch = launch;
		this.onTerminate = onTerminate;
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			Object source = event.getSource();
			if (event.getKind() == DebugEvent.TERMINATE && (source instanceof IProcess)
				&& ((IProcess) source).getLaunch() == launch) {
				terminatedProcesses.add((IProcess) source);

				boolean isTerminated = true;
				IProcess[] createdProcesses = launch.getProcesses();
				for (IProcess process : createdProcesses) {
					if (!terminatedProcesses.contains(process)) {
						isTerminated = false;
						break;
					}
				}

				if (isTerminated) {
					DebugPlugin.getDefault()
						.removeDebugEventListener(this);
					onTerminate.run();
				}
			}
		}
	}
}
