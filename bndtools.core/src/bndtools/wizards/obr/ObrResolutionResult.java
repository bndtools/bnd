package bndtools.wizards.obr;

import java.util.List;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resource;

public class ObrResolutionResult {

    private final boolean resolved;
    private final List<Resource> required;
    private final List<Resource> optional;
    private final List<Reason> unresolved;

    public ObrResolutionResult(boolean resolved, List<Resource> required, List<Resource> optional, List<Reason> unresolved) {
        this.resolved = resolved;
        this.required = required;
        this.optional = optional;
        this.unresolved = unresolved;
    }

    public boolean isResolved() {
        return resolved;
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
