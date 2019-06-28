package bndtools.internal.testcaseselection;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "bndtools.internal.testcaseselection.messages";	//$NON-NLS-1$
	public static String		JavaSearchScopeTestCaseLister_2;
	public static String		TestCaseSelectionDialog_btnSourceOnly;
	public static String		TestCaseSelectionDialog_title_select_tests;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
