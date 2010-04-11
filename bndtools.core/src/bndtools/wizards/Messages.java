package bndtools.wizards;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "bndtools.wizards.messages"; //$NON-NLS-1$
	public static String EmptyBndFileWizard_errorEnablingSubBundles;
	public static String EmptyBndFileWizard_errorOpeningBndEditor;
	public static String EmptyBndFileWizard_errorTitleNewBndFile;
	public static String EmptyBndFileWizard_questionSubBundlesNotEnabled;
	public static String EmptyBndFileWizard_selectAsDefault;
	public static String EmptyBndFileWizard_titleSubBundlesNotEnabled;
	
	public static String NewBndFileWizardPage_errorCheckingBndNature;
	public static String NewBndFileWizardPage_errorReservedFilename;
	public static String NewBndFileWizardPage_labelBndFile;
	public static String NewBndFileWizardPage_title;
	public static String NewBndFileWizardPage_titleError;
	public static String NewBndFileWizardPage_warningNonBndProject;
	public static String NewBndFileWizardPage_warningNotTopLevel;

	public static String NewWrappingBndFileWizardPage_errorInvalidVersion;
	public static String NewWrappingBndFileWizardPage_labelBSN;
	public static String NewWrappingBndFileWizardPage_labelVersion;
	public static String NewWrappingBndFileWizardPage_messageSpecifyFileName;
	public static String NewWrappingBndFileWizardPage_title;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
