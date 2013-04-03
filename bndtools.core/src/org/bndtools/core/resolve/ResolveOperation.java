package org.bndtools.core.resolve;

import org.apache.felix.resolver.ResolverImpl;
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
import aQute.bnd.deployer.repository.ReporterLogService;
import biz.aQute.resolve.ResolveProcess;
import bndtools.Central;
import bndtools.Plugin;

public class ResolveOperation implements IRunnableWithProgress {

    private final BndEditModel model;

    private ResolutionResult result;

    public ResolveOperation(BndEditModel model) {
        this.model = model;
    }

    public void run(IProgressMonitor monitor) {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorOverview, null);

        // Start a coordination
        BundleContext bc = Plugin.getDefault().getBundleContext();
        ServiceReference coordSvcRef = bc.getServiceReference(Coordinator.class.getName());
        Coordinator coordinator = coordSvcRef != null ? (Coordinator) bc.getService(coordSvcRef) : null;
        Coordination coordination = coordinator != null ? coordinator.begin(ResolveOperation.class.getName(), 0) : null;

        // Begin resolve
        ResolveProcess resolve = new ResolveProcess();
        ResolverLogger logger = new ResolverLogger();
        try {
            ResolverImpl felixResolver = new ResolverImpl(logger);

            ReporterLogService log = new ReporterLogService(Central.getWorkspace());
            boolean resolved = resolve.resolve(model, Central.getWorkspace(), felixResolver, log);
            if (resolved) {
                result = new ResolutionResult(ResolutionResult.Outcome.Resolved, resolve, status, logger.getLog());
            } else {
                ResolutionException exception = resolve.getResolutionException();
                if (exception != null)
                    status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, exception.getLocalizedMessage(), exception));
                else
                    status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Resolution failed, reason unknown", null));

                result = new ResolutionResult(ResolutionResult.Outcome.Unresolved, resolve, status, logger.getLog());
            }

            if (coordination != null)
                coordination.end();
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Exception during resolution.", e));
            result = new ResolutionResult(ResolutionResult.Outcome.Error, resolve, status, logger.getLog());

            if (coordination != null)
                coordination.fail(e);
        } finally {
            if (coordinator != null)
                bc.ungetService(coordSvcRef);
        }
    }

    public ResolutionResult getResult() {
        return result;
    }

}
