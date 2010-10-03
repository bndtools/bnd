package bndtools.repository.orbit;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

import bndtools.api.repository.RemoteRepository;
import bndtools.api.repository.RemoteRepositoryFactory;

public class OrbitRepositoryFactory implements RemoteRepositoryFactory, IExecutableExtension {

    private final List<OrbitRepository> repoList = Arrays.asList(new OrbitRepository[] { new OrbitRepository() });

    public Collection<? extends RemoteRepository> getConfiguredRepositories() {
        return repoList;
    }

    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        repoList.get(0).setInitializationData(config, propertyName, data);
    }

}
