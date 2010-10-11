package bndtools.wizards.workspace;

import org.apache.felix.bundlerepository.RepositoryAdmin;

interface IRepositoriesChangedCallback {

    void changedRepositories(RepositoryAdmin repoAdmin);

}
