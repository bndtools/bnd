package name.neilbartlett.eclipse.bndtools.editor.project;

import aQute.bnd.service.RepositoryPlugin;

class RepositoryBundle {
	
	private final RepositoryPlugin repo;
	private final String bsn;

	public RepositoryBundle(RepositoryPlugin repo, String bsn) {
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
