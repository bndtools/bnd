package org.bndtools.core.resolve;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import biz.aQute.resolve.ResolveResultProcessor;

public class ResolutionResult {

    private final List<Resource> required;
    private final List<Resource> optional;
    private final Outcome outcome;
    private final ResolveResultProcessor processor;
    private final Collection<Requirement> unresolved;
    private final IStatus status;

    public static enum Outcome {
        Resolved, Unresolved, Error
    }

    public ResolutionResult(Outcome outcome, ResolveResultProcessor processor, List<Resource> required, List<Resource> optional, Collection<Requirement> unresolved, IStatus status) {
        this.outcome = outcome;
        this.processor = processor;
        this.required = required;
        this.optional = optional;
        this.unresolved = unresolved;
        this.status = status;
    }

    public List<Resource> getRequired() {
        return required;
    }

    public List<Resource> getOptional() {
        return optional;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public ResolveResultProcessor getProcessor() {
        return processor;
    }

    public Collection<Requirement> getUnresolved() {
        return unresolved;
    }

    public IStatus getStatus() {
        return status;
    }

}
