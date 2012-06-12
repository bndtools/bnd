package org.bndtools.core.obr;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import bndtools.Plugin;
import bndtools.api.EE;
import bndtools.api.IBndModel;

public class ObrResolutionJob extends Job {

    private final IFile runFile;
    private final IBndModel model;

    private ObrResolutionResult result;

    public ObrResolutionJob(IFile runFile, IBndModel model) {
        super(Messages.ObrResolutionJob_jobName);
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
        ResolveOperation operation = new ResolveOperation(runFile, model);
        operation.run(monitor);
        result = operation.getResult();

        return Status.OK_STATUS;
    }

    public ObrResolutionResult getResolutionResult() {
        return result;
    }

}
