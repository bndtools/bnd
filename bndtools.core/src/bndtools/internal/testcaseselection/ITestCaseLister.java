package bndtools.internal.testcaseselection;

public interface ITestCaseLister {
	String[] getTestCases(boolean includeNonSource, ITestCaseFilter filter) throws TestCaseListException;
}
