package bndtools.model.repo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import aQute.bnd.service.Registry;
import aQute.lib.deployer.obr.AbstractBaseOBR;

public class WrappingObrRepository extends AbstractBaseOBR {

    private final String location;
    private final OBRIndexProvider delegate;
    private final File cacheDir;

    public WrappingObrRepository(String location, OBRIndexProvider delegate, File cacheDir, Registry registry) {
        this.location = location;
        this.delegate = delegate;
        this.cacheDir = cacheDir;
        this.registry = registry;
    }

    public OBRIndexProvider getDelegate() {
        return delegate;
    }

    public Collection<URL> getOBRIndexes() throws IOException {
        return delegate.getOBRIndexes();
    }

    @Override
    public Set<OBRResolutionMode> getSupportedModes() {
        return delegate.getSupportedModes();
    }

    public File getCacheDirectory() {
        return cacheDir;
    }

    @Override
    public synchronized String getName() {
        return delegate.toString();
    }
    
    public String getLocation() {
        return location;
    }

}
