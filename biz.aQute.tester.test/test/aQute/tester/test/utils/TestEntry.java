package aQute.tester.test.utils;

import java.util.Arrays;

// * testId: a unique id for the test
// * testName: the name of the test
// * isSuite: true or false depending on whether the test is a suite
// * testCount: an integer indicating the number of tests
// * isDynamicTest: true or false
// * parentId: the unique testId of its parent if it is a dynamic test,
// otherwise can be "-1"
// * displayName: the display name of the test
// * parameterTypes: comma-separated list of method parameter types if
// applicable, otherwise an empty string
// * uniqueId: the unique ID of the test provided by JUnit launcher,
// otherwise an empty string
public class TestEntry {
	public String		orig;
	public String		testId;
	public String		testName;
	public boolean		isSuite;
	public int			testCount;
	public boolean		isDynamicTest;
	public String		parentId;
	public String		displayName;
	public String[]		parameterTypes;
	public String		uniqueId;
	public TestEntry	parent;

	public TestEntry(String treeEntry) {
		orig = treeEntry;
		// The following code originally copied and adapted from
		// org.eclipse.jdt.internal.junit.model.TestRunSession
		// format:
		// "testId","testName","isSuite","testcount","isDynamicTest","parentId","displayName","parameterTypes","uniqueId"
		int index0 = treeEntry.indexOf(',');
		testId = treeEntry.substring(0, index0);

		StringBuffer testNameBuffer = new StringBuffer(100);
		int index1 = scanTestName(treeEntry, index0 + 1, testNameBuffer);
		testName = testNameBuffer.toString()
			.trim();

		int index2 = treeEntry.indexOf(',', index1 + 1);
		isSuite = treeEntry.substring(index1 + 1, index2)
			.equals("true"); //$NON-NLS-1$

		StringBuffer displayNameBuffer = new StringBuffer(100);
		StringBuffer parameterTypesBuffer = new StringBuffer(200);
		StringBuffer uniqueIdBuffer = new StringBuffer(200);
		int index3 = treeEntry.indexOf(',', index2 + 1);
		if (index3 == -1) {
			testCount = Integer.parseInt(treeEntry.substring(index2 + 1));
			isDynamicTest = false;
			parentId = null;
			displayName = null;
			parameterTypes = null;
			uniqueId = null;
		} else {
			testCount = Integer.parseInt(treeEntry.substring(index2 + 1, index3));

			int index4 = treeEntry.indexOf(',', index3 + 1);
			isDynamicTest = treeEntry.substring(index3 + 1, index4)
				.equals("true"); //$NON-NLS-1$

			int index5 = treeEntry.indexOf(',', index4 + 1);
			parentId = treeEntry.substring(index4 + 1, index5);
			if (parentId.equals("-1")) { //$NON-NLS-1$
				parentId = null;
			}

			int index6 = scanTestName(treeEntry, index5 + 1, displayNameBuffer);
			displayName = displayNameBuffer.toString()
				.trim();
			if (displayName.equals(testName)) {
				displayName = null;
			}

			int index7 = scanTestName(treeEntry, index6 + 1, parameterTypesBuffer);
			String parameterTypesString = parameterTypesBuffer.toString()
				.trim();
			if (parameterTypesString.isEmpty()) {
				parameterTypes = null;
			} else {
				parameterTypes = parameterTypesString.split(","); //$NON-NLS-1$
				Arrays.parallelSetAll(parameterTypes, i -> parameterTypes[i].trim());
			}

			scanTestName(treeEntry, index7 + 1, uniqueIdBuffer);
			uniqueId = uniqueIdBuffer.toString()
				.trim();
			if (uniqueId.isEmpty()) {
				uniqueId = null;
			}
		}
	}

	@Override
	public String toString() {
		return "TestEntry [testId=" + testId + ", testName=" + testName + ", isSuite=" + isSuite + ", testCount="
			+ testCount + ", isDynamicTest=" + isDynamicTest + ", parentId=" + parentId + ", displayName=" + displayName
			+ ", parameterTypes=" + Arrays.toString(parameterTypes) + ", uniqueId=" + uniqueId + "]";
	}

	/**
	 * Append the test name from <code>s</code> to <code>testName</code>.
	 *
	 * @param s the string to scan
	 * @param start the offset of the first character in <code>s</code>
	 * @param testName the result
	 * @return the index of the next ','
	 */
	static private int scanTestName(String s, int start, StringBuffer testName) {
		boolean inQuote = false;
		int i = start;
		for (; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' && !inQuote) {
				inQuote = true;
				continue;
			} else if (inQuote) {
				inQuote = false;
				testName.append(c);
			} else if (c == ',')
				break;
			else
				testName.append(c);
		}
		return i;
	}
}
