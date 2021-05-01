package org.bndtools.core.resolve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bndtools.core.resolve.ResolutionResult.Outcome;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.resolver.ResolutionException;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exceptions.RunnableWithException;
import biz.aQute.resolve.ResolutionCallback;
import biz.aQute.resolve.ResolverLogger;
import biz.aQute.resolve.RunResolution;
import bndtools.Plugin;

public class ResolveOperation implements IRunnableWithProgress {

	private final BndEditModel				model;
	private final List<ResolutionCallback>	callbacks;

	private ResolutionResult				result;
	private ResolverLogger					logger;

	public ResolveOperation(BndEditModel model) {
		this(model, Collections.<ResolutionCallback> emptyList());
	}

	public ResolveOperation(BndEditModel model, List<ResolutionCallback> callbacks) {
		this.model = model;
		this.callbacks = callbacks;
	}

	@Override
	public void run(IProgressMonitor monitor) {

		MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorOverview, null);

		try {
			coordinate(() -> {
				logger = new ResolverLogger();
				List<ResolutionCallback> operationCallbacks = new ArrayList<>(callbacks.size() + 1);
				operationCallbacks.addAll(callbacks);
				operationCallbacks.add(new ResolutionProgressCallback(monitor));

				RunResolution resolution = RunResolution.resolve(model.getProject(), model.getProperties(),
					operationCallbacks, logger);
				if (resolution.isOK()) {
					result = new ResolutionResult(Outcome.Resolved, resolution, status, logger);
				} else if (resolution.exception != null) {
					Throwable t = Exceptions.unrollCause(resolution.exception);
					if (resolution.exception instanceof OperationCanceledException
						|| t instanceof InterruptedException) {
						result = new ResolutionResult(Outcome.Cancelled, resolution, status, logger);
					} else {
						resolution.reportException();
						if (resolution.exception instanceof ResolutionException) {
							status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
								resolution.exception.getLocalizedMessage(), resolution.exception));
							result = new ResolutionResult(Outcome.Unresolved, resolution, status, logger);
						} else {
							throw resolution.exception;
						}
					}
				}
			});

		} catch (Exception e) {
			status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Exception during resolution.", e));
			result = new ResolutionResult(Outcome.Error, null, status, logger);
		}
	}

	public ResolutionResult getResult() {
		return result;
	}

	private void coordinate(RunnableWithException inCoordination) throws Exception {
		BundleContext bc = Plugin.getDefault()
			.getBundleContext();

		ServiceReference<Coordinator> coordSvcRef = bc.getServiceReference(Coordinator.class);
		Coordinator coordinator = coordSvcRef != null ? (Coordinator) bc.getService(coordSvcRef) : null;
		if (coordinator == null)
			inCoordination.run();
		else {
			try {
				Coordination coordination = coordinator.begin(ResolveOperation.class.getName(), 0);
				try {
					inCoordination.run();
				} catch (Exception e) {
					coordination.fail(e);
				} finally {
					coordination.end();
				}
			} finally {
				bc.ungetService(coordSvcRef);
			}
		}
	}

}
