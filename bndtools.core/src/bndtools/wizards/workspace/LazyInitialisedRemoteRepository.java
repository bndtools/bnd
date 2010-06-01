package bndtools.wizards.workspace;

import java.net.URL;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;

import aQute.libg.version.Version;
import bndtools.api.repository.RemoteRepository;

class LazyInitialisedRemoteRepository implements RemoteRepository {

    private final IConfigurationElement configElement;
    private RemoteRepository repository = null;

    LazyInitialisedRemoteRepository(IConfigurationElement configElement) {
        this.configElement = configElement;
    }

    public String getName() {
        return configElement.getAttribute("name");
    }

    public void initialise(IProgressMonitor monitor) throws CoreException {
        repository = (RemoteRepository) configElement.createExecutableExtension("class");
        repository.initialise(monitor);
    }

    public Collection<String> list(String regex) {
        return repository.list(regex);
    }

    public Collection<Version> versions(String bsn) {
        return repository.versions(bsn);
    }

    public URL[] get(String bsn, String range) {
        return repository.get(bsn, range);
    }


}
