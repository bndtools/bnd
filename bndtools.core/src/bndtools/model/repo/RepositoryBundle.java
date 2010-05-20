package bndtools.model.repo;

import aQute.bnd.service.RepositoryPlugin;

public class RepositoryBundle {

	private final RepositoryPlugin repo;
	private final String bsn;

	RepositoryBundle(RepositoryPlugin repo, String bsn) {
		this.repo = repo;
		this.bsn = bsn;
	}
	public RepositoryPlugin getRepo() {
		return repo;
	}
	public String getBsn() {
		return bsn;
	}
	@Override
	public String toString() {
		return "RepositoryBundle [repo=" + repo + ", bsn=" + bsn + "]";
	}
}
