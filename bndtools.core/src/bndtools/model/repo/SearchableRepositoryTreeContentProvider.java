package bndtools.model.repo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.EnumSet;

import aQute.bnd.service.ResolutionPhase;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.SearchableRepository;

public class SearchableRepositoryTreeContentProvider extends RepositoryTreeContentProvider {

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
		Object[] result = getChildren(rp);

		List<RepositoryBundleVersion> allChildren = new ArrayList<>();
		Queue<Object> queue = new LinkedList<>();

		if (result != null) {
			queue.addAll(Arrays.asList(result));
		}

		while (!queue.isEmpty()) {
			Object currentChild = queue.poll();

			if (currentChild instanceof RepositoryBundleVersion rpv) {
				allChildren.add(rpv);
			}
			else if (currentChild instanceof RepositoryResourceElement rre) {
				allChildren.add(rre.getRepositoryBundleVersion());
			}

			Object[] childrenOfChild = getChildren(currentChild);
			if (childrenOfChild != null) {
				queue.addAll(Arrays.asList(childrenOfChild));
			}
		}
		return allChildren;
	}
}
