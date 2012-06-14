package bndtools;

import java.net.URI;
import java.util.List;
import java.util.Set;

import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.ResolutionPhase;

public class WrappingIndexProvider implements IndexProvider {

    private final IndexProvider delegate;

    public WrappingIndexProvider(IndexProvider delegate) {
        this.delegate = delegate;
    }

    public List<URI> getIndexLocations() throws Exception {
        return delegate.getIndexLocations();
    }

    public Set<ResolutionPhase> getSupportedPhases() {
        return delegate.getSupportedPhases();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
