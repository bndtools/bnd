package aQute.p2.api;

public enum Classifier {
	BUNDLE("osgi.bundle"),
	FEATURE("org.eclipse.update.feature");

	public final String name;

	private Classifier(String name) {
		this.name = name;
	}
}
