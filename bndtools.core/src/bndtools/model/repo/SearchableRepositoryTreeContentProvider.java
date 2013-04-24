package bndtools.model.repo;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.SearchableRepository;

public class SearchableRepositoryTreeContentProvider extends RepositoryTreeContentProvider {

    @Override
    Object[] getRepositoryBundles(RepositoryPlugin repo) {
        Object[] bundles = super.getRepositoryBundles(repo);
        Object[] result = bundles;

        if (repo instanceof SearchableRepository) {
            String filter = getFilter();
            if (filter != null && filter.length() > 0) {
                result = new Object[bundles.length + 1];
                System.arraycopy(bundles, 0, result, 0, bundles.length);
                result[bundles.length] = new ContinueSearchElement(filter, (SearchableRepository) repo);
            }
        }

        return result;
    }
}
