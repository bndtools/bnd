package org.bndtools.core.resolve;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bndtools.core.obr.Messages;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.model.BndEditModel;
import biz.aQute.resolve.BndrunResolveContext;
import biz.aQute.resolve.ResolveResultProcessor;
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

        try {
            // Create the resolve context
            BndrunResolveContext resolveContext = new BndrunResolveContext(model, Central.getWorkspace());

            // Run resolution
            Map<Resource,List<Wire>> wirings = resolver.resolve(resolveContext);
            ResolveResultProcessor resultProcessor = new ResolveResultProcessor(resolveContext, wirings);
            resultProcessor.getMandatory();

            result = new ResolutionResult(ResolutionResult.Outcome.Resolved, resultProcessor, resultProcessor.getMandatory(), Collections.<Resource> emptyList(), Collections.<Requirement> emptyList(), status);
        } catch (ResolutionException e) {
            result = new ResolutionResult(ResolutionResult.Outcome.Unresolved, null, Collections.<Resource> emptyList(), Collections.<Resource> emptyList(), e.getUnresolvedRequirements(), status);
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Resolution failed.", e));
        } catch (Exception e) {
            result = new ResolutionResult(ResolutionResult.Outcome.Error, null, Collections.<Resource> emptyList(), Collections.<Resource> emptyList(), Collections.<Requirement> emptyList(), status);
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Exception during resolution.", e));
        }
    }

    public ResolutionResult getResult() {
        return result;
    }

}
