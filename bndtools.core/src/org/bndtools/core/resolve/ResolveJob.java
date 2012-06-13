package org.bndtools.core.resolve;

import org.apache.felix.resolver.ResolverImpl;
import org.bndtools.core.obr.Messages;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import bndtools.Plugin;

public class ResolveJob extends Job {

    private final IFile runFile;
    private final BndEditModel model;

    private ResolutionResult result;

    public ResolveJob(IFile runFile, BndEditModel model) {
        super("Resolving...");
        this.runFile = runFile;
        this.model = model;
    }

    public IStatus validateBeforeRun() {
        String runFramework = model.getRunFramework();
        if (runFramework == null)
            return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ObrResolutionJob_errorFrameworkOrExecutionEnvironmentUnspecified, null);

        EE ee = model.getEE();
        if (ee == null)
            return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ObrResolutionJob_errorFrameworkOrExecutionEnvironmentUnspecified, null);

        return Status.OK_STATUS;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        ResolverImpl felixResolver = new ResolverImpl(new org.apache.felix.resolver.Logger(4));
        R5ResolveOperation operation = new R5ResolveOperation(runFile, model, felixResolver);
        operation.run(monitor);
        result = operation.getResult();

        return Status.OK_STATUS;
    }

    public ResolutionResult getResolutionResult() {
        return result;
    }

}
