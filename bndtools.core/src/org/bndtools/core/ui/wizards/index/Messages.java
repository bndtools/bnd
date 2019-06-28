package org.bndtools.core.ui.wizards.index;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "org.bndtools.core.ui.wizards.index.messages";	//$NON-NLS-1$
	public static String		IndexerWizardPage_baseDir;
	public static String		IndexerWizardPage_browse;
	public static String		IndexerWizardPage_browseExternal;
	public static String		IndexerWizardPage_checking;
	public static String		IndexerWizardPage_compressed;
	public static String		IndexerWizardPage_description;
	public static String		IndexerWizardPage_error_fileExists;
	public static String		IndexerWizardPage_error_fileSearch;
	public static String		IndexerWizardPage_error_invalidPattern;
	public static String		IndexerWizardPage_error_noSlashes;
	public static String		IndexerWizardPage_error_noSuchDir;
	public static String		IndexerWizardPage_error_notDir;
	public static String		IndexerWizardPage_inputs;
	public static String		IndexerWizardPage_output;
	public static String		IndexerWizardPage_outputFileMessage;
	public static String		IndexerWizardPage_prefix;
	public static String		IndexerWizardPage_prettyPrint;
	public static String		IndexerWizardPage_resourcePattern;
	public static String		IndexerWizardPage_resourcePatternHelp;
	public static String		IndexerWizardPage_selectBaseDir;
	public static String		IndexerWizardPage_title;
	public static String		IndexerWizardPage_updateInputs;
	public static String		IndexerWizardPage_warn_fileOverwrite;
	public static String		IndexerWizardPage_warn_noMatchingFiles;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
