package bndtools.internal.testcaseselection;

public interface ITestCaseFilter {
	boolean select(String testClassName);
}
