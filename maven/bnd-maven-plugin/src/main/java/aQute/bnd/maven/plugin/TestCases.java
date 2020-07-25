package aQute.bnd.maven.plugin;

public enum TestCases {

	/**
	 * JUnit 3.
	 */
	junit3("${classes;EXTENDS;junit.framework.TestCase;CONCRETE}"),
	/**
	 * JUnit 4.
	 */
	junit4("${classes;HIERARCHY_ANNOTATED;org.junit.Test;CONCRETE}"),
	/**
	 * JUnit Platform.
	 */
	junit5("${classes;HIERARCHY_INDIRECTLY_ANNOTATED;org.junit.platform.commons.annotation.Testable;CONCRETE}"),
	/**
	 * All JUnit: {@link #junit3}, {@link #junit4}, and {@link #junit5}.
	 */
	all(junit3.filter() + "," + junit4.filter() + "," + junit5.filter()),
	/**
	 * TestNG.
	 */
	testng("${classes;HIERARCHY_ANNOTATED;org.testng.annotations.Test;CONCRETE}"),
	/**
	 * The Test-Cases header defined in the bundle.
	 */
	useTestCasesHeader("<<UNUSED>>");

	TestCases(String filter) {
		this.filter = filter;
	}

	public String filter() {
		return filter;
	}

	private String filter;
}
