package org.bndtools.core.resolve;

import org.apache.felix.resolver.ResolverImpl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import bndtools.Plugin;

public class ResolveJob extends Job {

    private final BndEditModel model;

    private ResolutionResult result;

    public ResolveJob(BndEditModel model) {
        super("Resolving...");
        this.model = model;
    }

    public IStatus validateBeforeRun() {
        String runfw = model.getRunFw();
        if (runfw == null)
            return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ResolutionJob_errorFrameworkOrExecutionEnvironmentUnspecified, null);

        EE ee = model.getEE();
        if (ee == null)
            return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ResolutionJob_errorFrameworkOrExecutionEnvironmentUnspecified, null);

        return Status.OK_STATUS;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        ResolverImpl felixResolver = new ResolverImpl(new org.apache.felix.resolver.Logger(4));
        ResolveOperation operation = new ResolveOperation(model, felixResolver);
        operation.run(monitor);
        result = operation.getResult();

        return Status.OK_STATUS;
    }

    public ResolutionResult getResolutionResult() {
        return result;
    }

}
