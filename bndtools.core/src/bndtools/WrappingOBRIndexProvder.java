package bndtools;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;

public class WrappingOBRIndexProvder implements OBRIndexProvider {

    private final OBRIndexProvider delegate;

    public WrappingOBRIndexProvder(OBRIndexProvider delegate) {
        this.delegate = delegate;
    }

    public Collection<URL> getOBRIndexes() throws IOException {
        return delegate.getOBRIndexes();
    }

    public Set<OBRResolutionMode> getSupportedModes() {
        return delegate.getSupportedModes();
    }

}
