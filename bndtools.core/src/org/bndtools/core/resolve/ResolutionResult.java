package org.bndtools.core.resolve;

import org.eclipse.core.runtime.IStatus;
import biz.aQute.resolve.ResolveProcess;

public class ResolutionResult {

    private final Outcome outcome;
    private final IStatus status;
    private final ResolveProcess resolve;

    public static enum Outcome {
        Resolved, Unresolved, Error
    }

    public ResolutionResult(Outcome outcome, ResolveProcess resolve, IStatus status) {
        this.outcome = outcome;
        this.resolve = resolve;
        this.status = status;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public ResolveProcess getResolve() {
        return resolve;
    }

    public IStatus getStatus() {
        return status;
    }

}
