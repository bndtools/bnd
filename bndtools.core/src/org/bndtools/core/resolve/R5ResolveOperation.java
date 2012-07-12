package org.bndtools.core.resolve;

import org.bndtools.core.obr.Messages;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.model.BndEditModel;
import biz.aQute.resolve.ResolveProcess;
import bndtools.Central;
import bndtools.Plugin;

public class R5ResolveOperation implements IRunnableWithProgress {

    private final IFile runFile;
    private final BndEditModel model;
    private final Resolver resolver;

    private ResolutionResult result;

    public R5ResolveOperation(IFile runFile, BndEditModel model, Resolver resolver) {
        this.runFile = runFile;
        this.model = model;
        this.resolver = resolver;
    }

    public void run(IProgressMonitor monitor) {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorOverview, null);
        SubMonitor progress = SubMonitor.convert(monitor, Messages.ResolveOperation_progressLabel, 0);

        ResolveProcess resolve = new ResolveProcess();
        try {
            boolean resolved = resolve.resolve(model, Central.getWorkspace(), resolver);
            if (resolved) {
                result = new ResolutionResult(ResolutionResult.Outcome.Resolved, resolve, status);
            } else {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Resolution failed.", null));
                result = new ResolutionResult(ResolutionResult.Outcome.Unresolved, resolve, status);
            }
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Exception during resolution.", e));
            result = new ResolutionResult(ResolutionResult.Outcome.Error, resolve, status);
        }
    }

    public ResolutionResult getResult() {
        return result;
    }

}
