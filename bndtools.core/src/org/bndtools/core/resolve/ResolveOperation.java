package org.bndtools.core.resolve;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.model.BndEditModel;
import biz.aQute.resolve.ResolveProcess;
import bndtools.Central;
import bndtools.Plugin;

public class ResolveOperation implements IRunnableWithProgress {

    private final BndEditModel model;
    private final Resolver resolver;

    private ResolutionResult result;

    public ResolveOperation(BndEditModel model, Resolver resolver) {
        this.model = model;
        this.resolver = resolver;
    }

    public void run(IProgressMonitor monitor) {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorOverview, null);

        ResolveProcess resolve = new ResolveProcess();
        try {
            boolean resolved = resolve.resolve(model, Central.getWorkspace(), resolver);
            if (resolved) {
                result = new ResolutionResult(ResolutionResult.Outcome.Resolved, resolve, status);
            } else {
                ResolutionException exception = resolve.getResolutionException();
                if (exception != null)
                    status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, exception.getLocalizedMessage(), exception));
                else
                    status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Resolution failed, reason unknown", null));

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
