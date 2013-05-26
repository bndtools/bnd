package org.bndtools.core.resolve;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;

public class ResolutionResult {

    private final Outcome outcome;
    private final Map<Resource,List<Wire>> resourceWirings;
    private final IStatus status;
    private final String log;
    private final ResolutionException resolutionException;

    public static enum Outcome {
        Resolved, Unresolved, Error, Cancelled
    }

    public ResolutionResult(Outcome outcome, Map<Resource,List<Wire>> resourceWirings, ResolutionException resolutionExceptoin, IStatus status, String log) {
        this.outcome = outcome;
        this.resourceWirings = resourceWirings;
        this.resolutionException = resolutionExceptoin;
        this.status = status;
        this.log = log;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public Map<Resource,List<Wire>> getResourceWirings() {
        return resourceWirings;
    }

    public ResolutionException getResolutionException() {
        return resolutionException;
    }

    public IStatus getStatus() {
        return status;
    }

    public String getLog() {
        return log;
    }

}
