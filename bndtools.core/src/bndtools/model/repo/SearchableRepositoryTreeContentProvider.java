package bndtools.model.repo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import aQute.bnd.service.ResolutionPhase;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.SearchableRepository;

public class SearchableRepositoryTreeContentProvider extends RepositoryTreeContentProvider {

	private static final Logger logger = Logger.getLogger(SearchableRepositoryTreeContentProvider.class.getName());

	public SearchableRepositoryTreeContentProvider() {
		super();
	}

	public SearchableRepositoryTreeContentProvider(ResolutionPhase mode) {
		super(mode);
	}

	public SearchableRepositoryTreeContentProvider(EnumSet<ResolutionPhase> modes) {
		super(modes);
	}

	@Override
	Object[] getRepositoryBundles(RepositoryPlugin repo) {
		Object[] bundles = super.getRepositoryBundles(repo);
		Object[] result = bundles;

		if (repo instanceof SearchableRepository) {
			String filter = getFilter();
			if (filter != null && filter.length() > 0) {
				ContinueSearchElement newElem = new ContinueSearchElement(filter, (SearchableRepository) repo);
				if (bundles != null) {
					result = new Object[bundles.length + 1];
					System.arraycopy(bundles, 0, result, 0, bundles.length);
					result[bundles.length] = newElem;
				} else {
					result = new Object[] {
						newElem
					};
				}
			}
		}

		return result;
	}

	public List<RepositoryBundleVersion> allRepoBundleVersions(final RepositoryPlugin rp) {
		List<RepositoryBundleVersion> allChildren = new ArrayList<>();
		List<String> bsns;
		try {
			List<String> repositoryBsns = rp.list(null);
			if (repositoryBsns == null) {
				return allChildren;
			}
			bsns = new ArrayList<>(repositoryBsns);
			Collections.sort(bsns);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error listing repository " + rp.getName(), e);
			return allChildren;
		}
		for (String bsn : bsns) {
			Object[] versions = getRepositoryBundleVersions(new RepositoryBundle(rp, bsn));
			if (versions != null) {
				for (Object version : versions) {
					if (version instanceof RepositoryBundleVersion bundleVersion) {
						allChildren.add(bundleVersion);
					}
				}
			}
		}
		return allChildren;
	}
}
