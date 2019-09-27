package bndtools.editor.contents;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "bndtools.editor.contents.messages";	//$NON-NLS-1$
	public static String		BundleCalculatedImportsPart_columnAttribs;
	public static String		BundleCalculatedImportsPart_columnPackage;
	public static String		BundleCalculatedImportsPart_description;
	public static String		BundleCalculatedImportsPart_error;
	public static String		BundleCalculatedImportsPart_errorFindingType;
	public static String		BundleCalculatedImportsPart_errorOpeningClass;
	public static String		BundleCalculatedImportsPart_errorOpeningJavaEditor;
	public static String		BundleCalculatedImportsPart_jobAnalyse;
	public static String		BundleCalculatedImportsPart_title;
	public static String		BundleCalculatedImportsPart_tooltipShowSelfImports;
	public static String		PackageInfoDialog_AlwaysGenerate;
	public static String		PackageInfoDialog_ExportedPackage;
	public static String		PackageInfoDialog_Message;
	public static String		PackageInfoDialog_Title;
	public static String		PackageInfoDialog_Version;
	public static String		PackageInfoDialog_Warning;
	public static String		PackageInfoDialog_btnCheckAll_text;
	public static String		PackageInfoDialog_btnUncheckAll_text;
	public static String		PackageInfoDialog_btnUncheckAll_text_1;
	public static String		PackageInfoDialog_VersionInvalid;
	public static String		PackageInfoDialog_VersionMissing;
	public static String		TestSuitesPart_add;
	public static String		TestSuitesPart_remove;
	public static String		TestSuitesPart_section_junit_tests;
	public static String		TestSuitesPart_title;
	public static String		TestSuitesPart_errorJavaType;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
