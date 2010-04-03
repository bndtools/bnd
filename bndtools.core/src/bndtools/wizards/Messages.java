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
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
