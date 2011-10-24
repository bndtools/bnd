package org.bndtools.core.obr;

import java.util.List;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.runtime.IStatus;

public class ObrResolutionResult {

    private final boolean resolved;
    private final IStatus status;
    private final List<Resource> required;
    private final List<Resource> optional;
    private final List<Reason> unresolved;

    public ObrResolutionResult(boolean resolved, IStatus status, List<Resource> required, List<Resource> optional, List<Reason> unresolved) {
        this.resolved = resolved;
        this.status = status;
        this.required = required;
        this.optional = optional;
        this.unresolved = unresolved;
    }

    public boolean isResolved() {
        return resolved;
    }

    public IStatus getStatus() {
        return status;
    }

    public List<Resource> getRequired() {
        return required;
    }

    public List<Resource> getOptional() {
        return optional;
    }

    public List<Reason> getUnresolved() {
        return unresolved;
    }
}
