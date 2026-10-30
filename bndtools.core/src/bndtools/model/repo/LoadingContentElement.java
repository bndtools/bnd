package bndtools.model.repo;

public class LoadingContentElement {

	public static final String MSG = "Loading content...";
	private final String repositoryName;

	public LoadingContentElement() {
		this(null);
	}

	public LoadingContentElement(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	@Override
	public String toString() {
		return repositoryName == null ? MSG : "Loading " + repositoryName + " content...";
	}

}
