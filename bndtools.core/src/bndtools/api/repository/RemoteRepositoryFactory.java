package bndtools.api.repository;

import java.util.Collection;

public interface RemoteRepositoryFactory {
    Collection<? extends RemoteRepository> getConfiguredRepositories();
}
