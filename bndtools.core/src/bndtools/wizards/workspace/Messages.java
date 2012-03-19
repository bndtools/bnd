package bndtools.wizards.workspace;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "bndtools.wizards.workspace.messages"; //$NON-NLS-1$

    public static String CnfSetupCreate;
	public static String CnfSetupCreateTitle;
    public static String CnfSetupCreateExplanation;
	public static String CnfSetupCreateSkip;

	public static String CnfSetupNever;

    public static String CnfSetupNeverWarning;

    public static String CnfSetupNeverWarningTitle;
    public static String DontShowMessageAgain;
    public static String CnfSetupUserConfirmationWizardPage_this_message;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
