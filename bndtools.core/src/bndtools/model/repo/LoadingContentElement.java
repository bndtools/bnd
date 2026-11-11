package bndtools.model.repo;

public class LoadingContentElement {

	public static final String MSG = "Loading content...";
	private final String message;
	private final boolean failed;

	public LoadingContentElement() {
		this(MSG, false);
	}

	public LoadingContentElement(String repositoryName) {
		this("Loading " + repositoryName + " content...", false);
	}

	private LoadingContentElement(String message, boolean failed) {
		this.message = message;
		this.failed = failed;
	}

	public static LoadingContentElement failure(String repositoryName, String reason) {
		return new LoadingContentElement("Failed to load " + repositoryName + " content: " + reason, true);
	}

	public boolean isFailure() {
		return failed;
	}

	@Override
	public String toString() {
		return message;
	}

}
