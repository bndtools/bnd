package bndtools.model.obr;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;

public class RequirementWrapper implements Requirement {

    private final bndtools.api.Requirement delegate;

    public RequirementWrapper(bndtools.api.Requirement delegate) {
        this.delegate = delegate;
    }

    public String getName() {
        return delegate.getName();
    }

    public String getFilter() {
        return delegate.getFilter();
    }

    public boolean isMultiple() {
        return false;
    }

    public boolean isOptional() {
        return false;
    }

    public boolean isExtend() {
        return false;
    }

    public String getComment() {
        return "";
    }

    public boolean isSatisfied(Capability capability) {
        // no need to implement correctly
        return false;
    }

}
