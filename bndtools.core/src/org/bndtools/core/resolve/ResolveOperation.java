package org.bndtools.core.resolve;

import java.util.Collections;
import java.util.List;

import org.bndtools.core.resolve.ResolutionResult.Outcome;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.resolver.ResolutionException;

import aQute.bnd.build.model.BndEditModel;
import aQute.lib.exceptions.RunnableWithException;
import biz.aQute.resolve.ResolutionCallback;
import biz.aQute.resolve.RunResolution;
import bndtools.Plugin;

public class ResolveOperation implements IRunnableWithProgress {

	private final BndEditModel				model;
	private final List<ResolutionCallback>	callbacks;

	private ResolutionResult				result;

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
				RunResolution resolution = RunResolution.resolve(model.getProject(), model.getProperties(), callbacks);
				if (resolution.isOK()) {
					result = new ResolutionResult(Outcome.Resolved, resolution, status);
				} else if (resolution.exception instanceof ResolveCancelledException) {
					result = new ResolutionResult(Outcome.Cancelled, resolution, status);
				} else {
					resolution.reportException();
					if (resolution.exception instanceof ResolutionException) {
						status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
							resolution.exception.getLocalizedMessage(), resolution.exception));
						result = new ResolutionResult(Outcome.Unresolved, resolution, status);
					} else {
						throw resolution.exception;
					}
				}
			});

		} catch (Exception e) {
			status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Exception during resolution.", e));
			result = new ResolutionResult(Outcome.Error, null, status);
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
			Coordination coordination = coordinator.begin(ResolveOperation.class.getName(), 0);
			try {
				inCoordination.run();
				coordination.end();
			} catch (Exception e1) {
				coordination.fail(e1);
				throw e1;
			} finally {
				bc.ungetService(coordSvcRef);
			}
		}
	}

}
