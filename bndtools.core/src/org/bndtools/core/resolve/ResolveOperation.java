package org.bndtools.core.resolve;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bndtools.core.resolve.ResolutionResult.Outcome;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.resolver.ResolutionException;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.deployer.repository.ReporterLogService;
import biz.aQute.resolve.BndResolver;
import biz.aQute.resolve.ResolutionCallback;
import biz.aQute.resolve.ResolveProcess;
import biz.aQute.resolve.ResolverLogger;
import bndtools.Plugin;
import bndtools.central.Central;

public class ResolveOperation implements IRunnableWithProgress {

    private final BndEditModel model;
    private final List<ResolutionCallback> callbacks;

    private ResolutionResult result;

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

        // Start a coordination
        BundleContext bc = Plugin.getDefault().getBundleContext();
        ServiceReference<Coordinator> coordSvcRef = bc.getServiceReference(Coordinator.class);
        Coordinator coordinator = coordSvcRef != null ? (Coordinator) bc.getService(coordSvcRef) : null;
        Coordination coordination = coordinator != null ? coordinator.begin(ResolveOperation.class.getName(), 0) : null;

        // Begin resolve
        ResolveProcess resolve = new ResolveProcess();
        ResolverLogger logger = new ResolverLogger();
        try {
            BndResolver bndResolver = new BndResolver(logger);

            //
            // Make sure we do macro expansion properly
            //

            File resource = model.getBndResource();
            Project project = Central.getProject(resource.getParentFile());
            if (model.isProjectFile()) {
                //
                // run's in projects are based on the project
                // and implicitly the ws
                //
                model.setProject(project);
            } else {
                //
                // run's in bndrun files are the bndrun + workspace
                //
                Run run = new Run(project.getWorkspace(), project.getBase(), resource);
                model.setProject(run);
            }

            ReporterLogService log = new ReporterLogService(Central.getWorkspace());
            Map<Resource,List<Wire>> wirings = resolve.resolveRequired(model, Central.getWorkspace(), bndResolver, callbacks, log);
            result = new ResolutionResult(Outcome.Resolved, wirings, null, status, logger.getLog());
            if (coordination != null)
                coordination.end();
        } catch (ResolveCancelledException e) {
            result = new ResolutionResult(Outcome.Cancelled, null, null, status, logger.getLog());

            if (coordination != null)
                coordination.fail(e);
        } catch (ResolutionException e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e));
            result = new ResolutionResult(Outcome.Unresolved, null, e, status, logger.getLog());

            if (coordination != null)
                coordination.fail(e);
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Exception during resolution.", e));
            result = new ResolutionResult(Outcome.Error, null, null, status, logger.getLog());

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
