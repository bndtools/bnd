package bndtools.model.obr;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.osgi.resource.Namespace;

public class RequirementWrapper implements Requirement {

    private final org.osgi.resource.Requirement delegate;

    public RequirementWrapper(org.osgi.resource.Requirement delegate) {
        this.delegate = delegate;
    }

    public String getName() {
        return delegate.getNamespace();
    }

    public String getFilter() {
        return delegate.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
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
